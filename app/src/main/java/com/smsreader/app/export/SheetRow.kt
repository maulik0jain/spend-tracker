package com.smsreader.app.export

import com.smsreader.app.data.Payment
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A payment as one spreadsheet row, in two clipboard forms: tab-separated text and an HTML
 * table. Spreadsheet apps split either one into cells when it is pasted.
 */
object SheetRow {
    val headings = listOf("Date", "Time", "Payee", "Amount", "Category", "Method", "Paid to")

    fun cells(payment: Payment, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        val at = Instant.ofEpochMilli(payment.txn.timestamp).atZone(zone)
        return listOf(
            at.toLocalDate().toString(), // 2026-10-29, which spreadsheets read as a date
            at.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(),
            text(payment.name),
            amount(payment.txn.amountPaise),
            payment.category.label,
            payment.txn.channel.label,
            text(payment.txn.payee),
        )
    }

    /** Plain number with two decimals and no ₹ or commas, so the cell can be summed. */
    fun amount(paise: Long) = "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"

    fun tsv(rows: List<List<String>>) = rows.joinToString("\n") { it.joinToString("\t") }

    fun html(rows: List<List<String>>) = rows.joinToString("", "<table>", "</table>") { row ->
        row.joinToString("", "<tr>", "</tr>") { "<td>${escape(it)}</td>" }
    }

    /**
     * Payee names come from SMS text. Tabs and line breaks would split the row, and a leading
     * = + - or @ would make the spreadsheet treat the name as a formula, so neither survives.
     */
    fun text(value: String): String {
        val flat = value.replace(Regex("\\s+"), " ").trim()
        return if (flat.isNotEmpty() && flat[0] in "=+-@") "'$flat" else flat
    }

    private fun escape(value: String) = buildString {
        value.forEach {
            when (it) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(it)
            }
        }
    }
}
