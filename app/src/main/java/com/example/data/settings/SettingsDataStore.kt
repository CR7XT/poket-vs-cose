package com.example.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.ai.GeminiChatClient
import com.example.ui.terminal.TerminalMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pocketCodeDataStore by preferencesDataStore(name = "pocketcode_settings")

enum class IdeLayoutMode(val label: String, val shortLabel: String) {
  AUTO("Auto (Adaptive)", "Auto"),
  VERTICAL_VSCODE("Vertical VS Code (Keyboard & Mouse)", "Vertical"),
  HORIZONTAL_SPLIT("Horizontal Split (Desktop)", "Horizontal"),
}

data class PocketCodeSettings(
  val themeId: String = "dark_plus",
  val fontSizeSp: Float = 14f,
  val tabWidth: Int = 2,
  val wordWrap: Boolean = false,
  val defaultTerminalMode: TerminalMode = TerminalMode.LOCAL_SH,
  val webSocketUrl: String = "ws://localhost:7681/ws",
  val geminiModelId: String = GeminiChatClient.DEFAULT_MODEL,
  val formatterEnabled: Boolean = true,
  val snippetsEnabled: Boolean = true,
  val markdownPreviewEnabled: Boolean = true,
  val layoutMode: IdeLayoutMode = IdeLayoutMode.AUTO,
  val windowsCommandAliases: Boolean = true,
  val mountedPhoneFolderUri: String? = null,
  val mountedPhoneFolderName: String? = null,
)

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val THEME_ID = stringPreferencesKey("theme_id")
    val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
    val TAB_WIDTH = intPreferencesKey("tab_width")
    val WORD_WRAP = booleanPreferencesKey("word_wrap")
    val TERMINAL_MODE = stringPreferencesKey("terminal_mode")
    val WEBSOCKET_URL = stringPreferencesKey("websocket_url")
    val GEMINI_MODEL_ID = stringPreferencesKey("gemini_model_id")
    val FORMATTER_ENABLED = booleanPreferencesKey("formatter_enabled")
    val SNIPPETS_ENABLED = booleanPreferencesKey("snippets_enabled")
    val MARKDOWN_PREVIEW_ENABLED = booleanPreferencesKey("markdown_preview_enabled")
    val LAYOUT_MODE = stringPreferencesKey("layout_mode")
    val WINDOWS_ALIASES = booleanPreferencesKey("windows_command_aliases")
    val PHONE_FOLDER_URI = stringPreferencesKey("mounted_phone_folder_uri")
    val PHONE_FOLDER_NAME = stringPreferencesKey("mounted_phone_folder_name")
  }

  val settingsFlow: Flow<PocketCodeSettings> =
    context.pocketCodeDataStore.data.map { prefs ->
      val modeName = prefs[Keys.TERMINAL_MODE] ?: TerminalMode.LOCAL_SH.name
      val mode = runCatching { TerminalMode.valueOf(modeName) }.getOrDefault(TerminalMode.LOCAL_SH)
      val layoutName = prefs[Keys.LAYOUT_MODE] ?: IdeLayoutMode.AUTO.name
      val layout = runCatching { IdeLayoutMode.valueOf(layoutName) }.getOrDefault(IdeLayoutMode.AUTO)
      PocketCodeSettings(
        themeId = prefs[Keys.THEME_ID] ?: "dark_plus",
        fontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: 14f,
        tabWidth = prefs[Keys.TAB_WIDTH] ?: 2,
        wordWrap = prefs[Keys.WORD_WRAP] ?: false,
        defaultTerminalMode = mode,
        webSocketUrl = prefs[Keys.WEBSOCKET_URL] ?: "ws://localhost:7681/ws",
        geminiModelId = prefs[Keys.GEMINI_MODEL_ID] ?: GeminiChatClient.DEFAULT_MODEL,
        formatterEnabled = prefs[Keys.FORMATTER_ENABLED] ?: true,
        snippetsEnabled = prefs[Keys.SNIPPETS_ENABLED] ?: true,
        markdownPreviewEnabled = prefs[Keys.MARKDOWN_PREVIEW_ENABLED] ?: true,
        layoutMode = layout,
        windowsCommandAliases = prefs[Keys.WINDOWS_ALIASES] ?: true,
        mountedPhoneFolderUri = prefs[Keys.PHONE_FOLDER_URI],
        mountedPhoneFolderName = prefs[Keys.PHONE_FOLDER_NAME],
      )
    }

  suspend fun updateTheme(themeId: String) {
    context.pocketCodeDataStore.edit { it[Keys.THEME_ID] = themeId }
  }

  suspend fun updateFontSize(sp: Float) {
    context.pocketCodeDataStore.edit { it[Keys.FONT_SIZE_SP] = sp.coerceIn(10f, 28f) }
  }

  suspend fun updateTabWidth(width: Int) {
    context.pocketCodeDataStore.edit { it[Keys.TAB_WIDTH] = width }
  }

  suspend fun updateWordWrap(wrap: Boolean) {
    context.pocketCodeDataStore.edit { it[Keys.WORD_WRAP] = wrap }
  }

  suspend fun updateTerminalMode(mode: TerminalMode) {
    context.pocketCodeDataStore.edit { it[Keys.TERMINAL_MODE] = mode.name }
  }

  suspend fun updateWebSocketUrl(url: String) {
    context.pocketCodeDataStore.edit { it[Keys.WEBSOCKET_URL] = url }
  }

  suspend fun updateGeminiModelId(modelId: String) {
    context.pocketCodeDataStore.edit { it[Keys.GEMINI_MODEL_ID] = modelId }
  }

  suspend fun updateFormatterEnabled(enabled: Boolean) {
    context.pocketCodeDataStore.edit { it[Keys.FORMATTER_ENABLED] = enabled }
  }

  suspend fun updateSnippetsEnabled(enabled: Boolean) {
    context.pocketCodeDataStore.edit { it[Keys.SNIPPETS_ENABLED] = enabled }
  }

  suspend fun updateMarkdownPreviewEnabled(enabled: Boolean) {
    context.pocketCodeDataStore.edit { it[Keys.MARKDOWN_PREVIEW_ENABLED] = enabled }
  }

  suspend fun updateLayoutMode(mode: IdeLayoutMode) {
    context.pocketCodeDataStore.edit { it[Keys.LAYOUT_MODE] = mode.name }
  }

  suspend fun updateWindowsAliases(enabled: Boolean) {
    context.pocketCodeDataStore.edit { it[Keys.WINDOWS_ALIASES] = enabled }
  }

  suspend fun updateMountedPhoneFolder(uri: String?, name: String?) {
    context.pocketCodeDataStore.edit { prefs ->
      if (uri.isNullOrBlank()) {
        prefs.remove(Keys.PHONE_FOLDER_URI)
        prefs.remove(Keys.PHONE_FOLDER_NAME)
      } else {
        prefs[Keys.PHONE_FOLDER_URI] = uri
        prefs[Keys.PHONE_FOLDER_NAME] = name ?: "Phone Folder"
      }
    }
  }
}
