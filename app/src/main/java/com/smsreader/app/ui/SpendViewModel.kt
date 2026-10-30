package com.smsreader.app.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smsreader.app.analysis.Period
import com.smsreader.app.analysis.Recurring
import com.smsreader.app.analysis.RecurringDetector
import com.smsreader.app.data.Category
import com.smsreader.app.data.PayeeTag
import com.smsreader.app.data.PayeeTags
import com.smsreader.app.data.Payment
import com.smsreader.app.data.Transaction
import com.smsreader.app.data.readDebitTransactions
import com.smsreader.app.parse.Categorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SpendViewModel(app: Application) : AndroidViewModel(app) {
    private val tags = PayeeTags(app)
    private var transactions: List<Transaction> = emptyList()
    private var loadJob: Job? = null

    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var smsScanned by mutableIntStateOf(0)
        private set
    /** Every payment found, newest first. */
    var payments by mutableStateOf<List<Payment>>(emptyList())
        private set
    var recurring by mutableStateOf<List<Recurring>>(emptyList())
        private set
    var period by mutableStateOf(Period.THIS_MONTH)
        private set
    var customRange by mutableStateOf<Pair<LocalDate, LocalDate>?>(null)
        private set

    /** Payments inside the selected period, newest first. */
    val visiblePayments: List<Payment> by derivedStateOf {
        val window = period.window(LocalDate.now(), customRange)
        if (window == null) payments else payments.filter { it.txn.timestamp in window }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loading = true
            val scan = withContext(Dispatchers.IO) { readDebitTransactions(getApplication<Application>().contentResolver) }
            transactions = scan.transactions
            smsScanned = scan.smsScanned
            rebuild()
            loading = false
            loaded = true
        }
    }

    fun setPeriod(period: Period, customRange: Pair<LocalDate, LocalDate>? = this.customRange) {
        this.period = period
        this.customRange = customRange
    }

    fun tagFor(tagKey: String): PayeeTag = tags.get(tagKey)

    /** A blank [label] removes the custom name. */
    fun saveTag(tagKey: String, label: String, category: Category?) {
        tags.set(tagKey, label.trim().ifEmpty { null }, category)
        viewModelScope.launch { rebuild() }
    }

    private suspend fun rebuild() {
        val (enriched, found) = withContext(Dispatchers.Default) {
            val enriched = transactions.map(::enrich)
            enriched to RecurringDetector.detect(enriched)
        }
        payments = enriched
        recurring = found
    }

    private fun enrich(t: Transaction): Payment {
        val auto = Categorizer.classify(t.payee, t.channel)
        val tagKey = auto.brand?.let { "brand:" + it.lowercase() } ?: t.payeeKey
        val tag = tags.get(tagKey)
        return Payment(
            txn = t,
            tagKey = tagKey,
            groupKey = tag.label?.let { "label:" + it.lowercase() } ?: tagKey,
            name = tag.label ?: auto.brand ?: t.payee,
            category = tag.category ?: auto.category,
        )
    }
}
