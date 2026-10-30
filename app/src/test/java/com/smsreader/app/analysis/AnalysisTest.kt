package com.smsreader.app.analysis

import com.smsreader.app.data.Category
import com.smsreader.app.data.Channel
import com.smsreader.app.data.Payment
import com.smsreader.app.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AnalysisTest {
    private val day = 86_400_000L
    private val start = 1_750_000_000_000L

    private fun pay(key: String, dayOffset: Int, paise: Long = 64900, category: Category = Category.OTHER) = Payment(
        Transaction(paise, key, key, Channel.UPI, null, start + dayOffset * day, "VM-BANK", ""),
        tagKey = key,
        groupKey = key,
        name = key,
        category = category,
    )

    @Test fun monthlySubscriptionIsDetected() {
        val found = RecurringDetector.detect(listOf(0, 30, 61, 91, 122).map { pay("netflix", it) })
        assertEquals(1, found.size)
        val r = found.single()
        assertEquals(Frequency.MONTHLY, r.frequency)
        assertEquals(64900L, r.typicalPaise)
        assertEquals(5, r.count)
        assertEquals(start + 122 * day + Frequency.MONTHLY.periodMs, r.nextExpected)
    }

    @Test fun subscriptionResumedAfterLongBreakCountsOnlyTheLatestStretch() {
        val found = RecurringDetector.detect(listOf(0, 30, 61, 300, 330, 361, 391).map { pay("netflix", it) })
        assertEquals(4, found.single().count)
        assertEquals(start + 300 * day, found.single().since)
    }

    @Test fun dailyShopIsNotRecurring() {
        assertTrue(RecurringDetector.detect((0..60).map { pay("chai", it, 2000) }).isEmpty())
    }

    @Test fun weeklyPocketMoneyWithVaryingAmountIsDetected() {
        val found = RecurringDetector.detect(
            listOf(0 to 50000L, 7 to 50000L, 14 to 70000L, 21 to 50000L).map { (d, p) -> pay("son", d, p) },
        )
        assertEquals(Frequency.WEEKLY, found.single().frequency)
        assertEquals(50000L, found.single().typicalPaise)
    }

    @Test fun oneExtraPaymentDoesNotHideTheCycle() {
        val found = RecurringDetector.detect(listOf(0, 30, 45, 60, 90, 120).map { pay("son", it) })
        assertEquals(Frequency.MONTHLY, found.single().frequency)
    }

    @Test fun irregularOrTooFewPaymentsAreNotRecurring() {
        assertTrue(RecurringDetector.detect(listOf(0, 3, 40, 41, 100).map { pay("shop", it) }).isEmpty())
        assertTrue(RecurringDetector.detect(listOf(0, 30).map { pay("netflix", it) }).isEmpty())
    }

    @Test fun lapsedAfterAMissedCycle() {
        val r = RecurringDetector.detect(listOf(0, 30, 60).map { pay("gym", it) }).single()
        assertFalse(r.lapsed(start + 95 * day))
        assertTrue(r.lapsed(start + 125 * day))
    }

    @Test fun periodWindows() {
        val today = LocalDate.of(2026, 10, 30)
        fun ms(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(Window(ms(2026, 9, 1), ms(2026, 10, 1)), Period.LAST_MONTH.window(today, null, ZoneOffset.UTC))
        assertEquals(Window(ms(2026, 8, 1), ms(2026, 10, 31)), Period.LAST_3_MONTHS.window(today, null, ZoneOffset.UTC))
        val custom = LocalDate.of(2026, 1, 5) to LocalDate.of(2026, 1, 5)
        assertEquals(Window(ms(2026, 1, 5), ms(2026, 1, 6)), Period.CUSTOM.window(today, custom, ZoneOffset.UTC))
        assertEquals(null, Period.ALL.window(today, null, ZoneOffset.UTC))
    }

    @Test fun summaryExcludesCardBillsAndBucketsBySize() {
        val s = summarise(
            listOf(
                pay("chai", 0, 5000),
                pay("netflix", 1, 64900),
                pay("rent", 2, 2_500_000),
                pay("cred", 3, 1_000_000, Category.CARD_BILL),
            ),
        )
        assertEquals(5000L + 64900 + 2_500_000, s.spentPaise)
        assertEquals(1_000_000L, s.cardBillPaise)
        assertEquals(listOf(1, 1, 0, 1), s.byBucket.map { it.count })
        assertEquals("rent", s.byPayee.first().label)
    }
}
