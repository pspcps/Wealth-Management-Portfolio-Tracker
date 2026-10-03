package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.FireSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FireSettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fire_settings_prefs", Context.MODE_PRIVATE)

    private val _fireSettings = MutableStateFlow(loadSettings())
    val fireSettings: StateFlow<FireSettings> = _fireSettings.asStateFlow()

    private fun loadSettings(): FireSettings {
        val ret = prefs.getFloat("fire_expected_return", 12.0f).toDouble()
        val inf = prefs.getFloat("fire_inflation", 6.0f).toDouble()
        val age = prefs.getInt("fire_current_age", 28)
        val targetAge = prefs.getInt("fire_target_age", 45)
        val expenses = prefs.getFloat("fire_monthly_expenses", 50000.0f).toDouble()
        val investment = prefs.getFloat("fire_monthly_investment", 50000.0f).toDouble()
        val swr = prefs.getFloat("fire_swr_rate", 3.5f).toDouble()
        val stepUp = prefs.getFloat("fire_step_up_rate", 8.0f).toDouble()

        return FireSettings(
            expectedReturnRate = ret,
            inflationRate = inf,
            currentAge = age,
            targetFireAge = targetAge,
            monthlyExpenses = expenses,
            monthlyInvestment = investment,
            swrRate = swr,
            annualStepUpRate = stepUp
        )
    }

    suspend fun updateFireSettings(settings: FireSettings) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putFloat("fire_expected_return", settings.expectedReturnRate.toFloat())
            .putFloat("fire_inflation", settings.inflationRate.toFloat())
            .putInt("fire_current_age", settings.currentAge)
            .putInt("fire_target_age", settings.targetFireAge)
            .putFloat("fire_monthly_expenses", settings.monthlyExpenses.toFloat())
            .putFloat("fire_monthly_investment", settings.monthlyInvestment.toFloat())
            .putFloat("fire_swr_rate", settings.swrRate.toFloat())
            .putFloat("fire_step_up_rate", settings.annualStepUpRate.toFloat())
            .apply()

        _fireSettings.value = settings
    }
}
