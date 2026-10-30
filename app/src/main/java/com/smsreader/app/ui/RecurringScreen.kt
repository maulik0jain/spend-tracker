package com.smsreader.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Recurring

@Composable
fun RecurringScreen(vm: SpendViewModel, onOpen: (Recurring) -> Unit) {
    val now = System.currentTimeMillis()
    val (active, lapsed) = vm.recurring.partition { !it.lapsed(now) }
    val perMonth = active.sumOf { it.perMonthPaise }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Recurring payments", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "≈ ${rupees(perMonth)} a month",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Payees you pay on a regular cycle (at least 3 payments). " +
                            "Give a payee a name to merge it with others, e.g. two UPI IDs of the same person.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        if (active.isEmpty()) {
            item {
                Text(
                    "No regular payments found yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(active) { RecurringRow(it, lapsed = false, onClick = { onOpen(it) }) }
        if (lapsed.isNotEmpty()) {
            item { SectionTitle("Not seen recently — cancelled?") }
            items(lapsed) { RecurringRow(it, lapsed = true, onClick = { onOpen(it) }) }
        }
    }
}

@Composable
private fun RecurringRow(r: Recurring, lapsed: Boolean, onClick: () -> Unit) {
    val next = if (lapsed) "was due ${formatDate(r.nextExpected)}" else "next around ${formatDate(r.nextExpected)}"
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = { Text("${r.frequency.label} · ${r.latest.category.label}") },
        headlineContent = { Text(r.latest.name, maxLines = 1) },
        supportingContent = {
            Text(
                "Paid ${r.count} times since ${formatDate(r.since)}, ${rupees(r.totalPaise)} in all\n" +
                    "Last ${formatDate(r.latest.txn.timestamp)} · $next",
            )
        },
        trailingContent = { Text(rupees(r.typicalPaise), fontWeight = FontWeight.SemiBold) },
    )
    HorizontalDivider()
}
