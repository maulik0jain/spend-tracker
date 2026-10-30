package com.smsreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smsreader.app.data.Category
import com.smsreader.app.data.Payment
import com.smsreader.app.parse.TransactionParser

/** The payee being named or re-categorised. */
data class EditTarget(val tagKey: String, val payee: String, val label: String?, val category: Category) {
    companion object {
        fun of(payment: Payment, vm: SpendViewModel) =
            EditTarget(payment.tagKey, payment.txn.payee, vm.tagFor(payment.tagKey).label, payment.category)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(payment: Payment, onDismiss: () -> Unit, onEditPayee: () -> Unit) {
    val txn = payment.txn
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(rupees(txn.amountPaise), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(payment.name, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.padding(vertical = 12.dp)) {
                Detail("Paid to", txn.payee)
                Detail("Category", payment.category.label)
                Detail("Method", txn.channel.label)
                Detail("When", formatDateTime(txn.timestamp))
                txn.reference?.let { Detail("Reference", it) }
                Detail("SMS from", txn.sender)
            }
            Text("Original SMS", style = MaterialTheme.typography.labelLarge)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
            ) {
                Text(txn.body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
            if (txn.payee != TransactionParser.UNKNOWN_PAYEE) {
                Button(onClick = onEditPayee, modifier = Modifier.fillMaxWidth()) {
                    Text("Name or categorise this payee")
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}

/** Name a payee ("Son – pocket money") and/or set its category. Applies to every payment to it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayeeEditor(target: EditTarget, onDismiss: () -> Unit, onSave: (label: String, category: Category) -> Unit) {
    var label by remember { mutableStateOf(target.label.orEmpty()) }
    var category by remember { mutableStateOf(target.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payee") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Paid to: ${target.payee}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Your name for it") },
                    placeholder = { Text("e.g. Son – pocket money") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(label, category) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
