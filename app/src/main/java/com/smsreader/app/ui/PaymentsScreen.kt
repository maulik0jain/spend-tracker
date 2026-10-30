package com.smsreader.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smsreader.app.data.Payment

@Composable
fun PaymentsScreen(vm: SpendViewModel, onOpen: (Payment) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val payments = vm.visiblePayments
    val shown = remember(payments, query) {
        if (query.isBlank()) payments
        else payments.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.txn.payee.contains(query, ignoreCase = true) ||
                it.category.label.contains(query, ignoreCase = true)
        }
    }
    Column {
        PeriodBar(vm)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search payee or category") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        Text(
            "${paymentsText(shown.size)} · ${rupees(shown.sumOf { it.txn.amountPaise })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn {
            items(shown) { payment ->
                PaymentRow(payment, onClick = { onOpen(payment) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(payment.name, maxLines = 1) },
        supportingContent = {
            Text("${payment.category.label} · ${payment.txn.channel.label} · ${formatDateTime(payment.txn.timestamp)}")
        },
        trailingContent = { Text(rupees(payment.txn.amountPaise), fontWeight = FontWeight.SemiBold) },
    )
}
