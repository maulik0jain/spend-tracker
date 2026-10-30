package com.smsreader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smsreader.app.R
import com.smsreader.app.analysis.Period
import com.smsreader.app.analysis.Summary
import com.smsreader.app.analysis.Timeline
import com.smsreader.app.analysis.summarise
import com.smsreader.app.analysis.timeline
import com.smsreader.app.data.Category
import com.smsreader.app.data.Payment
import java.time.LocalDate

private const val TOP_PAYEES = 8

@Composable
fun OverviewScreen(
    payments: List<Payment>,
    selection: PeriodSelection,
    smsScanned: Int,
    totalFound: Int,
    onPayee: (Payment) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val summary = remember(payments) { summarise(payments) }
    val window = selection.period.window(today, selection.custom)
    val timeline = remember(payments, window) { timeline(payments, window) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PeriodBar(selection) }
        item { SpentCard(summary, timeline, spentTitle(selection)) }
        if (payments.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_receipt_long_outline,
                    title = "No payments in this period",
                    body = "Pick a longer period, or check that your bank sends an SMS when money leaves your account.",
                    action = if (selection.period != Period.ALL) {
                        { TextButton(onClick = { selection.onSelect(Period.ALL, selection.custom) }) { Text("Show all time") } }
                    } else {
                        null
                    },
                )
            }
        } else {
            val categories = summary.byCategory.filter { it.id != Category.CARD_BILL.name }
            if (categories.isNotEmpty()) {
                item { SectionTitle("Where it went") }
                item {
                    Panel {
                        categories.forEachIndexed { i, total ->
                            val category = Category.valueOf(total.id)
                            if (i > 0) RowDivider()
                            ShareRow(
                                label = total.label,
                                detail = percentText(total.paise, summary.spentPaise),
                                paise = total.paise,
                                whole = categories.first().paise,
                                barColor = colorOf(category),
                                leading = { CategoryTile(category) },
                            )
                        }
                    }
                }
            }

            val top = summary.byPayee.take(TOP_PAYEES)
            item { SectionTitle("Top payees", trailing = "Tap to name") }
            item {
                Panel {
                    top.forEachIndexed { i, total ->
                        val payment = payments.firstOrNull { it.groupKey == total.id }
                        val category = payment?.category ?: Category.OTHER
                        if (i > 0) RowDivider()
                        ShareRow(
                            label = total.label,
                            detail = paymentsText(total.count),
                            paise = total.paise,
                            whole = top.first().paise,
                            barColor = colorOf(category),
                            leading = { CategoryTile(category) },
                            onClick = payment?.let { { onPayee(it) } },
                        )
                    }
                }
            }

            item { SectionTitle("By payment size") }
            item {
                Panel {
                    val biggest = summary.byBucket.maxOf { it.paise }
                    summary.byBucket.forEachIndexed { i, total ->
                        if (i > 0) RowDivider(start = 16.dp)
                        ShareRow(
                            label = total.label,
                            detail = paymentsText(total.count),
                            paise = total.paise,
                            whole = biggest,
                            barColor = spendColors.sizeRamp[i],
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Read $smsScanned SMS and found ${paymentsText(totalFound)} in all.",
                style = MaterialTheme.typography.bodySmall,
                color = spendColors.muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )
        }
    }
}

private fun spentTitle(selection: PeriodSelection): String = when (selection.period) {
    Period.THIS_MONTH -> "Spent this month"
    Period.LAST_MONTH -> "Spent last month"
    Period.LAST_3_MONTHS -> "Spent in the last 3 months"
    Period.THIS_YEAR -> "Spent this year"
    Period.ALL -> "Spent in all"
    Period.CUSTOM -> selection.custom?.let { (a, b) -> "Spent ${formatShortDate(a)} – ${formatShortDate(b)}" } ?: "Spent"
}

/** The headline: total spent, three supporting figures, and spending over the period. */
@Composable
private fun SpentCard(summary: Summary, timeline: Timeline, title: String) {
    val colors = spendColors
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.hero)
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onHeroMuted)
        Amount(
            summary.spentPaise,
            style = MaterialTheme.typography.displaySmall,
            color = colors.onHero,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (summary.count > 0) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Payments", summary.count.toString(), colors.onHero, colors.onHeroMuted, Modifier.weight(1f))
                // Whole rupees: paise in an average is noise.
                Stat("Average", rupees((summary.spentPaise / summary.count + 50) / 100 * 100), colors.onHero, colors.onHeroMuted, Modifier.weight(1f))
                Stat("Largest", rupees(summary.largestPaise), colors.onHero, colors.onHeroMuted, Modifier.weight(1f))
            }
            SpendChart(timeline, color = colors.onHero, mutedColor = colors.onHeroMuted, modifier = Modifier.padding(top = 20.dp))
        }
        if (summary.cardBillPaise > 0) {
            Text(
                "Credit card bills (${rupees(summary.cardBillPaise)}) aren't counted, because the card spends are already in.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onHeroMuted,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, labelColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.merge(Tabular),
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
