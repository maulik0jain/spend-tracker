package com.smsreader.app.export

import com.smsreader.app.data.Category
import com.smsreader.app.data.Channel
import com.smsreader.app.data.Payment
import com.smsreader.app.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SheetRowTest {
    private fun pay(name: String, payee: String, paise: Long, category: Category = Category.GROCERIES) = Payment(
        Transaction(
            amountPaise = paise,
            payee = payee,
            payeeKey = payee.lowercase(),
            channel = Channel.UPI,
            reference = "429911873310",
            timestamp = LocalDateTime.of(2026, 10, 29, 19, 40, 37).toInstant(ZoneOffset.UTC).toEpochMilli(),
            sender = "AD-HDFCBK",
            body = "",
        ),
        tagKey = payee.lowercase(),
        groupKey = name.lowercase(),
        name = name,
        category = category,
    )

    @Test fun cellsLineUpWithHeadings() {
        val cells = SheetRow.cells(pay("BigBasket", "bigbasket@hdfcbank", 218640), ZoneOffset.UTC)
        assertEquals(listOf("2026-10-29", "19:40", "BigBasket", "2186.40", "Groceries", "UPI", "bigbasket@hdfcbank"), cells)
        assertEquals(SheetRow.headings.size, cells.size)
    }

    @Test fun amountIsAPlainNumber() {
        assertEquals("0.05", SheetRow.amount(5))
        assertEquals("649.00", SheetRow.amount(64900))
        assertEquals("125000.50", SheetRow.amount(12500050))
    }

    @Test fun textCannotSplitTheRowOrBecomeAFormula() {
        assertEquals("Kumar Electronics Andheri", SheetRow.text(" Kumar\tElectronics\r\nAndheri "))
        assertEquals("'=HYPERLINK(\"x\")", SheetRow.text("=HYPERLINK(\"x\")"))
        assertEquals("'+91 98200 41177", SheetRow.text("+91 98200 41177"))
        assertEquals("'@shop", SheetRow.text("@shop"))
        assertEquals("Aarav – pocket money", SheetRow.text("Aarav – pocket money"))
    }

    @Test fun tsvAndHtmlCarryTheSameCells() {
        val rows = listOf(SheetRow.cells(pay("Swiggy", "swiggy@icici", 43700, Category.FOOD), ZoneOffset.UTC))
        assertEquals("2026-10-29\t19:40\tSwiggy\t437.00\tFood & dining\tUPI\tswiggy@icici", SheetRow.tsv(rows))
        assertEquals(
            "<table><tr><td>2026-10-29</td><td>19:40</td><td>Swiggy</td><td>437.00</td>" +
                "<td>Food &amp; dining</td><td>UPI</td><td>swiggy@icici</td></tr></table>",
            SheetRow.html(rows),
        )
        assertEquals("<table><tr><td>&lt;b&gt; &quot;A&quot;</td></tr></table>", SheetRow.html(listOf(listOf("<b> \"A\""))))
    }
}
