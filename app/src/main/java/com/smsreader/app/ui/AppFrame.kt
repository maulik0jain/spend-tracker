package com.smsreader.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smsreader.app.R

enum class Tab(val label: String, @DrawableRes val icon: Int, @DrawableRes val selectedIcon: Int) {
    OVERVIEW("Overview", R.drawable.ic_home_outline, R.drawable.ic_home),
    PAYMENTS("Payments", R.drawable.ic_receipt_long_outline, R.drawable.ic_receipt_long),
    RECURRING("Recurring", R.drawable.ic_event_repeat_outline, R.drawable.ic_event_repeat),
}

/** Top bar, tab bar and the screen between them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendScaffold(
    tab: Tab,
    onTab: (Tab) -> Unit,
    showNavigation: Boolean,
    onRefresh: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Spend Tracker", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(painterResource(R.drawable.ic_refresh_outline), contentDescription = "Reload SMS")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background, scrolledContainerColor = colors.background),
            )
        },
        bottomBar = {
            if (showNavigation) {
                NavigationBar(containerColor = spendColors.panel, tonalElevation = 0.dp) {
                    Tab.entries.forEach { t ->
                        val selected = tab == t
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTab(t) },
                            icon = { Icon(painterResource(if (selected) t.selectedIcon else t.icon), contentDescription = null) },
                            label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = colors.secondaryContainer,
                                selectedIconColor = colors.onSecondaryContainer,
                                selectedTextColor = colors.onSurface,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize(), content = content)
    }
}

@Composable
fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        IconTile(R.drawable.ic_sms_outline, tint = MaterialTheme.colorScheme.primary, background = MaterialTheme.colorScheme.primaryContainer, size = 64.dp)
        Text("See where your money goes", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Spend Tracker reads the SMS your bank, UPI apps and wallets send when money leaves your " +
                "account, and turns them into a spending summary.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Everything stays on this phone. The app has no internet permission, so your messages " +
                "can't be sent anywhere.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Allow SMS access", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open app settings") }
        Text(
            "No permission prompt? Android blocks SMS access for apps installed from a file. In app " +
                "settings, tap ⋮, then Allow restricted settings, then Permissions, SMS, Allow.",
            style = MaterialTheme.typography.bodyMedium,
            color = spendColors.muted,
        )
    }
}
