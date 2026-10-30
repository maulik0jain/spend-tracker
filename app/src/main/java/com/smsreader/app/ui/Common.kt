package com.smsreader.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Period
import com.smsreader.app.data.Category
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
private val weekdayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val weekdayYearFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val monthFormat = DateTimeFormatter.ofPattern("MMM yyyy")

/** ₹1,00,000 or ₹649.50 — Indian digit grouping, decimals only when there are paise. */
fun rupees(paise: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
    return format.format(paise / 100.0)
}

fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatDateTime(ms: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun formatDate(ms: Long): String = dateFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun formatTime(ms: Long): String = timeFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun formatShortDate(date: LocalDate): String = shortDateFormat.format(date)

fun formatMonth(date: LocalDate): String = monthFormat.format(date)

/** "Today", "Yesterday", "Tue, 28 Oct", or with the year when it isn't this year. */
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> (if (date.year == today.year) weekdayFormat else weekdayYearFormat).format(date)
}

fun paymentsText(count: Int): String = if (count == 1) "1 payment" else "$count payments"

/** The period chips' state, shared by Overview and Payments. */
class PeriodSelection(
    val period: Period,
    val custom: Pair<LocalDate, LocalDate>?,
    val onSelect: (Period, Pair<LocalDate, LocalDate>?) -> Unit,
)

/** The period chips. "Custom" opens a date-range picker. */
@Composable
fun PeriodBar(selection: PeriodSelection) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Period.entries.forEach { period ->
            val range = selection.custom
            val label = if (period == Period.CUSTOM && selection.period == Period.CUSTOM && range != null) {
                "${formatShortDate(range.first)} – ${formatShortDate(range.second)}"
            } else {
                period.label
            }
            val selected = selection.period == period
            FilterChip(
                selected = selected,
                onClick = { if (period == Period.CUSTOM) picking = true else selection.onSelect(period, selection.custom) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = spendColors.panel,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
    if (picking) {
        RangePickerDialog(
            onDismiss = { picking = false },
            onPicked = { first, last ->
                selection.onSelect(Period.CUSTOM, first to last)
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

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A white (dark: charcoal) rounded group of rows on the tinted page. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(spendColors.panel),
        content = content,
    )
}

/** Hairline between rows of a [Panel], indented past the icon tile. */
@Composable
fun RowDivider(start: Dp = 72.dp) {
    HorizontalDivider(Modifier.padding(start = start), color = MaterialTheme.colorScheme.outlineVariant)
}

/** The category's icon on a tint of its colour. */
@Composable
fun CategoryTile(category: Category, size: Dp = 40.dp) {
    IconTile(iconOf(category), tint = iconColorOf(category), background = colorOf(category).copy(alpha = if (spendColors.dark) 0.22f else 0.14f), size = size)
}

@Composable
fun IconTile(@DrawableRes icon: Int, tint: Color, background: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun Amount(paise: Long, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleMedium, color: Color = Color.Unspecified) {
    Text(rupees(paise), modifier = modifier, style = style.merge(Tabular), color = color, maxLines = 1, textAlign = TextAlign.End)
}

/**
 * A labelled amount with a bar showing its share of [whole]. The bar's colour repeats the
 * tile's, so it is never the only thing saying which row is which.
 */
@Composable
fun ShareRow(
    label: String,
    detail: String,
    paise: Long,
    whole: Long,
    barColor: Color,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val share = if (whole > 0) paise.toFloat() / whole else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Amount(paise, style = MaterialTheme.typography.titleSmall)
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ShareBar(share, barColor, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                // Fixed width, so every bar in a list ends at the same x and lengths compare fairly.
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall.merge(Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(76.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareBar(share: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (share > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(share.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

/** Whole-number percentage, never showing 0% for a non-zero share. */
fun percentText(part: Long, whole: Long): String {
    if (whole <= 0 || part <= 0) return "0%"
    val pct = part * 100.0 / whole
    return if (pct < 1) "<1%" else "${pct.toInt()}%"
}

@Composable
fun EmptyState(@DrawableRes icon: Int, title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(icon, tint = MaterialTheme.colorScheme.primary, background = MaterialTheme.colorScheme.primaryContainer, size = 56.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (action != null) Box(Modifier.padding(top = 12.dp)) { action() }
    }
}
