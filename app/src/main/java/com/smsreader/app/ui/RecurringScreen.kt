package com.smsreader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smsreader.app.R
import com.smsreader.app.analysis.Recurring
import kotlin.math.ceil

private const val DAY_MS = 86_400_000L

@Composable
fun RecurringScreen(recurring: List<Recurring>, onOpen: (Recurring) -> Unit, now: Long = System.currentTimeMillis()) {
    val (active, lapsed) = recurring.partition { !it.lapsed(now) }
    val perMonth = active.sumOf { it.perMonthPaise }

    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
        item { MonthlyCard(perMonth, active.size) }
        if (recurring.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_event_repeat_outline,
                    title = "No regular payments yet",
                    body = "A payee shows up here after at least 3 payments on a weekly, monthly, quarterly or yearly cycle.",
                )
            }
        }
        if (active.isNotEmpty()) {
            item { SectionTitle("Coming up") }
            item {
                Panel {
                    active.forEachIndexed { i, r ->
                        if (i > 0) RowDivider()
                        RecurringRow(r, now, lapsed = false, onClick = { onOpen(r) })
                    }
                }
            }
        }
        if (lapsed.isNotEmpty()) {
            item { SectionTitle("Not seen recently") }
            item {
                Text(
                    "Probably cancelled or paid another way. Their cost isn't in the monthly total.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )
            }
            item {
                Panel {
                    lapsed.forEachIndexed { i, r ->
                        if (i > 0) RowDivider()
                        RecurringRow(r, now, lapsed = true, onClick = { onOpen(r) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCard(perMonthPaise: Long, payees: Int) {
    val colors = spendColors
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.hero)
            .padding(20.dp),
    ) {
        Text("Regular payments", style = MaterialTheme.typography.titleSmall, color = colors.onHeroMuted)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
            Amount(perMonthPaise, style = MaterialTheme.typography.displaySmall, color = colors.onHero)
            Text(
                " a month",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onHeroMuted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            (if (payees == 1) "1 payee" else "$payees payees") + " you pay on a cycle. Two UPI IDs of the same person? " +
                "Give them the same name and they merge.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onHeroMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun RecurringRow(r: Recurring, now: Long, lapsed: Boolean, onClick: () -> Unit) {
    val nameColor = if (lapsed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CategoryTile(r.latest.category)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.latest.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Amount(r.typicalPaise, style = MaterialTheme.typography.titleSmall, color = nameColor)
            }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (lapsed) "${r.frequency.label}, last paid ${formatDate(r.latest.txn.timestamp)}" else r.frequency.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!lapsed) DuePill(r.nextExpected, now)
            }
            Text(
                "${paymentsText(r.count)} since ${formatDate(r.since)}, ${rupees(r.totalPaise)} in all",
                style = MaterialTheme.typography.bodySmall,
                color = spendColors.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** When the next payment is due; late ones get the warning colour, an icon and the word "late". */
@Composable
private fun DuePill(nextExpected: Long, now: Long) {
    val days = ceil((nextExpected - now).toDouble() / DAY_MS).toInt()
    val late = days < 0
    val text = when {
        late -> if (days == -1) "1 day late" else "${-days} days late"
        days == 0 -> "Due today"
        days == 1 -> "Due tomorrow"
        else -> "Due in $days days"
    }
    val fg = if (late) spendColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (late) spendColors.warningContainer else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (late) {
            Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
