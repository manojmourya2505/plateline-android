package com.inouiw.plateline.data

import android.content.Context

/** Remembers which phone number is logged in on this device. */
object Session {
    private const val PREFS = "plateline_session"
    private const val KEY_PHONE = "phone"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun phone(context: Context): String? = prefs(context).getString(KEY_PHONE, null)

    fun save(context: Context, phone: String) {
        prefs(context).edit().putString(KEY_PHONE, phone).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_PHONE).apply()
    }
}
