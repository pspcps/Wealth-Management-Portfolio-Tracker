package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Global Financial Privacy & Discreet Mode Manager.
 * By default, financial numbers and balances are masked across the entire app
 * (e.g. ₹ ••••••). Toggling the eye reveals the real numbers.
 */
object FinancialPrivacyManager {
    private const val PREFS_NAME = "financial_privacy_prefs"
    private const val KEY_HIDE_BY_DEFAULT = "key_hide_by_default"

    // Reactive Compose state - defaults to false (hidden by default)
    private val _isAmountsVisible = mutableStateOf(false)
    val isAmountsVisible: State<Boolean> = _isAmountsVisible

    val isMasked: Boolean
        get() = !_isAmountsVisible.value

    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            val prefs = getPrefs(context)
            val hideByDefault = prefs.getBoolean(KEY_HIDE_BY_DEFAULT, true)
            // If hideByDefault is true, start with amounts hidden (false)
            _isAmountsVisible.value = !hideByDefault
            initialized = true
        }
    }

    fun isHideByDefault(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HIDE_BY_DEFAULT, true)
    }

    fun setHideByDefault(context: Context, hideByDefault: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIDE_BY_DEFAULT, hideByDefault).apply()
        if (hideByDefault) {
            _isAmountsVisible.value = false
        }
    }

    fun toggleAmountsVisibility(context: Context? = null): Boolean {
        val newVisibleState = !_isAmountsVisible.value
        _isAmountsVisible.value = newVisibleState

        context?.let { ctx ->
            val msg = if (newVisibleState) {
                "👁 Balances revealed • Tap eye again to hide"
            } else {
                "🔒 Privacy Mode: Financial balances hidden"
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }

        return newVisibleState
    }

    fun setAmountsVisible(visible: Boolean) {
        _isAmountsVisible.value = visible
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
