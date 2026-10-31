package com.smsreader.app.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.smsreader.app.data.Payment

/**
 * Hands rows to the Google Sheets app without any network access from this app: the rows go
 * on the clipboard and Sheets opens, where the user picks a spreadsheet and pastes. Sheets
 * itself syncs the spreadsheet to the user's Google account whenever it is online.
 */
object Sheets {
    // Declared in <queries> in the manifest so Android 11+ lets us see whether it is installed.
    private const val PACKAGE = "com.google.android.apps.docs.editors.sheets"

    fun add(context: Context, payment: Payment) =
        send(context, listOf(SheetRow.cells(payment)), "Copied. In Sheets, tap an empty cell in column A, then Paste.")

    fun addHeadings(context: Context) =
        send(context, listOf(SheetRow.headings), "Headings copied. In Sheets, tap cell A1, then Paste.")

    private fun send(context: Context, rows: List<List<String>>, howToPaste: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newHtmlText("Spend Tracker", SheetRow.tsv(rows), SheetRow.html(rows)))

        val sheets = context.packageManager.getLaunchIntentForPackage(PACKAGE)
        if (sheets != null) {
            context.startActivity(sheets.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(context, howToPaste, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Copied. Install Google Sheets, then paste it into a spreadsheet.", Toast.LENGTH_LONG).show()
            openStoreListing(context)
        }
    }

    private fun openStoreListing(context: Context) {
        val listing = listOf("market://details?id=$PACKAGE", "https://play.google.com/store/apps/details?id=$PACKAGE")
        for (uri in listing) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // No store app; try the next way of showing the listing.
            }
        }
    }
}
