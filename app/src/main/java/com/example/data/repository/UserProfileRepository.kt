package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class UserProfile(
    val name: String = "Prakash Choudhary",
    val email: String = "prakash.seervi9460@gmail.com",
    val tagline: String = "Wealth Builder",
    val profileImagePath: String? = null,
    val targetNetWorth: Double = 5000000.0,
    val colorIndex: Int = 0,
    val dateOfBirth: String = "1998-05-01" // YYYY-MM-DD (0105 for bank statement passwords)
) {
    val initials: String
        get() {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "W"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            }
        }

    val calculatedAge: Int?
        get() {
            if (dateOfBirth.isBlank()) return null
            return try {
                val parts = dateOfBirth.split("-")
                if (parts.size == 3) {
                    val birthYear = parts[0].toIntOrNull() ?: return null
                    val birthMonth = parts[1].toIntOrNull() ?: return null
                    val birthDay = parts[2].toIntOrNull() ?: return null

                    val today = java.util.Calendar.getInstance()
                    val currentYear = today.get(java.util.Calendar.YEAR)
                    val currentMonth = today.get(java.util.Calendar.MONTH) + 1
                    val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)

                    var age = currentYear - birthYear
                    if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                        age--
                    }
                    if (age in 1..120) age else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
}

class UserProfileRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val name = prefs.getString("profile_name", "Prakash Seervi") ?: "Prakash Seervi"
        val email = prefs.getString("profile_email", "prakash.seervi9460@gmail.com") ?: "prakash.seervi9460@gmail.com"
        val tagline = prefs.getString("profile_tagline", "Wealth & Portfolio Builder") ?: "Wealth & Portfolio Builder"
        val imagePath = prefs.getString("profile_image_path", null)
        val targetNetWorth = prefs.getFloat("profile_target_net_worth", 5000000f).toDouble()
        val colorIndex = prefs.getInt("profile_color_index", 0)
        val dateOfBirth = prefs.getString("profile_dob", "1998-05-15") ?: "1998-05-15"

        // Verify image file exists if path is set
        val validImagePath = if (imagePath != null && File(imagePath).exists()) {
            imagePath
        } else {
            null
        }

        return UserProfile(
            name = name,
            email = email,
            tagline = tagline,
            profileImagePath = validImagePath,
            targetNetWorth = targetNetWorth,
            colorIndex = colorIndex,
            dateOfBirth = dateOfBirth
        )
    }

    suspend fun updateProfile(
        name: String,
        email: String,
        tagline: String,
        targetNetWorth: Double,
        colorIndex: Int,
        dateOfBirth: String = ""
    ) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString("profile_name", name.trim().ifEmpty { "Prakash Seervi" })
            .putString("profile_email", email.trim())
            .putString("profile_tagline", tagline.trim())
            .putFloat("profile_target_net_worth", targetNetWorth.toFloat())
            .putInt("profile_color_index", colorIndex)
            .putString("profile_dob", dateOfBirth.trim())
            .apply()

        _userProfile.value = _userProfile.value.copy(
            name = name.trim().ifEmpty { "Prakash Seervi" },
            email = email.trim(),
            tagline = tagline.trim(),
            targetNetWorth = targetNetWorth,
            colorIndex = colorIndex,
            dateOfBirth = dateOfBirth.trim()
        )
    }

    suspend fun saveProfileImageFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val avatarFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                // Delete previous avatar file if exists
                _userProfile.value.profileImagePath?.let { oldPath ->
                    val oldFile = File(oldPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                FileOutputStream(avatarFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                val savedPath = avatarFile.absolutePath
                prefs.edit().putString("profile_image_path", savedPath).apply()
                _userProfile.value = _userProfile.value.copy(profileImagePath = savedPath)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun removeProfileImage() = withContext(Dispatchers.IO) {
        _userProfile.value.profileImagePath?.let { oldPath ->
            val oldFile = File(oldPath)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }
        prefs.edit().remove("profile_image_path").apply()
        _userProfile.value = _userProfile.value.copy(profileImagePath = null)
    }
}
