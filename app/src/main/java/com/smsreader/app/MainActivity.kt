package com.smsreader.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsreader.app.data.Payment
import com.smsreader.app.export.Sheets
import com.smsreader.app.ui.EditTarget
import com.smsreader.app.ui.OverviewScreen
import com.smsreader.app.ui.PayeeEditor
import com.smsreader.app.ui.PaymentSheet
import com.smsreader.app.ui.PaymentsScreen
import com.smsreader.app.ui.PeriodSelection
import com.smsreader.app.ui.PermissionScreen
import com.smsreader.app.ui.RecurringScreen
import com.smsreader.app.ui.SpendScaffold
import com.smsreader.app.ui.SpendTheme
import com.smsreader.app.ui.SpendViewModel
import com.smsreader.app.ui.Tab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The tab bar paints its own background down to the screen edge.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { SpendTheme { App() } }
    }
}

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
    val selection = PeriodSelection(vm.period, vm.customRange, vm::setPeriod)

    SpendScaffold(tab = tab, onTab = { tab = it }, showNavigation = granted, onRefresh = if (granted) vm::load else null) {
        when {
            !granted -> PermissionScreen(
                onAllow = { requestPermission.launch(Manifest.permission.READ_SMS) },
                onOpenSettings = { openAppSettings(context) },
            )
            !vm.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            else -> {
                when (tab) {
                    Tab.OVERVIEW -> OverviewScreen(
                        payments = vm.visiblePayments,
                        selection = selection,
                        smsScanned = vm.smsScanned,
                        totalFound = vm.payments.size,
                        onPayee = { editing = EditTarget.of(it, vm) },
                    )
                    Tab.PAYMENTS -> PaymentsScreen(
                        payments = vm.visiblePayments,
                        selection = selection,
                        onOpen = { openPayment = it },
                        onAddToSheets = { Sheets.add(context, it) },
                        onCopyHeadings = { Sheets.addHeadings(context) },
                    )
                    Tab.RECURRING -> RecurringScreen(vm.recurring, onOpen = { editing = EditTarget.of(it.latest, vm) })
                }
                if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
            onAddToSheets = {
                openPayment = null
                Sheets.add(context, payment)
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

private fun hasSmsPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
