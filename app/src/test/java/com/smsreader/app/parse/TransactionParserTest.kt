package com.smsreader.app.parse

import com.smsreader.app.data.Channel
import com.smsreader.app.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionParserTest {
    private fun parse(body: String, sender: String = "VM-HDFCBK"): Transaction? =
        TransactionParser.parse(sender, body, 0L)

    private fun debit(body: String, sender: String = "VM-HDFCBK"): Transaction {
        val t = parse(body, sender)
        assertNotNull("expected a debit: $body", t)
        return t!!
    }

    // --- Debits that must be recognised ---

    @Test fun hdfcUpiMultiline() {
        val t = debit(
            "Sent Rs.649.00\nFrom HDFC Bank A/C *1234\nTo NETFLIX\nOn 05/10/26\nRef 527812345678\n" +
                "Not You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
        )
        assertEquals(64900L, t.amountPaise)
        assertEquals("NETFLIX", t.payee)
        assertEquals(Channel.UPI, t.channel)
        assertEquals("527812345678", t.reference)
    }

    @Test fun sbiUpiWithoutCurrencySymbol() {
        val t = debit(
            "Dear UPI user A/C X1234 debited by 20.0 on date 30Oct26 trf to SWIGGY Refno 612345678901. " +
                "If not u? call 1800111109. -SBI",
            sender = "AD-SBIUPI",
        )
        assertEquals(2000L, t.amountPaise)
        assertEquals("SWIGGY", t.payee)
        assertEquals(Channel.UPI, t.channel)
        assertEquals("612345678901", t.reference)
    }

    @Test fun iciciPayeeBeforeCredited() {
        val t = debit(
            "ICICI Bank Acct XX123 debited for Rs 1,500.00 on 30-Oct-26; RAHUL SHARMA credited. UPI:612345678901. " +
                "Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.",
            sender = "JD-ICICIB-S",
        )
        assertEquals(150000L, t.amountPaise)
        assertEquals("RAHUL SHARMA", t.payee)
        assertEquals("612345678901", t.reference)
    }

    @Test fun axisUpiPathFormat() {
        val t = debit(
            "INR 250.00 debited\nA/c no. XX1234\n30-10-26, 10:15:00\nUPI/P2A/612345678901/RAHUL SHARMA\n" +
                "Not you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            sender = "AX-AXISBK",
        )
        assertEquals(25000L, t.amountPaise)
        assertEquals("RAHUL SHARMA", t.payee)
        assertEquals(Channel.UPI, t.channel)
    }

    @Test fun kotakToUpiId() {
        val t = debit(
            "Sent Rs.100.00 from Kotak Bank AC X1234 to son.sharma@okaxis on 30-10-26.UPI Ref 612345678901. " +
                "Not you, https://kotak.com/KBANKT/Fraud",
            sender = "BZ-KOTAKB",
        )
        assertEquals(10000L, t.amountPaise)
        assertEquals("son.sharma@okaxis", t.payee)
        assertEquals("son.sharma@okaxis", t.payeeKey)
    }

    @Test fun canaraTowardsUpiPath() {
        val t = debit(
            "An amount of INR 100.00 has been DEBITED to your account XXX1234 on 30/10/2026 towards " +
                "UPI/612345678901/RAHUL SHARMA. Total Avail.bal INR 5,000.00. - Canara Bank",
            sender = "VM-CANBNK",
        )
        assertEquals(10000L, t.amountPaise)
        assertEquals("RAHUL SHARMA", t.payee)
    }

    @Test fun bankOfBarodaDrCr() {
        val t = debit(
            "Rs.500.00 Dr. from A/C XXXXXX1234 and Cr. to rahul@ybl. Ref:612345678901. " +
                "AvlBal:Rs5000.00(2026:10:30 10:15:00). Not you? Call 18005700-BOB",
            sender = "VM-BOBTXN",
        )
        assertEquals(50000L, t.amountPaise)
        assertEquals("rahul@ybl", t.payee)
        assertEquals("612345678901", t.reference)
    }

    @Test fun paytmWallet() {
        val t = debit(
            "Paid Rs.200 to Swiggy from Paytm Balance. Updated Balance: Paytm Wallet- Rs 50. " +
                "More Details: https://paytm.me/x-AbCdE",
            sender = "VK-iPaytm",
        )
        assertEquals(20000L, t.amountPaise)
        assertEquals("Swiggy", t.payee)
        assertEquals(Channel.WALLET, t.channel)
    }

    @Test fun creditCardSpendAtMerchant() {
        val t = debit(
            "Rs.649.00 spent on your SBI Credit Card ending 1234 at NETFLIX on 05/11/26. Trxn. Ref No. 123456.",
            sender = "AD-SBICRD",
        )
        assertEquals(64900L, t.amountPaise)
        assertEquals("NETFLIX", t.payee)
        assertEquals(Channel.CARD, t.channel)
    }

    @Test fun hdfcCardSkipsBalanceAmount() {
        val t = debit(
            "Spent Rs.1,299 On HDFC Bank Card 1234 At AMAZON PAY INDIA On 2026-10-30:10:15:20 Bal Rs.50,000 " +
                "Not You? Call 18002586161",
        )
        assertEquals(129900L, t.amountPaise)
        assertEquals("AMAZON PAY INDIA", t.payee)
        assertEquals(Channel.CARD, t.channel)
    }

    @Test fun balanceBeforeAmountIsSkipped() {
        val t = debit("Avl Bal Rs 9,000.00. Your A/c XX1234 is debited with Rs 1,000.00 towards NEFT to MOTHER")
        assertEquals(100000L, t.amountPaise)
    }

    @Test fun atmWithdrawal() {
        val t = debit("Rs.2000 withdrawn from A/c XX1234 at ATM SBIN0001234 on 30Oct26. Avl Bal Rs.5000")
        assertEquals(200000L, t.amountPaise)
        assertEquals(Channel.ATM, t.channel)
    }

    @Test fun unknownPayeeStillCounted() {
        val t = debit(
            "A/c *1234 Debited for Rs:100.00 on 30-10-2026 10:15:00 by Mob Bk ref no 612345678901 " +
                "Avl Bal Rs:5000.00.If not you, Call 1800222243 -Union Bank of India",
            sender = "VM-UNIONB",
        )
        assertEquals(10000L, t.amountPaise)
        assertEquals(TransactionParser.UNKNOWN_PAYEE, t.payee)
        assertEquals("612345678901", t.reference)
    }

    // --- Messages that must be ignored ---

    @Test fun ignoresOtp() {
        assertNull(parse("123456 is the OTP for txn of INR 500.00 at AMAZON on HDFC Bank card ending 1234. Do not share OTP."))
    }

    @Test fun ignoresCredit() {
        assertNull(parse("Rs.5000.00 credited to your A/c XX1234 on 30-10-26 by UPI from RAHUL SHARMA. Avl Bal Rs.10000"))
    }

    @Test fun ignoresMoneyReceived() {
        assertNull(parse("Received Rs.500.00 in your Kotak Bank AC X1234 from rahul@ybl on 30-10-26.UPI Ref:612345678901."))
    }

    @Test fun ignoresUpcomingMandateDebit() {
        assertNull(parse("Rs.649 will be debited from your A/c XX1234 on 05-11-26 towards NETFLIX for UPI Mandate. -HDFC Bank"))
    }

    @Test fun ignoresFailedPayment() {
        assertNull(parse("Txn of Rs.500 to rahul@ybl from A/c XX1234 failed due to insufficient balance."))
    }

    @Test fun ignoresCardStatement() {
        assertNull(parse("HDFC Bank Credit Card XX1234 statement: Total amt due Rs.12,345 Min amt due Rs.620 due date 15-11-26"))
    }

    @Test fun ignoresPersonalNumbers() {
        assertNull(parse("I sent you Rs 500 for dinner, check your account", sender = "+919876543210"))
    }

    // --- Grouping and de-duplication ---

    @Test fun payeeKeyMergesDomainAndCompanySuffix() {
        assertEquals(TransactionParser.keyOf("Netflix"), TransactionParser.keyOf("NETFLIX.COM"))
        assertEquals(TransactionParser.keyOf("ZOMATO"), TransactionParser.keyOf("ZOMATO PVT LTD"))
    }

    @Test fun dedupeDropsSameReferenceFromTwoSenders() {
        val bank = txn(reference = "612345678901", sender = "VM-HDFCBK", timestamp = 60_000)
        val app = txn(reference = "612345678901", sender = "VK-iPaytm", timestamp = 0)
        assertEquals(listOf(bank), TransactionParser.dedupe(listOf(bank, app)))
    }

    @Test fun dedupeKeepsTwoRealPaymentsOfSameAmount() {
        val first = txn(reference = null, sender = "VM-HDFCBK", timestamp = 60_000)
        val second = txn(reference = null, sender = "VM-HDFCBK", timestamp = 0)
        assertEquals(2, TransactionParser.dedupe(listOf(first, second)).size)
    }

    private fun txn(reference: String?, sender: String, timestamp: Long) =
        Transaction(10000, "X", "x", Channel.UPI, reference, timestamp, sender, "")
}
