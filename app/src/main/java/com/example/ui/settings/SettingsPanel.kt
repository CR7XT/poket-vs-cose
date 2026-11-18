package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.DeviceContextSnapshot
import com.example.data.device.DeviceLocationSnapshot
import com.example.data.settings.IdeLayoutMode
import com.example.ui.terminal.TerminalMode
import com.example.ui.theme.AllIdePalettes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun SettingsPanel(
  themeId: String,
  layoutMode: IdeLayoutMode,
  windowsCommandAliases: Boolean,
  fontSizeSp: Float,
  tabWidth: Int,
  wordWrap: Boolean,
  defaultTerminalMode: TerminalMode,
  webSocketUrl: String,
  hasGeminiApiKey: Boolean,
  geminiModelId: String,
  hasGitToken: Boolean,
  formatterEnabled: Boolean,
  snippetsEnabled: Boolean,
  markdownPreviewEnabled: Boolean,
  mountedPhoneFolderName: String?,
  deviceContext: DeviceContextSnapshot,
  locationSnapshot: DeviceLocationSnapshot?,
  isFetchingLocation: Boolean,
  onSelectTheme: (String) -> Unit,
  onSelectLayoutMode: (IdeLayoutMode) -> Unit,
  onToggleWindowsAliases: (Boolean) -> Unit,
  onFontSizeChange: (Float) -> Unit,
  onTabWidthChange: (Int) -> Unit,
  onToggleWordWrap: () -> Unit,
  onSelectDefaultTerminalMode: (TerminalMode) -> Unit,
  onSaveWebSocketUrl: (String) -> Unit,
  onSaveGeminiApiKey: (String) -> Unit,
  onSaveGeminiModelId: (String) -> Unit,
  onSaveGitToken: (String) -> Unit,
  onClearGitToken: () -> Unit,
  onToggleFormatter: (Boolean) -> Unit,
  onToggleSnippets: (Boolean) -> Unit,
  onToggleMarkdownPreview: (Boolean) -> Unit,
  onMountPhoneFolder: () -> Unit,
  onUnmountPhoneFolder: () -> Unit,
  onRequestLocation: () -> Unit,
  onSaveLocationJson: () -> Unit,
  onRefreshDeviceContext: () -> Unit,
  onOpenLicenses: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  var wsInput by remember(webSocketUrl) { mutableStateOf(webSocketUrl) }
  var geminiKeyInput by remember { mutableStateOf("") }
  var geminiModelInput by remember(geminiModelId) { mutableStateOf(geminiModelId) }
  var gitTokenInput by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxSize().background(palette.sidebarBg),
  ) {
    Text(
      text = "SETTINGS & DEVICE ACCESS",
      color = palette.textMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
    HorizontalDivider(color = palette.border)

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1. Workspace Layout (Vertical / Horizontal / Auto) & Windows Mode
      SettingsSectionCard(icon = Icons.Default.DesktopWindows, title = "Workspace Layout & Keyboard/Mouse") {
        Text(
          "Choose Vertical VS Code to work upright with a hardware keyboard & mouse without forcing horizontal rotation.",
          color = palette.textMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          IdeLayoutMode.entries.forEach { mode ->
            val selected = mode == layoutMode
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (selected) palette.accent.copy(alpha = 0.2f) else palette.inputBg)
                  .border(
                    width = 0.5.dp,
                    color = if (selected) palette.accent else palette.border,
                    shape = RoundedCornerShape(4.dp),
                  )
                  .clickable { onSelectLayoutMode(mode) }
                  .padding(horizontal = 10.dp, vertical = 8.dp)
                  .testTag("layout_mode_${mode.name.lowercase()}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(mode.label, color = palette.textPrimary, fontSize = 12.sp)
              if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = palette.accent, modifier = Modifier.size(15.dp))
              }
            }
          }
        }
        SettingSwitchRow(
          label = "Windows Command Aliases (cls, dir, type, copy, move, del)",
          checked = windowsCommandAliases,
          onCheckedChange = onToggleWindowsAliases,
          tag = "settings_windows_aliases",
        )
        Text(
          "Hardware shortcuts: Ctrl+S Save, Ctrl+P Quick Open, Ctrl+Shift+P Commands, Ctrl+F Find, Ctrl+B Sidebar, Ctrl+` Terminal, Ctrl+Shift+B Run Task.",
          color = palette.textMuted,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
          lineHeight = 14.sp,
        )
      }

      // 2. Phone Access & Device Context (SAF Files, Location, Network/Hardware)
      SettingsSectionCard(icon = Icons.Default.FolderShared, title = "Phone Files, Location & Hardware") {
        Text(
          text =
            if (mountedPhoneFolderName != null) "Mounted Phone Folder: $mountedPhoneFolderName"
            else "No phone folder mounted (uses scoped Storage Access Framework)",
          color = if (mountedPhoneFolderName != null) palette.addedFg else palette.textMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onMountPhoneFolder,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            modifier = Modifier.weight(1f).testTag("settings_mount_phone_folder_btn"),
          ) {
            Text(
              if (mountedPhoneFolderName != null) "Switch Folder" else "Open Phone Folder",
              color = Color.White,
              fontSize = 12.sp,
            )
          }
          if (mountedPhoneFolderName != null) {
            OutlinedButton(onClick = onUnmountPhoneFolder, modifier = Modifier.weight(1f)) {
              Text("Unmount", fontSize = 12.sp)
            }
          }
        }

        HorizontalDivider(color = palette.border)

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            text =
              when {
                isFetchingLocation -> "Reading device coordinates..."
                locationSnapshot != null ->
                  "Lat %.5f, Lon %.5f (±%.0fm, %s)".format(
                    locationSnapshot.latitude,
                    locationSnapshot.longitude,
                    locationSnapshot.accuracyMeters,
                    locationSnapshot.provider,
                  )
                deviceContext.locationPermissionGranted -> "Location permission granted — tap to read coordinates"
                else -> "Foreground location permission not granted"
              },
            color = palette.textPrimary,
            fontSize = 11.sp,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onRequestLocation,
            enabled = !isFetchingLocation,
            modifier = Modifier.weight(1f).testTag("settings_read_location_btn"),
          ) {
            Text(
              if (deviceContext.locationPermissionGranted) "Read Location" else "Grant & Read Location",
              fontSize = 11.sp,
            )
          }
          OutlinedButton(
            onClick = onSaveLocationJson,
            enabled = locationSnapshot != null,
            modifier = Modifier.weight(1f).testTag("settings_save_location_json_btn"),
          ) {
            Text("Save location.json", fontSize = 11.sp)
          }
        }

        HorizontalDivider(color = palette.border)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "Network: ${deviceContext.networkSummary} • Free: ${deviceContext.freeStorageMb} MB",
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
            )
            Text(
              "Keyboard: ${if (deviceContext.hardwareKeyboardConnected) "Connected" else "On-Screen"} • ${deviceContext.pointerDeviceSummary}",
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
            )
          }
          TextButton(onClick = onRefreshDeviceContext) {
            Text("Refresh", fontSize = 11.sp)
          }
        }
      }

      // 3. Themes
      SettingsSectionCard(icon = Icons.Default.Palette, title = "Color Theme") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          AllIdePalettes.forEach { theme ->
            val selected = theme.id == themeId
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (selected) palette.accent.copy(alpha = 0.18f) else palette.inputBg.copy(alpha = 0.6f))
                  .border(
                    width = if (selected) 1.dp else 0.5.dp,
                    color = if (selected) palette.accent else palette.border,
                    shape = RoundedCornerShape(6.dp),
                  )
                  .clickable { onSelectTheme(theme.id) }
                  .padding(horizontal = 10.dp, vertical = 9.dp)
                  .testTag("theme_option_${theme.id}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier =
                    Modifier.size(16.dp)
                      .clip(CircleShape)
                      .background(theme.editorBg)
                      .border(2.dp, theme.accent, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                  text = theme.displayName,
                  color = palette.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
              }
              if (selected) {
                Icon(
                  Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = palette.accent,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }

      // 4. Editor Preferences
      SettingsSectionCard(icon = Icons.Default.Palette, title = "Editor") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Font Size", color = palette.textPrimary, fontSize = 12.sp)
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onFontSizeChange(fontSizeSp - 1f) },
              modifier = Modifier.size(36.dp).testTag("settings_font_dec"),
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease font size", tint = palette.textPrimary)
            }
            Text(
              "${fontSizeSp.toInt()} sp",
              color = palette.textPrimary,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 12.sp,
              modifier = Modifier.padding(horizontal = 6.dp),
            )
            IconButton(
              onClick = { onFontSizeChange(fontSizeSp + 1f) },
              modifier = Modifier.size(36.dp).testTag("settings_font_inc"),
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase font size", tint = palette.textPrimary)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Tab Width", color = palette.textPrimary, fontSize = 12.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 4).forEach { spaces ->
              val selected = tabWidth == spaces
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (selected) palette.accent else palette.inputBg)
                    .clickable { onTabWidthChange(spaces) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("settings_tab_width_$spaces")
              ) {
                Text(
                  "$spaces sp",
                  color = if (selected) Color.White else palette.textPrimary,
                  fontSize = 12.sp,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Word Wrap", color = palette.textPrimary, fontSize = 12.sp)
          Switch(checked = wordWrap, onCheckedChange = { onToggleWordWrap() })
        }
      }

      // 5. Built-in Feature Toggles
      SettingsSectionCard(icon = Icons.Default.Extension, title = "Built-in Feature Toggles") {
        Text(
          "Built-in PocketCode toggles — NOT VS Code Marketplace extensions.",
          color = palette.accent,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
        )
        SettingSwitchRow("Code Formatter", formatterEnabled, onToggleFormatter, "settings_toggle_formatter")
        SettingSwitchRow("Code Snippets", snippetsEnabled, onToggleSnippets, "settings_toggle_snippets")
        SettingSwitchRow(
          "Markdown Live Preview",
          markdownPreviewEnabled,
          onToggleMarkdownPreview,
          "settings_toggle_md_preview",
        )
      }

      // 6. Terminal Configuration
      SettingsSectionCard(icon = Icons.Default.Terminal, title = "Terminal") {
        Text("Default Terminal Mode", color = palette.textMuted, fontSize = 11.sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          TerminalMode.entries.forEach { mode ->
            val selected = mode == defaultTerminalMode
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (selected) palette.accent.copy(alpha = 0.2f) else palette.inputBg)
                  .border(
                    width = 0.5.dp,
                    color = if (selected) palette.accent else palette.border,
                    shape = RoundedCornerShape(4.dp),
                  )
                  .clickable { onSelectDefaultTerminalMode(mode) }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(mode.label, color = palette.textPrimary, fontSize = 12.sp)
              if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = palette.accent, modifier = Modifier.size(15.dp))
              }
            }
          }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
          value = wsInput,
          onValueChange = { wsInput = it },
          label = { Text("ttyd / WebSocket URL", fontSize = 11.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = { onSaveWebSocketUrl(wsInput.trim()) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Save WebSocket URL", fontSize = 12.sp)
        }
      }

      // 7. Encrypted API Keys & Tokens
      SettingsSectionCard(icon = Icons.Default.Key, title = "Encrypted Credentials (Android Keystore)") {
        Text(
          "Stored locally with AES-256 EncryptedSharedPreferences. Optional: not needed for editor, Git, terminal, or CLI AI tools.",
          color = palette.textMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp,
        )
        Text(
          text = if (hasGeminiApiKey) "Gemini API Key: Saved (Encrypted)" else "Gemini API Key: Not set",
          color = if (hasGeminiApiKey) palette.addedFg else palette.modifiedFg,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
          value = geminiKeyInput,
          onValueChange = { geminiKeyInput = it },
          label = { Text("Paste Gemini API Key", fontSize = 11.sp) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("settings_gemini_key_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              onSaveGeminiApiKey(geminiKeyInput)
              geminiKeyInput = ""
            },
            enabled = geminiKeyInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            modifier = Modifier.weight(1f).testTag("settings_save_gemini_key_btn"),
          ) {
            Text("Save Key", color = Color.White, fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = { onSaveGeminiApiKey("") },
            enabled = hasGeminiApiKey,
            modifier = Modifier.weight(1f).testTag("settings_clear_gemini_key_btn"),
          ) {
            Text("Clear Key", fontSize = 12.sp)
          }
        }

        OutlinedTextField(
          value = geminiModelInput,
          onValueChange = { geminiModelInput = it },
          label = { Text("Gemini Model ID", fontSize = 11.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = { onSaveGeminiModelId(geminiModelInput) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Save Model ID", fontSize = 12.sp)
        }

        HorizontalDivider(color = palette.border)
        Text(
          text = if (hasGitToken) "Git HTTPS Token: Saved (Encrypted)" else "Git HTTPS Token: Not set",
          color = if (hasGitToken) palette.addedFg else palette.textMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
          value = gitTokenInput,
          onValueChange = { gitTokenInput = it },
          label = { Text("Git Personal Access Token", fontSize = 11.sp) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              onSaveGitToken(gitTokenInput)
              gitTokenInput = ""
            },
            enabled = gitTokenInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            modifier = Modifier.weight(1f),
          ) {
            Text("Save Token", color = Color.White, fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = onClearGitToken,
            enabled = hasGitToken,
            modifier = Modifier.weight(1f),
          ) {
            Text("Clear Token", fontSize = 12.sp)
          }
        }
      }

      // 8. Privacy & Open-Source Licenses
      SettingsSectionCard(icon = Icons.Default.Gavel, title = "Privacy & Open-Source Licenses") {
        Text(
          "Zero analytics or telemetry. Network calls occur only when you clone a Git repo, download Ubuntu rootfs, connect to a WebSocket terminal, or send a Gemini AI prompt.",
          color = palette.textMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp,
        )
        Button(
          onClick = onOpenLicenses,
          colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
          modifier = Modifier.fillMaxWidth().testTag("open_licenses_button"),
        ) {
          Text("Open-Source Licenses", color = Color.White, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
  icon: ImageVector,
  title: String,
  content: @Composable () -> Unit,
) {
  val palette = LocalIdePalette.current
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(palette.inputBg.copy(alpha = 0.35f))
        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = title,
        color = palette.textPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
    content()
  }
}

@Composable
private fun SettingSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
  }
}

private data class OssLicenseEntry(
  val library: String,
  val license: String,
  val coordinate: String,
  val notes: String,
)

private val OssLicenses =
  listOf(
    OssLicenseEntry(
      library = "Rosemoe sora-editor",
      license = "LGPL-2.1",
      coordinate = "io.github.rosemoe:{editor,language-textmate,editor-lsp}:0.24.4",
      notes = "Code editor widget and TextMate grammar engine.",
    ),
    OssLicenseEntry(
      library = "Termux Terminal Libraries",
      license = "Apache-2.0",
      coordinate = "com.github.termux.termux-app:{terminal-view,terminal-emulator}:v0.118.0",
      notes = "VT100/xterm terminal view and PTY emulator. No GPLv3 Termux app code included.",
    ),
    OssLicenseEntry(
      library = "Eclipse JGit",
      license = "EDL-1.0 (BSD-3-Clause)",
      coordinate = "org.eclipse.jgit:org.eclipse.jgit:5.13.5",
      notes = "Pure Java Git implementation for init, clone, status, stage, commit, log, and diff.",
    ),
    OssLicenseEntry(
      library = "OkHttp",
      license = "Apache-2.0",
      coordinate = "com.squareup.okhttp3:okhttp:4.10.0",
      notes = "WebSocket terminal client, Ubuntu rootfs download, and Gemini REST calls.",
    ),
    OssLicenseEntry(
      library = "Apache Commons Compress",
      license = "Apache-2.0",
      coordinate = "org.apache.commons:commons-compress:1.27.1",
      notes = "Streaming tar.gz extraction with Zip-Slip / path-traversal checks.",
    ),
    OssLicenseEntry(
      library = "AndroidX Security Crypto & Jetpack DataStore",
      license = "Apache-2.0",
      coordinate = "androidx.security:security-crypto:1.1.0, androidx.datastore:datastore-preferences:1.1.7",
      notes = "Keystore-backed AES-256 EncryptedSharedPreferences and local settings persistence.",
    ),
    OssLicenseEntry(
      library = "SLF4J NOP",
      license = "MIT",
      coordinate = "org.slf4j:slf4j-nop:1.7.36",
      notes = "No-op logging facade binding required by Eclipse JGit.",
    ),
    OssLicenseEntry(
      library = "JetBrains Mono & Inter Fonts",
      license = "SIL OFL 1.1",
      coordinate = "Bundled in res/font/",
      notes = "Monospace code typography and UI sans-serif typography.",
    ),
  )

@Composable
fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
  val palette = LocalIdePalette.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Open-Source Licenses") },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).testTag("licenses_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(OssLicenses, key = { it.library }) { item ->
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(palette.inputBg.copy(alpha = 0.5f))
                .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                .padding(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                item.library,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
              )
              Text(
                item.license,
                color = palette.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(Modifier.height(3.dp))
            Text(
              item.coordinate,
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(item.notes, color = palette.textPrimary, fontSize = 11.sp, lineHeight = 15.sp)
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}
