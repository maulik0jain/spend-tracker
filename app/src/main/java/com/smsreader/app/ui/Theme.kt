package com.smsreader.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smsreader.app.R
import com.smsreader.app.data.Category

// A fixed palette instead of wallpaper colours: neutrals tinted towards the app's green, one
// accent, and one colour per category. Text contrast is at least 4.5:1 everywhere.

private val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF17694A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEE2),
    onPrimaryContainer = Color(0xFF0B3B29),
    secondary = Color(0xFF17694A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EEE2),
    onSecondaryContainer = Color(0xFF0B3B29),
    background = Color(0xFFF3F5F4),
    onBackground = Color(0xFF121715),
    surface = Color(0xFFF3F5F4),
    onSurface = Color(0xFF121715),
    surfaceVariant = Color(0xFFE3E8E5),
    onSurfaceVariant = Color(0xFF46524D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEAEFEC),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFDFE5E2),
    outline = Color(0xFF7C8883),
    outlineVariant = Color(0xFFD7DEDA),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD3A6),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF1B4D38),
    onPrimaryContainer = Color(0xFFC9F0DD),
    secondary = Color(0xFF6FD3A6),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF1B4D38),
    onSecondaryContainer = Color(0xFFC9F0DD),
    background = Color(0xFF0E1211),
    onBackground = Color(0xFFE8EDEA),
    surface = Color(0xFF0E1211),
    onSurface = Color(0xFFE8EDEA),
    surfaceVariant = Color(0xFF232A27),
    onSurfaceVariant = Color(0xFFB7C2BD),
    surfaceContainerLowest = Color(0xFF161B19),
    surfaceContainerLow = Color(0xFF161B19),
    surfaceContainer = Color(0xFF1A201E),
    surfaceContainerHigh = Color(0xFF222926),
    surfaceContainerHighest = Color(0xFF2A322F),
    outline = Color(0xFF6F7B76),
    outlineVariant = Color(0xFF2C3531),
    error = Color(0xFFF2B8B5),
)

/** Colours Material's scheme has no slot for. */
@Immutable
data class SpendColors(
    val dark: Boolean,
    /** The white (or near-black) panels that group rows on the tinted page. */
    val panel: Color,
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    /** For text that is least important; still at least 4.5:1. */
    val muted: Color,
    /** "Payment is late" and similar; always shown with an icon and words. */
    val warning: Color,
    val warningContainer: Color,
    /** Payment-size bars, smallest bucket first: one hue, getting stronger with size. */
    val sizeRamp: List<Color>,
)

private val LightSpendColors = SpendColors(
    dark = false,
    panel = Color.White,
    hero = Color(0xFF17694A),
    onHero = Color.White,
    onHeroMuted = Color(0xFFCFE9DC),
    muted = Color(0xFF66726D),
    warning = Color(0xFF9A3412),
    warningContainer = Color(0xFFFCE8DC),
    sizeRamp = listOf(Color(0xFF86BFA6), Color(0xFF52A07F), Color(0xFF2B805F), Color(0xFF125A3E)),
)

private val DarkSpendColors = SpendColors(
    dark = true,
    panel = Color(0xFF161B19),
    hero = Color(0xFF1B4D38),
    onHero = Color(0xFFE6F4EC),
    onHeroMuted = Color(0xFFA9D9C1),
    muted = Color(0xFF8D9994),
    warning = Color(0xFFFDBA8C),
    warningContainer = Color(0xFF45261A),
    sizeRamp = listOf(Color(0xFF2E7457), Color(0xFF3F9C74), Color(0xFF57BC90), Color(0xFF8FE3BC)),
)

private val LocalSpendColors = staticCompositionLocalOf { LightSpendColors }

val spendColors: SpendColors
    @Composable @ReadOnlyComposable get() = LocalSpendColors.current

private val SpendTypography: Typography = Typography().run {
    fun TextStyle.figtree() = copy(fontFamily = Figtree)
    Typography(
        displayLarge = displayLarge.figtree(),
        displayMedium = displayMedium.figtree(),
        displaySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.figtree(),
        headlineMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.25).sp),
        headlineSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

/** Tabular figures, so amounts in a column line up digit for digit. */
val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun SpendTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpendColors provides if (dark) DarkSpendColors else LightSpendColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = SpendTypography,
            content = content,
        )
    }
}

/**
 * One hue from the validated eight-colour data palette. [icon] is a darker (light theme) or
 * lighter (dark theme) step of the same hue so the glyph clears 3:1 on its tinted tile.
 */
private class Hue(val light: Long, val lightIcon: Long, val dark: Long, val darkIcon: Long)

private val Blue = Hue(0xFF2A78D6, 0xFF2A78D6, 0xFF3987E5, 0xFF3987E5)
private val Orange = Hue(0xFFEB6834, 0xFFD6551C, 0xFFD95926, 0xFFD95926)
private val Aqua = Hue(0xFF1BAF7A, 0xFF009360, 0xFF199E70, 0xFF199E70)
private val Yellow = Hue(0xFFEDA100, 0xFFBB7200, 0xFFC98500, 0xFFC98500)
private val Magenta = Hue(0xFFE87BA4, 0xFFC65C86, 0xFFD55181, 0xFFD55181)
private val Green = Hue(0xFF008300, 0xFF008300, 0xFF008300, 0xFF21931E)
private val Violet = Hue(0xFF6250D6, 0xFF6250D6, 0xFF9085E9, 0xFF9085E9)
private val Red = Hue(0xFFE34948, 0xFFDF4545, 0xFFE66767, 0xFFE66767)
private val Neutral = Hue(0xFF66726D, 0xFF46524D, 0xFF8D9994, 0xFFB7C2BD)

/**
 * Each category keeps its colour on every screen. There are more categories than distinct
 * hues, so a few share one; the icon and the label always say which category it is.
 */
private fun hueOf(category: Category): Hue = when (category) {
    Category.PEOPLE -> Blue
    Category.FOOD -> Orange
    Category.BILLS, Category.HEALTH -> Aqua
    Category.FUEL, Category.EDUCATION -> Yellow
    Category.SHOPPING, Category.ENTERTAINMENT -> Magenta
    Category.GROCERIES, Category.INVESTMENT -> Green
    Category.SUBSCRIPTIONS -> Violet
    Category.TRAVEL -> Red
    Category.CASH, Category.CARD_BILL, Category.OTHER -> Neutral
}

@DrawableRes
fun iconOf(category: Category): Int = when (category) {
    Category.SUBSCRIPTIONS -> R.drawable.ic_subscriptions
    Category.FOOD -> R.drawable.ic_restaurant
    Category.GROCERIES -> R.drawable.ic_shopping_cart
    Category.SHOPPING -> R.drawable.ic_shopping_bag
    Category.TRAVEL -> R.drawable.ic_commute
    Category.FUEL -> R.drawable.ic_local_gas_station
    Category.BILLS -> R.drawable.ic_receipt
    Category.ENTERTAINMENT -> R.drawable.ic_movie
    Category.HEALTH -> R.drawable.ic_health_and_safety
    Category.EDUCATION -> R.drawable.ic_school
    Category.INVESTMENT -> R.drawable.ic_trending_up
    Category.CASH -> R.drawable.ic_currency_rupee
    Category.PEOPLE -> R.drawable.ic_person
    Category.CARD_BILL -> R.drawable.ic_credit_card
    Category.OTHER -> R.drawable.ic_category
}

/** Bar and chart colour for a category. */
@Composable
@ReadOnlyComposable
fun colorOf(category: Category): Color = hueOf(category).let { Color(if (spendColors.dark) it.dark else it.light) }

/** Glyph colour for a category's icon tile. */
@Composable
@ReadOnlyComposable
fun iconColorOf(category: Category): Color = hueOf(category).let { Color(if (spendColors.dark) it.darkIcon else it.lightIcon) }
