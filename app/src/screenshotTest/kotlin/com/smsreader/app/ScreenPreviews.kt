package com.smsreader.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Frequency
import com.smsreader.app.analysis.Period
import com.smsreader.app.analysis.Recurring
import com.smsreader.app.data.Category
import com.smsreader.app.data.Channel
import com.smsreader.app.data.Payment
import com.smsreader.app.data.Transaction
import com.smsreader.app.ui.OverviewScreen
import com.smsreader.app.ui.PaymentDetails
import com.smsreader.app.ui.PaymentsScreen
import com.smsreader.app.ui.PeriodSelection
import com.smsreader.app.ui.PermissionScreen
import com.smsreader.app.ui.RecurringScreen
import com.smsreader.app.ui.SpendScaffold
import com.smsreader.app.ui.SpendTheme
import com.smsreader.app.ui.Tab
import com.smsreader.app.ui.spendColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// Screenshot tests: ./gradlew updateDebugScreenshotTest writes the images to
// app/src/debug/screenshotTest/reference; validateDebugScreenshotTest compares against them.

private val today = LocalDate.of(2026, 10, 30)
private val now = at(10, 30, 12, 0)
private val thisMonth = PeriodSelection(Period.THIS_MONTH, null) { _, _ -> }

private fun at(month: Int, day: Int, hour: Int, minute: Int, year: Int = 2026) =
    LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun pay(name: String, payee: String, rupees: Double, category: Category, channel: Channel, ms: Long) = Payment(
    Transaction(
        amountPaise = Math.round(rupees * 100),
        payee = payee,
        payeeKey = payee.lowercase(),
        channel = channel,
        reference = (ms / 1000 % 1_000_000_000_000).toString(),
        timestamp = ms,
        sender = "AD-HDFCBK",
        body = "Sent Rs.${"%.2f".format(rupees)}\nFrom HDFC Bank A/C *4821\nTo $payee\nOn ${"%02d".format(LocalDate.ofEpochDay(ms / 86_400_000).dayOfMonth)}/10/26\n" +
            "Ref ${ms / 1000 % 1_000_000_000_000}\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
    ),
    tagKey = payee.lowercase(),
    groupKey = name.lowercase(),
    name = name,
    category = category,
)

private val son = "Aarav – pocket money"

private val payments = listOf(
    pay("Swiggy", "swiggy@icici", 437.0, Category.FOOD, Channel.UPI, at(10, 30, 9, 12)),
    pay("Rapido", "rapido.bike@axl", 118.0, Category.TRAVEL, Channel.UPI, at(10, 30, 8, 5)),
    pay("BigBasket", "bigbasket@hdfcbank", 2186.40, Category.GROCERIES, Channel.UPI, at(10, 29, 19, 40)),
    pay(son, "aarav.s@okaxis", 500.0, Category.PEOPLE, Channel.UPI, at(10, 29, 13, 2)),
    pay("Netflix", "NETFLIX.COM", 649.0, Category.SUBSCRIPTIONS, Channel.CARD, at(10, 28, 21, 15)),
    pay("HP Petrol Pump Andheri", "HP PETROL PUMP ANDHERI", 1850.0, Category.FUEL, Channel.CARD, at(10, 28, 18, 30)),
    pay("Apollo Pharmacy", "apollopharmacy@ybl", 612.80, Category.HEALTH, Channel.UPI, at(10, 27, 10, 45)),
    pay("Zomato", "zomato@kotak", 864.0, Category.FOOD, Channel.UPI, at(10, 26, 20, 10)),
    pay("Airtel", "airtelpayments@airtel", 719.0, Category.BILLS, Channel.UPI, at(10, 25, 12, 0)),
    pay("Amazon", "amazonpay@apl", 1349.0, Category.SHOPPING, Channel.UPI, at(10, 24, 17, 20)),
    pay(son, "aarav.s@okaxis", 500.0, Category.PEOPLE, Channel.UPI, at(10, 22, 9, 0)),
    pay("Meera Iyer", "meera.iyer84@oksbi", 2400.0, Category.PEOPLE, Channel.UPI, at(10, 21, 14, 33)),
    pay("BookMyShow", "bookmyshow@icici", 780.0, Category.ENTERTAINMENT, Channel.UPI, at(10, 20, 11, 11)),
    pay("DMart", "DMART AVENUE SUPERMARTS", 3412.50, Category.GROCERIES, Channel.CARD, at(10, 18, 16, 45)),
    pay(son, "aarav.s@okaxis", 500.0, Category.PEOPLE, Channel.UPI, at(10, 15, 9, 0)),
    pay("Cash withdrawal", "ATM", 5000.0, Category.CASH, Channel.ATM, at(10, 14, 8, 30)),
    pay("Kumar Electronics", "kumarelectronics@ybl", 9240.0, Category.SHOPPING, Channel.UPI, at(10, 12, 13, 13)),
    pay("Tata Power", "tatapower@hdfcbank", 2315.0, Category.BILLS, Channel.UPI, at(10, 10, 10, 0)),
    pay(son, "aarav.s@okaxis", 500.0, Category.PEOPLE, Channel.UPI, at(10, 8, 9, 0)),
    pay("Spotify", "SPOTIFY INDIA", 119.0, Category.SUBSCRIPTIONS, Channel.CARD, at(10, 7, 21, 0)),
    pay("Swiggy", "swiggy@icici", 389.0, Category.FOOD, Channel.UPI, at(10, 5, 19, 20)),
    pay("Uber", "uber.india@hdfcbank", 342.0, Category.TRAVEL, Channel.UPI, at(10, 3, 20, 0)),
    pay("CRED", "cred.club@axisb", 12000.0, Category.CARD_BILL, Channel.UPI, at(10, 2, 10, 0)),
    pay(son, "aarav.s@okaxis", 500.0, Category.PEOPLE, Channel.UPI, at(10, 1, 9, 0)),
)

private fun recurring(latest: Payment, frequency: Frequency, count: Int, since: Long, perMonth: Long) = Recurring(
    latest = latest,
    frequency = frequency,
    typicalPaise = latest.txn.amountPaise,
    count = count,
    totalPaise = latest.txn.amountPaise * count,
    since = since,
    perMonthPaise = perMonth,
    nextExpected = latest.txn.timestamp + frequency.periodMs,
)

private val regulars = listOf(
    recurring(pay("Society maintenance", "greenviewchs@sbi", 3500.0, Category.BILLS, Channel.UPI, at(9, 26, 10, 0)), Frequency.MONTHLY, 14, at(8, 25, 10, 0, 2025), 3500_00),
    recurring(payments[3], Frequency.WEEKLY, 22, at(5, 27, 9, 0), 2173_00),
    recurring(payments[19], Frequency.MONTHLY, 19, at(4, 7, 21, 0, 2025), 119_00),
    recurring(payments[17], Frequency.MONTHLY, 9, at(2, 10, 10, 0), 2315_00),
    recurring(payments[8], Frequency.MONTHLY, 11, at(12, 25, 12, 0, 2025), 719_00),
    recurring(payments[4], Frequency.MONTHLY, 8, at(3, 28, 21, 15), 649_00),
    recurring(pay("Cult.fit", "cultfit@hdfcbank", 1299.0, Category.HEALTH, Channel.UPI, at(6, 14, 7, 30)), Frequency.MONTHLY, 6, at(1, 14, 7, 30), 1299_00),
    recurring(pay("JioHotstar", "JIOHOTSTAR", 499.0, Category.SUBSCRIPTIONS, Channel.CARD, at(4, 2, 22, 0)), Frequency.QUARTERLY, 4, at(7, 2, 22, 0, 2025), 166_00),
).sortedBy { it.nextExpected }

@Composable
private fun Frame(tab: Tab, dark: Boolean, content: @Composable BoxScope.() -> Unit) {
    SpendTheme(dark = dark) { SpendScaffold(tab, onTab = {}, showNavigation = true, onRefresh = {}, content = content) }
}

@Composable
private fun Overview(dark: Boolean) = Frame(Tab.OVERVIEW, dark) {
    OverviewScreen(payments, thisMonth, smsScanned = 4812, totalFound = 1204, onPayee = {}, today = today)
}

@Composable
private fun PaymentsList(dark: Boolean) = Frame(Tab.PAYMENTS, dark) { PaymentsScreen(payments, thisMonth, onOpen = {}, today = today) }

@Composable
private fun Regulars(dark: Boolean) = Frame(Tab.RECURRING, dark) { RecurringScreen(regulars, onOpen = {}, now = now) }

@Preview(name = "overview", widthDp = 400, heightDp = 2000)
@Composable
fun OverviewLightPreview() = Overview(dark = false)

@Preview(name = "overview-dark", widthDp = 400, heightDp = 2000)
@Composable
fun OverviewDarkPreview() = Overview(dark = true)

@Preview(name = "overview-phone", widthDp = 400, heightDp = 880)
@Composable
fun OverviewPhonePreview() = Overview(dark = false)

@Preview(name = "payments", widthDp = 400, heightDp = 880)
@Composable
fun PaymentsLightPreview() = PaymentsList(dark = false)

@Preview(name = "payments-dark", widthDp = 400, heightDp = 880)
@Composable
fun PaymentsDarkPreview() = PaymentsList(dark = true)

@Preview(name = "recurring", widthDp = 400, heightDp = 1100)
@Composable
fun RecurringLightPreview() = Regulars(dark = false)

@Preview(name = "recurring-dark", widthDp = 400, heightDp = 1100)
@Composable
fun RecurringDarkPreview() = Regulars(dark = true)

@Preview(name = "payment-details", widthDp = 400, heightDp = 880)
@Composable
fun PaymentDetailsPreview() = SpendTheme(dark = false) {
    Box(Modifier.background(spendColors.panel).padding(top = 24.dp)) { PaymentDetails(payments[2], onEditPayee = {}) }
}

@Preview(name = "permission", widthDp = 400, heightDp = 880)
@Composable
fun PermissionPreview() = Frame(Tab.OVERVIEW, dark = false) { PermissionScreen(onAllow = {}, onOpenSettings = {}) }

@Preview(name = "overview-large-font", widthDp = 400, heightDp = 1000, fontScale = 1.5f)
@Composable
fun OverviewLargeFontPreview() = Overview(dark = false)
