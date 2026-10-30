package com.smsreader.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsreader.app.data.Payment
import com.smsreader.app.ui.EditTarget
import com.smsreader.app.ui.OverviewScreen
import com.smsreader.app.ui.PayeeEditor
import com.smsreader.app.ui.PaymentSheet
import com.smsreader.app.ui.PaymentsScreen
import com.smsreader.app.ui.RecurringScreen
import com.smsreader.app.ui.SpendTheme
import com.smsreader.app.ui.SpendViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpendTheme { App() } }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Home),
    PAYMENTS("Payments", Icons.AutoMirrored.Filled.List),
    RECURRING("Recurring", Icons.Default.DateRange),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: SpendViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasSmsPermission(context)) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    // Re-read the inbox whenever the app comes to the front, so new payments show up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = hasSmsPermission(context)
        if (granted) vm.load()
    }

    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    var openPayment by remember { mutableStateOf<Payment?>(null) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spend Tracker") },
                actions = {
                    if (granted) {
                        IconButton(onClick = vm::load) { Icon(Icons.Default.Refresh, contentDescription = "Reload SMS") }
                    }
                },
            )
        },
        bottomBar = {
            if (granted) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !granted -> PermissionScreen(
                    onAllow = { requestPermission.launch(Manifest.permission.READ_SMS) },
                    onOpenSettings = { openAppSettings(context) },
                )
                !vm.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {
                    when (tab) {
                        Tab.OVERVIEW -> OverviewScreen(vm, onPayee = { editing = EditTarget.of(it, vm) })
                        Tab.PAYMENTS -> PaymentsScreen(vm, onOpen = { openPayment = it })
                        Tab.RECURRING -> RecurringScreen(vm, onOpen = { editing = EditTarget.of(it.latest, vm) })
                    }
                    if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
        }
    }

    openPayment?.let { payment ->
        PaymentSheet(
            payment = payment,
            onDismiss = { openPayment = null },
            onEditPayee = {
                openPayment = null
                editing = EditTarget.of(payment, vm)
            },
        )
    }
    editing?.let { target ->
        PayeeEditor(
            target = target,
            onDismiss = { editing = null },
            onSave = { label, category ->
                vm.saveTag(target.tagKey, label, category)
                editing = null
            },
        )
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Track where your money goes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Spend Tracker reads the SMS your bank, UPI apps and wallets send when money leaves your " +
                "account, and turns them into a spending summary.",
        )
        Text(
            "Everything stays on this phone. The app has no internet permission, so your messages " +
                "cannot be sent anywhere.",
        )
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Allow SMS access") }
        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open app settings") }
        Text(
            "No permission prompt? Android blocks SMS access for apps installed from a file. In app " +
                "settings, tap ⋮ → Allow restricted settings, then Permissions → SMS → Allow.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun hasSmsPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
