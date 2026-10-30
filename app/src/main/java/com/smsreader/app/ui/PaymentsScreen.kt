package com.smsreader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smsreader.app.R
import com.smsreader.app.data.Payment
import java.time.LocalDate

@Composable
fun PaymentsScreen(
    payments: List<Payment>,
    selection: PeriodSelection,
    onOpen: (Payment) -> Unit,
    today: LocalDate = LocalDate.now(),
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val shown = remember(payments, query) {
        if (query.isBlank()) payments
        else payments.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.txn.payee.contains(query, ignoreCase = true) ||
                it.category.label.contains(query, ignoreCase = true)
        }
    }
    // payments are newest first, so days come out newest first too.
    val days = remember(shown) { shown.groupBy { localDate(it.txn.timestamp) }.toList() }

    Column(Modifier.fillMaxSize().imePadding()) {
        PeriodBar(selection)
        SearchField(query, onChange = { query = it })
        if (shown.isEmpty()) {
            EmptyState(
                icon = R.drawable.ic_search_outline,
                title = if (query.isBlank()) "No payments in this period" else "Nothing matches “$query”",
                body = if (query.isBlank()) "Pick a longer period above." else "Search looks at payee names, UPI IDs and categories.",
            )
            return@Column
        }
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(paymentsText(shown.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Amount(shown.sumOf { it.txn.amountPaise }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            days.forEach { (date, dayPayments) ->
                item(key = "day-$date") { DayHeader(dayLabel(date, today), dayPayments.sumOf { it.txn.amountPaise }) }
                itemsIndexed(dayPayments, key = { _, p -> "${p.txn.timestamp}-${p.txn.amountPaise}-${p.txn.payeeKey}" }) { i, payment ->
                    val top = if (i == 0) 20.dp else 0.dp
                    val bottom = if (i == dayPayments.lastIndex) 20.dp else 0.dp
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                            .background(spendColors.panel),
                    ) {
                        if (i > 0) RowDivider()
                        PaymentRow(payment, onClick = { onOpen(payment) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("Search payee or category", style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search_outline), contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onChange("") }) {
                    Icon(painterResource(R.drawable.ic_close_outline), contentDescription = "Clear search")
                }
            }
        } else {
            null
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = spendColors.panel,
            unfocusedContainerColor = spendColors.panel,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun DayHeader(label: String, paise: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Amount(paise, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTile(payment.category)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                payment.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                payment.category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Amount(payment.txn.amountPaise, style = MaterialTheme.typography.titleSmall)
            Text(formatTime(payment.txn.timestamp), style = MaterialTheme.typography.bodySmall, color = spendColors.muted)
        }
    }
}
