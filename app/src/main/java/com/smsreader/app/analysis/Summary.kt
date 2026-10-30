package com.smsreader.app.analysis

import com.smsreader.app.data.Category
import com.smsreader.app.data.Payment
import java.time.LocalDate
import java.time.ZoneId

/** Half-open time window [startMs, endMs). */
data class Window(val startMs: Long, val endMs: Long) {
    operator fun contains(timestamp: Long): Boolean = timestamp in startMs until endMs
}

enum class Period(val label: String) {
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_3_MONTHS("Last 3 months"),
    THIS_YEAR("This year"),
    ALL("All time"),
    CUSTOM("Custom");

    /** null means no limit. [custom] holds the first and last day (inclusive) for CUSTOM. */
    fun window(today: LocalDate, custom: Pair<LocalDate, LocalDate>?, zone: ZoneId = ZoneId.systemDefault()): Window? {
        fun ms(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val monthStart = today.withDayOfMonth(1)
        val tomorrow = today.plusDays(1)
        return when (this) {
            THIS_MONTH -> Window(ms(monthStart), ms(tomorrow))
            LAST_MONTH -> Window(ms(monthStart.minusMonths(1)), ms(monthStart))
            LAST_3_MONTHS -> Window(ms(monthStart.minusMonths(2)), ms(tomorrow))
            THIS_YEAR -> Window(ms(today.withDayOfYear(1)), ms(tomorrow))
            ALL -> null
            CUSTOM -> custom?.let { (first, last) -> Window(ms(first), ms(last.plusDays(1))) }
        }
    }
}

enum class AmountBucket(val label: String, private val belowPaise: Long) {
    SMALL("Under ₹100", 100_00),
    MEDIUM("₹100 – ₹999", 1_000_00),
    LARGE("₹1,000 – ₹9,999", 10_000_00),
    HUGE("₹10,000 and above", Long.MAX_VALUE);

    companion object {
        fun of(paise: Long): AmountBucket = entries.first { paise < it.belowPaise }
    }
}

data class Total(val id: String, val label: String, val count: Int, val paise: Long)

/**
 * [spentPaise] leaves out credit-card bill payments: the card spends they settle are
 * already counted from the card's own SMS, so adding the bill would count them twice.
 */
data class Summary(
    val spentPaise: Long,
    val count: Int,
    val largestPaise: Long,
    val cardBillPaise: Long,
    val byCategory: List<Total>,
    val byPayee: List<Total>,
    val byBucket: List<Total>,
)

/** [payments] newest first, so each payee group is labelled with its latest name. */
fun summarise(payments: List<Payment>): Summary {
    val (cardBills, spending) = payments.partition { it.category == Category.CARD_BILL }
    return Summary(
        spentPaise = spending.sumOf { it.txn.amountPaise },
        count = spending.size,
        largestPaise = spending.maxOfOrNull { it.txn.amountPaise } ?: 0,
        cardBillPaise = cardBills.sumOf { it.txn.amountPaise },
        byCategory = payments.groupBy { it.category }
            .map { (category, ps) -> total(category.name, category.label, ps) }
            .sortedByDescending { it.paise },
        byPayee = spending.groupBy { it.groupKey }
            .map { (key, ps) -> total(key, ps.first().name, ps) }
            .sortedByDescending { it.paise },
        byBucket = spending.groupBy { AmountBucket.of(it.txn.amountPaise) }
            .let { byBucket -> AmountBucket.entries.map { total(it.name, it.label, byBucket[it].orEmpty()) } },
    )
}

private fun total(id: String, label: String, payments: List<Payment>) =
    Total(id, label, payments.size, payments.sumOf { it.txn.amountPaise })
