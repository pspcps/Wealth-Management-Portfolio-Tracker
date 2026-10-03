package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseInitializer
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfile
import com.example.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isOperating: Boolean = false,
    val operationSuccessMessage: String? = null,
    val userProfile: UserProfile = UserProfile(),
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val hasPin: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val lastAutoSyncTime: Long = 0L,
    val autoBackupState: com.example.util.AutoBackupState? = null,
    val backupFiles: List<com.example.util.BackupFileInfo> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)
    private val profileRepository = UserProfileRepository(application)
    private val fireSettingsRepo = com.example.data.repository.FireSettingsRepository(application)
    private val securityManager = com.example.util.SecurityManager.getInstance(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isAppLockEnabled = securityManager.isAppLockEnabled,
            isBiometricEnabled = securityManager.isBiometricEnabled,
            hasPin = !securityManager.appLockPin.isNullOrEmpty(),
            isAutoSyncEnabled = securityManager.isAutoSyncEnabled,
            lastAutoSyncTime = securityManager.lastAutoSyncTime
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
            refreshAutoBackupState()
            profileRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean, pin: String? = null) {
        if (enabled) {
            if (pin != null) {
                securityManager.setPin(pin)
            } else {
                securityManager.isAppLockEnabled = true
            }
        } else {
            securityManager.disableAppLock()
        }
        _uiState.update {
            it.copy(
                isAppLockEnabled = securityManager.isAppLockEnabled,
                hasPin = !securityManager.appLockPin.isNullOrEmpty(),
                operationSuccessMessage = if (enabled) "App Lock enabled with biometric / passcode security!" else "App Lock disabled."
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.isBiometricEnabled = enabled
        _uiState.update {
            it.copy(
                isBiometricEnabled = enabled,
                operationSuccessMessage = if (enabled) "Biometric / Fingerprint unlock enabled!" else "Biometric unlock disabled. PIN passcode will be required."
            )
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        securityManager.isAutoSyncEnabled = enabled
        _uiState.update {
            it.copy(
                isAutoSyncEnabled = enabled,
                operationSuccessMessage = if (enabled) "Continuous Auto-Sync active. All changes will be saved to your backup package." else "Auto-Sync disabled."
            )
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            val now = System.currentTimeMillis()
            securityManager.lastAutoSyncTime = now
            _uiState.update {
                it.copy(
                    isOperating = false,
                    lastAutoSyncTime = now,
                    operationSuccessMessage = "Data successfully auto-synced and backup bundle refreshed!"
                )
            }
        }
    }

    fun updateUserProfile(name: String, email: String, tagline: String, targetNetWorth: Double, colorIndex: Int, dateOfBirth: String = "") {
        viewModelScope.launch {
            profileRepository.updateProfile(name, email, tagline, targetNetWorth, colorIndex, dateOfBirth)
            _uiState.update { it.copy(operationSuccessMessage = "Profile updated successfully!") }
        }
    }

    fun saveProfileImage(uri: android.net.Uri) {
        viewModelScope.launch {
            val success = profileRepository.saveProfileImageFromUri(uri)
            if (success) {
                _uiState.update { it.copy(operationSuccessMessage = "Profile photo updated!") }
            }
        }
    }

    fun removeProfileImage() {
        viewModelScope.launch {
            profileRepository.removeProfileImage()
            _uiState.update { it.copy(operationSuccessMessage = "Profile photo removed. Showing logo avatar.") }
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            repository.restoreDefaultCategories()
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = "Successfully restored all 24+ standard default asset and expense categories!"
                )
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            DatabaseInitializer.seedSampleData(db)
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = "Successfully seeded 8 months of realistic portfolio, SIPs and expense demo data (Jan 2026 - Aug 2026)!"
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            DatabaseInitializer.clearAllUserData(db)
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = "All portfolio data, monthly valuations, expenses, and recurring flows cleared. Clean slate restored!"
                )
            }
        }
    }

    fun importJsonSnapshot(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            val result = com.example.util.SnapshotImporter.importSnapshotJson(repository, jsonString)
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = result.message
                )
            }
        }
    }

    suspend fun getFullBackupJson(): String {
        val currentProfile = _uiState.value.userProfile
        val fireSettings = fireSettingsRepo.fireSettings.value
        return com.example.util.CloudBackupManager.createFullBackupJson(
            repository = repository,
            userProfile = currentProfile,
            fireSettings = fireSettings
        )
    }

    fun restoreFullBackup(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            val result = com.example.util.CloudBackupManager.restoreFullBackupJson(
                repository = repository,
                jsonString = jsonString,
                profileRepository = profileRepository,
                fireSettingsRepository = fireSettingsRepo
            )
            refreshAutoBackupState()
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = result.message
                )
            }
        }
    }

    fun refreshAutoBackupState() {
        val app = getApplication<Application>()
        val state = com.example.util.AutoBackupManager.getState(app)
        val files = com.example.util.AutoBackupManager.getBackupFiles(app)
        _uiState.update { it.copy(autoBackupState = state, backupFiles = files) }
    }

    fun setDailyAutoBackupEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        com.example.util.AutoBackupManager.setAutoBackupEnabled(app, enabled)
        com.example.util.AutoBackupManager.setAutoBackupConfigured(app, true)
        refreshAutoBackupState()
        _uiState.update {
            it.copy(
                operationSuccessMessage = if (enabled) {
                    "Daily Auto-Backup enabled! Saved in portfolioBackup folder."
                } else {
                    "Daily Auto-Backup disabled."
                }
            )
        }
        if (enabled) {
            triggerAutoBackupNow()
        }
    }

    fun triggerAutoBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            val app = getApplication<Application>()
            val result = com.example.util.AutoBackupManager.checkAndPerformDailyBackup(
                context = app,
                repository = repository,
                profileRepository = profileRepository,
                fireSettingsRepository = fireSettingsRepo,
                force = true
            )
            refreshAutoBackupState()
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = result.message
                )
            }
        }
    }

    fun restoreFromBackupFile(file: java.io.File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            val app = getApplication<Application>()
            val result = com.example.util.AutoBackupManager.restoreFromFile(
                context = app,
                file = file,
                repository = repository,
                profileRepository = profileRepository,
                fireSettingsRepository = fireSettingsRepo
            )
            refreshAutoBackupState()
            _uiState.update {
                it.copy(
                    isOperating = false,
                    operationSuccessMessage = result.message
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(operationSuccessMessage = null) }
    }
}
