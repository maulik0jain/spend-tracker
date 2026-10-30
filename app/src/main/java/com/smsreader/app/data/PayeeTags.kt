package com.smsreader.app.data

import android.content.Context
import androidx.core.content.edit

/** The user's own name and category for a payee, e.g. "son@okaxis" -> "Son – pocket money". */
data class PayeeTag(val label: String?, val category: Category?)

class PayeeTags(context: Context) {
    private val prefs = context.getSharedPreferences("payee_tags", Context.MODE_PRIVATE)

    fun get(key: String) = PayeeTag(
        label = prefs.getString("label:$key", null),
        category = prefs.getString("category:$key", null)?.let { name -> Category.entries.firstOrNull { it.name == name } },
    )

    fun set(key: String, label: String?, category: Category?) {
        prefs.edit {
            if (label == null) remove("label:$key") else putString("label:$key", label)
            if (category == null) remove("category:$key") else putString("category:$key", category.name)
        }
    }
}
