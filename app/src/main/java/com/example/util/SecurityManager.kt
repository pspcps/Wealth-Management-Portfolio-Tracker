package com.example.util

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_APP_LOCK_PIN = "key_app_lock_pin"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_SYNC_ENABLED = "key_auto_sync_enabled"
        private const val KEY_LAST_AUTO_SYNC = "key_last_auto_sync"
        private const val KEY_HIDE_SALARY_DEFAULT = "key_hide_salary_default"

        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var appLockPin: String?
        get() = prefs.getString(KEY_APP_LOCK_PIN, null)
        set(value) = prefs.edit().putString(KEY_APP_LOCK_PIN, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()

    var lastAutoSyncTime: Long
        get() = prefs.getLong(KEY_LAST_AUTO_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTO_SYNC, value).apply()

    var isSalaryHiddenDefault: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SALARY_DEFAULT, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SALARY_DEFAULT, value).apply()

    fun verifyPin(inputPin: String): Boolean {
        val savedPin = appLockPin
        return if (savedPin.isNullOrEmpty()) {
            true // No PIN set
        } else {
            savedPin == inputPin
        }
    }

    fun setPin(pin: String) {
        appLockPin = pin
        isAppLockEnabled = true
    }

    fun disableAppLock() {
        isAppLockEnabled = false
        appLockPin = null
    }
}
