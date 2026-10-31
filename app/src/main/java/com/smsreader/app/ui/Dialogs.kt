package com.smsreader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smsreader.app.R
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
fun PaymentSheet(payment: Payment, onDismiss: () -> Unit, onEditPayee: () -> Unit, onAddToSheets: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = spendColors.panel) {
        PaymentDetails(payment, onEditPayee, onAddToSheets, Modifier.verticalScroll(rememberScrollState()))
    }
}

/** Everything about one payment, including the SMS it came from. */
@Composable
fun PaymentDetails(payment: Payment, onEditPayee: () -> Unit, onAddToSheets: () -> Unit, modifier: Modifier = Modifier) {
    val txn = payment.txn
    Column(
        modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(payment.category, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(payment.name, style = MaterialTheme.typography.titleMedium)
                Text(payment.category.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Amount(
            txn.amountPaise,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(formatDateTime(txn.timestamp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Column(
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Detail("Paid to", txn.payee)
            Detail("Method", txn.channel.label)
            txn.reference?.let { Detail("Reference", it) }
            Detail("SMS from", txn.sender)
        }

        Row(Modifier.padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sms_outline), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("The SMS", style = MaterialTheme.typography.titleSmall)
        }
        // Drawn as a received message: square corner where the bubble's tail would be.
        Text(
            txn.body,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(14.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddToSheets, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(painterResource(R.drawable.ic_table_view_outline), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add to Google Sheets", style = MaterialTheme.typography.labelLarge)
        }
        if (txn.payee != TransactionParser.UNKNOWN_PAYEE) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onEditPayee, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_edit_outline), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Name or categorise this payee", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.padding(vertical = 8.dp)) {
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
        containerColor = spendColors.panel,
        title = { Text("Name this payee", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Paid to ${target.payee}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Your name for it") },
                    placeholder = { Text("e.g. Son – pocket money") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                )
                Text("Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.label) },
                            leadingIcon = {
                                Icon(painterResource(iconOf(c)), contentDescription = null, tint = iconColorOf(c), modifier = Modifier.size(18.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorOf(c).copy(alpha = if (spendColors.dark) 0.28f else 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(label, category) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
