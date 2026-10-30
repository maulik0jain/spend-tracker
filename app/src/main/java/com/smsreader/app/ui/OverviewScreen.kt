package com.smsreader.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Summary
import com.smsreader.app.analysis.summarise
import com.smsreader.app.data.Payment

private const val TOP_PAYEES = 10

@Composable
fun OverviewScreen(vm: SpendViewModel, onPayee: (Payment) -> Unit) {
    val payments = vm.visiblePayments
    val summary = remember(payments) { summarise(payments) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PeriodBar(vm) }
        item { SpentCard(summary) }
        if (payments.isEmpty()) {
            item {
                Text(
                    "No payments found in this period.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item { SectionTitle("By category") }
            items(summary.byCategory) { TotalRow(it, summary.byCategory) }

            item { SectionTitle("Top payees") }
            val top = summary.byPayee.take(TOP_PAYEES)
            items(top) { total ->
                TotalRow(total, top, onClick = { payments.firstOrNull { it.groupKey == total.id }?.let(onPayee) })
            }

            item { SectionTitle("By payment size") }
            items(summary.byBucket) { TotalRow(it, summary.byBucket) }
        }
        item {
            Text(
                "Scanned ${vm.smsScanned} SMS · ${paymentsText(vm.payments.size)} found in total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SpentCard(summary: Summary) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Spent", style = MaterialTheme.typography.labelLarge)
            Text(rupees(summary.spentPaise), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (summary.count > 0) {
                Text(
                    "${paymentsText(summary.count)} · average ${rupees(summary.spentPaise / summary.count)}" +
                        " · largest ${rupees(summary.largestPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (summary.cardBillPaise > 0) {
                Text(
                    "Not counting ${rupees(summary.cardBillPaise)} of credit-card bill payments " +
                        "(the card spends are already counted).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
