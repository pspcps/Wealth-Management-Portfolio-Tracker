package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.FireSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoBackupState(
    val isEnabled: Boolean,
    val isConfigured: Boolean,
    val hasPermissions: Boolean,
    val lastBackupTimestamp: Long,
    val lastBackupDateStr: String,
    val lastBackupStatus: String,
    val primaryFolderPath: String,
    val backupFilesCount: Int,
    val latestFileName: String
)

data class BackupFileInfo(
    val file: File,
    val fileName: String,
    val folderPath: String,
    val formattedDate: String,
    val sizeFormatted: String,
    val timestamp: Long
)

data class BackupOperationResult(
    val success: Boolean,
    val message: String,
    val savedFile: File? = null,
    val totalFiles: Int = 0
)

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"
    private const val PREFS_NAME = "portfolio_auto_backup_prefs"
    private const val KEY_ENABLED = "key_auto_backup_enabled"
    private const val KEY_CONFIGURED = "key_auto_backup_configured"
    private const val KEY_LAST_TIMESTAMP = "key_last_backup_timestamp"
    private const val KEY_LAST_DATE_STR = "key_last_backup_date_str"
    private const val KEY_LAST_STATUS = "key_last_backup_status"
    private const val KEY_LAST_PATH = "key_last_backup_path"
    private const val CHANNEL_ID = "daily_portfolio_backup_channel"
    const val FOLDER_NAME = "portfolioBackup"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isAutoBackupConfigured(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CONFIGURED, false)
    }

    fun setAutoBackupConfigured(context: Context, configured: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONFIGURED, configured).apply()
    }

    fun getLastBackupTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_TIMESTAMP, 0L)
    }

    /**
     * Returns the list of system permissions required or recommended on this Android version.
     * Android 13+ (API 33+): POST_NOTIFICATIONS
     * Android <= 32: READ_EXTERNAL_STORAGE
     * Android <= 29: WRITE_EXTERNAL_STORAGE
     */
    fun getRequiredPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return list
    }

    /**
     * Returns true if all permissions for daily backup are granted.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = getRequiredPermissions()
        if (permissions.isEmpty()) return true
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Finds or creates the primary "portfolioBackup" directory on mobile storage.
     */
    fun getPrimaryBackupFolder(context: Context): File {
        // Priority 1: Public Documents/portfolioBackup if accessible
        try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(docs, FOLDER_NAME)
            if (!dir.exists()) dir.mkdirs()
            if (dir.exists() && dir.canWrite()) {
                return dir
            }
        } catch (e: Exception) {
            Log.w(TAG, "Documents/portfolioBackup not directly writable: ${e.message}")
        }

        // Priority 2: External App Storage (Android/data/.../files/portfolioBackup)
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val dir = File(extDir, FOLDER_NAME)
                if (!dir.exists()) dir.mkdirs()
                if (dir.exists() && dir.canWrite()) {
                    return dir
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ExternalFilesDir/portfolioBackup not accessible: ${e.message}")
        }

        // Priority 3: Internal Storage Fallback
        val internalDir = File(context.filesDir, FOLDER_NAME)
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    /**
     * Returns all potential backup directories to search for existing backup files.
     */
    fun getAllBackupFolders(context: Context): List<File> {
        val list = mutableListOf<File>()
        try {
            val docs = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME)
            if (docs.exists()) list.add(docs)
        } catch (_: Exception) {}

        try {
            val downloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
            if (downloads.exists()) list.add(downloads)
        } catch (_: Exception) {}

        try {
            val ext = context.getExternalFilesDir(null)?.let { File(it, FOLDER_NAME) }
            if (ext != null && ext.exists() && !list.contains(ext)) list.add(ext)
        } catch (_: Exception) {}

        val internalDir = File(context.filesDir, FOLDER_NAME)
        if (internalDir.exists() && !list.contains(internalDir)) list.add(internalDir)

        return list
    }

    /**
     * Saves JSON content to MediaStore in public Documents/portfolioBackup (Android 10+).
     */
    private fun saveToMediaStoreDocuments(
        context: Context,
        fileName: String,
        content: String
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            // Check if file already exists in MediaStore
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "%Documents/$FOLDER_NAME%")
            var targetUri: Uri? = null

            resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    targetUri = ContentUris.withAppendedId(collection, id)
                }
            }

            if (targetUri == null) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$FOLDER_NAME")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                targetUri = resolver.insert(collection, values)
            }

            if (targetUri != null) {
                resolver.openOutputStream(targetUri!!, "rwt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(targetUri!!, updateValues, null, null)
                Log.i(TAG, "Successfully wrote $fileName to MediaStore Documents/$FOLDER_NAME")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore write failed for $fileName: ${e.message}")
            false
        }
    }

    /**
     * Returns all backup JSON files sorted from newest to oldest.
     */
    fun getBackupFiles(context: Context): List<BackupFileInfo> {
        val folders = getAllBackupFolders(context)
        val fileMap = mutableMapOf<String, File>()

        // 1. Scan all local folders
        for (folder in folders) {
            val files = folder.listFiles { file -> file.isFile && file.name.endsWith(".json", ignoreCase = true) } ?: emptyArray()
            for (f in files) {
                val existing = fileMap[f.name]
                if (existing == null || f.lastModified() > existing.lastModified()) {
                    fileMap[f.name] = f
                }
            }
        }

        // 2. Scan MediaStore on Android 10+ for any backups in Documents/portfolioBackup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.SIZE
                )
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%Documents/$FOLDER_NAME%", "%.json")

                resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol)
                        if (!fileMap.containsKey(name)) {
                            val id = cursor.getLong(idCol)
                            val uri = ContentUris.withAppendedId(collection, id)
                            // Cache to external/internal files so we have a local File reference
                            val cacheDir = File(context.cacheDir, FOLDER_NAME).apply { if (!exists()) mkdirs() }
                            val tempFile = File(cacheDir, name)
                            try {
                                resolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (tempFile.exists() && tempFile.length() > 0) {
                                    fileMap[name] = tempFile
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scanning MediaStore files: ${e.message}")
            }
        }

        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

        return fileMap.values
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val size = file.length()
                val sizeFormatted = when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", size / 1024.0)
                    else -> String.format(Locale.US, "%.2f MB", size / (1024.0 * 1024.0))
                }
                BackupFileInfo(
                    file = file,
                    fileName = file.name,
                    folderPath = file.parentFile?.absolutePath ?: "Documents / $FOLDER_NAME",
                    formattedDate = dateFormat.format(Date(file.lastModified())),
                    sizeFormatted = sizeFormatted,
                    timestamp = file.lastModified()
                )
            }
    }

    /**
     * Checks if a daily backup is needed for today, and if so, runs it asynchronously.
     */
    suspend fun checkAndPerformDailyBackup(
        context: Context,
        repository: PortfolioRepository,
        profileRepository: UserProfileRepository? = null,
        fireSettingsRepository: FireSettingsRepository? = null,
        force: Boolean = false
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        val enabled = isAutoBackupEnabled(context)
        if (!enabled && !force) {
            return@withContext BackupOperationResult(
                success = false,
                message = "Daily Auto-Backup is currently disabled in Settings"
            )
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDateStr = getPrefs(context).getString(KEY_LAST_DATE_STR, "")

        if (!force && todayStr == lastDateStr) {
            return@withContext BackupOperationResult(
                success = true,
                message = "Today's daily backup is already up to date ($todayStr)"
            )
        }

        try {
            val profile = profileRepository?.userProfile?.value
            val fireRepo = fireSettingsRepository ?: FireSettingsRepository(context)
            val fireSettings = fireRepo.fireSettings.value
            val backupJson = CloudBackupManager.createFullBackupJson(repository, profile, fireSettings)
            val dailyFileName = "portfolio_backup_$todayStr.json"
            val latestFileName = "portfolio_backup_latest.json"

            var primarySavedFile: File? = null

            // 1. Attempt Public MediaStore Documents save (visible in phone's Files app under Documents/portfolioBackup)
            val mediaStoreSuccess = saveToMediaStoreDocuments(context, dailyFileName, backupJson)
            saveToMediaStoreDocuments(context, latestFileName, backupJson)

            // 2. Direct write to external app storage (guaranteed reliable on all Android versions)
            try {
                val extDir = context.getExternalFilesDir(null)?.let { File(it, FOLDER_NAME) }
                if (extDir != null) {
                    if (!extDir.exists()) extDir.mkdirs()
                    val dailyExtFile = File(extDir, dailyFileName)
                    FileOutputStream(dailyExtFile).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                    val latestExtFile = File(extDir, latestFileName)
                    FileOutputStream(latestExtFile).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                    primarySavedFile = dailyExtFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed writing to externalFilesDir: ${e.message}")
            }

            // 3. Direct write to public Documents/portfolioBackup if filesystem permissions allow
            try {
                val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME)
                if (!docsDir.exists()) docsDir.mkdirs()
                if (docsDir.exists() && docsDir.canWrite()) {
                    val publicFile = File(docsDir, dailyFileName)
                    FileOutputStream(publicFile).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                    val latestPublicFile = File(docsDir, latestFileName)
                    FileOutputStream(latestPublicFile).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                    primarySavedFile = publicFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct public Documents write skipped: ${e.message}")
            }

            // 4. Mirror to internal private files directory for bulletproof safety
            try {
                val internalFolder = File(context.filesDir, FOLDER_NAME)
                if (!internalFolder.exists()) internalFolder.mkdirs()
                val mirrorDaily = File(internalFolder, dailyFileName)
                FileOutputStream(mirrorDaily).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                val mirrorLatest = File(internalFolder, latestFileName)
                FileOutputStream(mirrorLatest).use { it.write(backupJson.toByteArray(Charsets.UTF_8)) }
                if (primarySavedFile == null) {
                    primarySavedFile = mirrorDaily
                }
            } catch (e: Exception) {
                Log.w(TAG, "Internal files write failed: ${e.message}")
            }

            if (primarySavedFile == null && !mediaStoreSuccess) {
                throw IllegalStateException("Failed to write backup to any mobile storage location")
            }

            val now = System.currentTimeMillis()
            val status = "Success: Backed up to $FOLDER_NAME ($todayStr)"
            val savedPath = primarySavedFile?.absolutePath ?: "Documents / $FOLDER_NAME / $dailyFileName"

            getPrefs(context).edit()
                .putLong(KEY_LAST_TIMESTAMP, now)
                .putString(KEY_LAST_DATE_STR, todayStr)
                .putString(KEY_LAST_STATUS, status)
                .putString(KEY_LAST_PATH, savedPath)
                .apply()

            val allFiles = getBackupFiles(context)

            // Post notification if permission is available
            showBackupNotification(context, dailyFileName)

            Log.i(TAG, "Daily backup completed successfully: $savedPath")

            BackupOperationResult(
                success = true,
                message = "Daily backup saved to Documents / $FOLDER_NAME ($dailyFileName)",
                savedFile = primarySavedFile,
                totalFiles = allFiles.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Daily backup failed", e)
            val errorMsg = "Backup error: ${e.localizedMessage ?: "Unknown error"}"
            getPrefs(context).edit().putString(KEY_LAST_STATUS, errorMsg).apply()
            BackupOperationResult(
                success = false,
                message = errorMsg
            )
        }
    }

    /**
     * Posts a system notification when daily backup completes.
     */
    private fun showBackupNotification(context: Context, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Portfolio Backup",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for automatic daily portfolio backups"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Daily Portfolio Backup")
                .setContentText("Saved $fileName to Documents / $FOLDER_NAME")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post backup notification: ${e.message}")
        }
    }

    /**
     * Restores portfolio data from a given backup file.
     */
    suspend fun restoreFromFile(
        context: Context,
        file: File,
        repository: PortfolioRepository,
        profileRepository: UserProfileRepository? = null,
        fireSettingsRepository: FireSettingsRepository? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext ImportResult(success = false, message = "Backup file not found: ${file.name}")
        }
        val jsonString = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            return@withContext ImportResult(success = false, message = "Could not read backup file: ${e.message}")
        }
        val fireRepo = fireSettingsRepository ?: FireSettingsRepository(context)
        CloudBackupManager.restoreFullBackupJson(repository, jsonString, profileRepository, fireRepo)
    }

    /**
     * Share a backup file with external apps / Google Drive.
     */
    fun shareBackupFile(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Wealth Portfolio Backup - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Portfolio Backup JSON").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share backup file", e)
        }
    }

    fun getState(context: Context): AutoBackupState {
        val prefs = getPrefs(context)
        val files = getBackupFiles(context)
        val primaryFolder = getPrimaryBackupFolder(context)

        return AutoBackupState(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            isConfigured = prefs.getBoolean(KEY_CONFIGURED, false),
            hasPermissions = hasRequiredPermissions(context),
            lastBackupTimestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L),
            lastBackupDateStr = prefs.getString(KEY_LAST_DATE_STR, "") ?: "",
            lastBackupStatus = prefs.getString(KEY_LAST_STATUS, "No backup performed yet") ?: "",
            primaryFolderPath = primaryFolder.absolutePath,
            backupFilesCount = files.size,
            latestFileName = files.firstOrNull()?.fileName ?: "None"
        )
    }
}
