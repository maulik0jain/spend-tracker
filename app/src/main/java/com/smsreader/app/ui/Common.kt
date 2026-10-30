package com.smsreader.app.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Period
import com.smsreader.app.analysis.Total
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SpendTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM")

/** ₹1,00,000 or ₹649.50 — Indian digit grouping, decimals only when there are paise. */
fun rupees(paise: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
    return format.format(paise / 100.0)
}

fun formatDateTime(ms: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun formatDate(ms: Long): String = dateFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun paymentsText(count: Int): String = if (count == 1) "1 payment" else "$count payments"

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

/** A labelled amount with a bar showing its share of the largest row in [all]. */
@Composable
fun TotalRow(total: Total, all: List<Total>, onClick: (() -> Unit)? = null) {
    val max = all.maxOfOrNull { it.paise }?.takeIf { it > 0 } ?: 1
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(total.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    paymentsText(total.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(rupees(total.paise), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { total.paise.toFloat() / max },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

/** The period chips shared by Overview and Payments. "Custom" opens a date-range picker. */
@Composable
fun PeriodBar(vm: SpendViewModel) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Period.entries.forEach { period ->
            val range = vm.customRange
            val label = if (period == Period.CUSTOM && vm.period == Period.CUSTOM && range != null) {
                "${shortDateFormat.format(range.first)} – ${shortDateFormat.format(range.second)}"
            } else {
                period.label
            }
            FilterChip(
                selected = vm.period == period,
                onClick = { if (period == Period.CUSTOM) picking = true else vm.setPeriod(period) },
                label = { Text(label) },
            )
        }
    }
    if (picking) {
        RangePickerDialog(
            onDismiss = { picking = false },
            onPicked = { first, last ->
                vm.setPeriod(Period.CUSTOM, first to last)
                picking = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(onDismiss: () -> Unit, onPicked: (LocalDate, LocalDate) -> Unit) {
    val state = rememberDateRangePickerState()
    // The picker reports UTC midnight of each chosen day.
    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    state.selectedStartDateMillis?.let { start -> onPicked(day(start), day(state.selectedEndDateMillis ?: start)) }
                },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
