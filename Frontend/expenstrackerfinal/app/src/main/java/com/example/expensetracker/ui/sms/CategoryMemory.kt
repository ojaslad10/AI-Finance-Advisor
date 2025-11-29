package com.example.expensetracker.ui.sms

import android.content.Context
import android.content.SharedPreferences

object CategoryMemory {
    private const val PREFS = "category_memory_prefs"
    private const val KEY_PREFIX = "map_"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun norm(s: String?): String =
        (s ?: "").trim().lowercase().replace(Regex("\\s+"), " ").take(80)

    fun getCategoryFor(ctx: Context, receiverOrTitle: String?): String? {
        val key = KEY_PREFIX + norm(receiverOrTitle)
        return prefs(ctx).getString(key, null)
    }

    fun remember(ctx: Context, receiverOrTitle: String?, category: String) {
        val key = KEY_PREFIX + norm(receiverOrTitle)
        prefs(ctx).edit().putString(key, category).apply()
    }
}
