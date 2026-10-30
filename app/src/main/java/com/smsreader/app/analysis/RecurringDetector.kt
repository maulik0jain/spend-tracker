package com.smsreader.app.analysis

import com.smsreader.app.data.Payment
import com.smsreader.app.parse.TransactionParser
import kotlin.math.floor

private const val DAY_MS = 86_400_000L
private const val DAYS_PER_MONTH = 30.44

enum class Frequency(val label: String, val periodDays: Double) {
    WEEKLY("Weekly", 7.0),
    MONTHLY("Monthly", DAYS_PER_MONTH),
    QUARTERLY("Quarterly", 91.31),
    YEARLY("Yearly", 365.25);

    val periodMs: Long get() = (periodDays * DAY_MS).toLong()
}

data class Recurring(
    val latest: Payment,
    val frequency: Frequency,
    /** Median payment: what you usually pay. */
    val typicalPaise: Long,
    val count: Int,
    val totalPaise: Long,
    val since: Long,
    /** Average spend per month on this payee, extras included. */
    val perMonthPaise: Long,
    val nextExpected: Long,
) {
    /** A whole cycle has passed since the expected date: probably cancelled or paid another way. */
    fun lapsed(now: Long): Boolean = now > nextExpected + frequency.periodMs
}

/**
 * Finds payees paid on a regular cycle: Netflix every month, pocket money every week...
 *
 * Payments are bucketed into cycles of each candidate length. A payee is recurring when, over
 * its latest stretch of activity, at least 80% of cycles contain a payment. Extra payments
 * inside a cycle and the odd skipped cycle are fine; a shop visited every day is not
 * recurring (too many payments per cycle), and neither is a one-off payee.
 */
object RecurringDetector {
    private const val MIN_CYCLES = 3
    private const val MIN_COVERAGE = 0.8
    private const val MAX_PAYMENTS_PER_CYCLE = 3

    fun detect(payments: List<Payment>): List<Recurring> =
        payments.filter { it.txn.payee != TransactionParser.UNKNOWN_PAYEE }
            .groupBy { it.groupKey }
            .values
            .mapNotNull(::analyse)
            .sortedBy { it.nextExpected }

    private fun analyse(group: List<Payment>): Recurring? {
        if (group.size < MIN_CYCLES) return null
        val sorted = group.sortedBy { it.txn.timestamp }
        val first = sorted.first().txn.timestamp
        for (frequency in Frequency.entries) {
            // Rounded so a few days of jitter keep a payment in its own cycle.
            val cycleOf = sorted.map { floor((it.txn.timestamp - first).toDouble() / frequency.periodMs + 0.5).toInt() }
            val run = latestRun(cycleOf.distinct())
            val cycles = run.last() - run.first() + 1
            if (run.size < MIN_CYCLES || run.size < cycles * MIN_COVERAGE) continue
            val inRun = sorted.filterIndexed { i, _ -> cycleOf[i] >= run.first() }
            if (inRun.size > run.size * MAX_PAYMENTS_PER_CYCLE) continue

            val latest = inRun.last()
            val amounts = inRun.map { it.txn.amountPaise }.sorted()
            val total = amounts.sum()
            return Recurring(
                latest = latest,
                frequency = frequency,
                typicalPaise = amounts[amounts.size / 2],
                count = inRun.size,
                totalPaise = total,
                since = inRun.first().txn.timestamp,
                perMonthPaise = (total.toDouble() / cycles * DAYS_PER_MONTH / frequency.periodDays).toLong(),
                nextExpected = latest.txn.timestamp + frequency.periodMs,
            )
        }
        return null
    }

    /** The most recent stretch of cycles with at most one cycle skipped between payments. */
    private fun latestRun(cycles: List<Int>): List<Int> {
        var start = cycles.lastIndex
        while (start > 0 && cycles[start] - cycles[start - 1] <= 2) start--
        return cycles.subList(start, cycles.size)
    }
}
