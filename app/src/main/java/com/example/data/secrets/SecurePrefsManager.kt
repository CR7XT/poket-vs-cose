package com.example.data.secrets

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePrefsManager(context: Context) {

  private val prefs: SharedPreferences by lazy {
    createEncryptedPrefs(context.applicationContext)
  }

  private fun createEncryptedPrefs(appContext: Context): SharedPreferences {
    return try {
      val masterKey =
        MasterKey.Builder(appContext)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()
      EncryptedSharedPreferences.create(
        appContext,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
    } catch (_: Exception) {
      runCatching { appContext.deleteSharedPreferences(PREFS_FILE) }
      runCatching {
        val masterKey =
          MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
          appContext,
          PREFS_FILE,
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
      }.getOrElse {
        appContext.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
      }
    }
  }

  fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API, "").orEmpty()

  fun setGeminiApiKey(value: String) {
    prefs.edit().putString(KEY_GEMINI_API, value.trim()).apply()
  }

  fun getGitToken(): String = prefs.getString(KEY_GIT_TOKEN, "").orEmpty()

  fun setGitToken(value: String) {
    prefs.edit().putString(KEY_GIT_TOKEN, value.trim()).apply()
  }

  fun clearAllSecrets() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_FILE = "pocketcode_secure_secrets"
    private const val KEY_GEMINI_API = "gemini_api_key"
    private const val KEY_GIT_TOKEN = "git_https_token"
  }
}
