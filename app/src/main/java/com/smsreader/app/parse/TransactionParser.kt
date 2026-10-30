package com.smsreader.app.parse

import com.smsreader.app.data.Channel
import com.smsreader.app.data.Transaction

/**
 * Turns bank, UPI and wallet SMS into debit [Transaction]s. Anything that is not a
 * completed money-out event (credits, OTPs, reminders, failed or reversed payments,
 * messages from personal numbers) returns null.
 *
 * Google Pay / PhonePe do not send SMS themselves: the bank sends the UPI debit SMS,
 * so covering bank formats covers those apps.
 */
object TransactionParser {
    const val UNKNOWN_PAYEE = "Unknown payee"
    private const val DUPLICATE_WINDOW_MS = 2 * 60 * 1000L

    private fun rx(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private val personalNumber = Regex("""^\+?\d{10,}$""")

    // From "Not you? Call ..." onwards: never holds the payee, but often holds "to <number>".
    private val disclaimer =
        rx("""\b(?:not you|if not (?:done|you|u|initiated|made|by you)|not done by you|to block|sms block|call\s*:?\s*\d|queries|for dispute)|https?://""")
    private val notACompletedDebit =
        rx("""\b(?:otp|one[- ]time password|will be debited|to be debited|is due|due date|amt due|amount due|min(?:imum)? (?:amt |amount )?due|has requested|requested money|collect request|payment request|failed|declined|unsuccessful|insufficient|reversed|reversal)\b""")
    private val debitWord =
        rx("""\b(?:debited|debit|dr|sent|paid|spent|withdrawn|withdrawal|purchase|transferred|txn (?:of )?(?:rs|inr)|transaction of)\b""")
    private val creditWord = rx("""\b(?:credited|cr|received|deposited|refund|refunded)\b""")
    private val instrument =
        rx("""\b(?:a/c|acct|account|ac|card|upi|vpa|imps|neft|rtgs|atm|wallet|ref|refno|rrn|txn)\b""")

    private val amount = rx("""(?:\brs\.?|\binr|₹)\s*[:.]?\s*(\d[\d,]*(?:\.\d{1,2})?)""")
    // SBI UPI leaves out the currency: "A/C X1234 debited by 20.0 on date ..."
    private val bareDebitAmount = rx("""\bdebited\s+(?:by|for|with)\s+(\d[\d,]*(?:\.\d{1,2})?)""")
    private val balanceLabel = rx("""(?:bal|balance|lmt|limit)\D{0,6}$""")
    private val reference =
        rx("""\b(?:upi\s*ref(?:erence)?|ref(?:erence)?|refno|rrn|upi|txn\s*id|utr)(?:\s*no)?\s*[:.#/-]?\s*(\d{6,})""")

    private const val VPA = """[a-z0-9][a-z0-9._-]*@[a-z][a-z0-9]+"""
    private const val NAME = """[a-z][a-z0-9 .&'_*/-]*?"""
    private const val STOP =
        """\s+(?:on|ref|refno|upi|via|from|using|avl|info|for|at|by|thru|dated)\b|\s*[,;:(\n]|\.(?:\s|$)|$"""

    // Tried in order; the first acceptable capture wins.
    private val payeePatterns = listOf(
        rx("""\b(?:to|towards)\s+(?:vpa\s+)?($VPA)"""),
        rx(""";\s*($NAME)\s+credited\b"""),                              // ICICI: "...; RAHUL SHARMA credited."
        rx("""\bupi/(?:[a-z0-9]{2,4}/)?\d{6,}/($NAME)(?=/|$STOP)"""),    // Axis, Canara: "UPI/P2A/6123.../RAHUL"
        rx("""\b(?:to|towards)\s+($NAME)(?=$STOP)"""),
        rx("""\bat\s+($NAME)(?=$STOP)"""),                               // cards: "spent ... at NETFLIX on ..."
        rx("""($VPA)"""),
    )
    private val notAPayee = rx("""^(?:your|you|the|beneficiary)\b""")
    private val whitespace = Regex("""\s+""")

    private val atmHint = rx("""\batm\b|withdrawn|cash withdrawal""")
    private val upiHint = rx("""\b(?:upi|vpa)\b""")
    private val cardHint = rx("""\bcard\b""")
    private val bankHint = rx("""\b(?:neft|imps|rtgs|net ?banking|mob bk)\b""")
    private val walletHint = rx("""wallet|paytm balance""")

    private val companySuffix = rx("""(?:\.com|\.co\.in|\.in|\s+(?:pvt\.?|private)?\s*(?:ltd|limited)\.?)$""")
    private val nonAlphanumeric = Regex("""[^a-z0-9]""")

    fun parse(sender: String, body: String, timestamp: Long): Transaction? {
        if (personalNumber.matches(sender.trim())) return null
        val text = disclaimer.find(body)?.let { body.substring(0, it.range.first) } ?: body
        if (notACompletedDebit.containsMatchIn(text)) return null
        val debit = debitWord.find(text) ?: return null
        val credit = creditWord.find(text)
        if (credit != null && credit.range.first < debit.range.first) return null
        if (!instrument.containsMatchIn(text)) return null
        val paise = amountIn(text)?.takeIf { it > 0 } ?: return null

        val payee = payeeIn(text)
        val channel = channelOf(text, body, payee)
        val name = payee ?: if (channel == Channel.ATM) "ATM withdrawal" else UNKNOWN_PAYEE
        return Transaction(
            amountPaise = paise,
            payee = name,
            payeeKey = keyOf(name),
            channel = channel,
            reference = reference.find(body)?.groupValues?.get(1),
            timestamp = timestamp,
            sender = sender,
            body = body,
        )
    }

    /** Normalises a payee so "NETFLIX.COM" and "Netflix" group together; UPI IDs are kept whole. */
    fun keyOf(payee: String): String {
        val p = payee.lowercase().trim()
        if ('@' in p) return p
        return companySuffix.replace(p, "").replace(nonAlphanumeric, "")
    }

    /**
     * Drops the second SMS about one payment, e.g. the bank and Paytm both notifying.
     * Input must be sorted newest first.
     */
    fun dedupe(newestFirst: List<Transaction>): List<Transaction> {
        val kept = ArrayList<Transaction>()
        for (t in newestFirst) {
            val duplicate = kept.asReversed().asSequence()
                .takeWhile { it.timestamp - t.timestamp <= DUPLICATE_WINDOW_MS }
                .any { sameEvent(it, t) }
            if (!duplicate) kept += t
        }
        return kept
    }

    private fun sameEvent(a: Transaction, b: Transaction): Boolean =
        if (a.reference != null && b.reference != null) a.reference == b.reference
        else a.amountPaise == b.amountPaise && a.sender != b.sender

    private fun amountIn(text: String): Long? {
        for (m in amount.findAll(text)) {
            val before = text.substring(maxOf(0, m.range.first - 14), m.range.first)
            if (balanceLabel.containsMatchIn(before)) continue
            return toPaise(m.groupValues[1])
        }
        return bareDebitAmount.find(text)?.let { toPaise(it.groupValues[1]) }
    }

    private fun toPaise(number: String): Long? =
        number.replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()

    private fun payeeIn(text: String): String? {
        for (pattern in payeePatterns) {
            for (m in pattern.findAll(text)) {
                val candidate = m.groupValues[1].trim().trimEnd('.', '-', '/', '*').replace(whitespace, " ")
                if (candidate.length >= 2 && !notAPayee.containsMatchIn(candidate)) return candidate
            }
        }
        return null
    }

    // [text] is the SMS without its disclaimer; some banks (HDFC UPI) only say "UPI" in the disclaimer.
    private fun channelOf(text: String, body: String, payee: String?): Channel = when {
        atmHint.containsMatchIn(text) -> Channel.ATM
        upiHint.containsMatchIn(text) || payee?.contains('@') == true -> Channel.UPI
        cardHint.containsMatchIn(text) -> Channel.CARD
        bankHint.containsMatchIn(text) -> Channel.BANK_TRANSFER
        walletHint.containsMatchIn(text) -> Channel.WALLET
        upiHint.containsMatchIn(body) -> Channel.UPI
        else -> Channel.OTHER
    }
}
