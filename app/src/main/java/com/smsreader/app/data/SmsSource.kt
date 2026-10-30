package com.smsreader.app.data

import android.content.ContentResolver
import android.provider.Telephony
import com.smsreader.app.parse.TransactionParser

data class SmsScan(val transactions: List<Transaction>, val smsScanned: Int)

/** Reads the whole SMS inbox and keeps the messages that are debits. Needs READ_SMS. */
fun readDebitTransactions(resolver: ContentResolver): SmsScan {
    val found = ArrayList<Transaction>()
    var scanned = 0
    resolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
        null,
        null,
        "${Telephony.Sms.DATE} DESC",
    )?.use { cursor ->
        val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
        while (cursor.moveToNext()) {
            scanned++
            val sender = cursor.getString(address) ?: continue
            val text = cursor.getString(body) ?: continue
            TransactionParser.parse(sender, text, cursor.getLong(date))?.let(found::add)
        }
    }
    return SmsScan(TransactionParser.dedupe(found), scanned)
}
