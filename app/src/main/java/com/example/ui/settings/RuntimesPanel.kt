package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

/** One-tap installable language / tool. [command] runs inside the Ubuntu proot terminal. */
data class RuntimeEntry(val name: String, val blurb: String, val command: String)

private const val APT = "export DEBIAN_FRONTEND=noninteractive; apt-get update && apt-get install -y"

val RuntimeCatalog: List<RuntimeEntry> =
  listOf(
    RuntimeEntry("Python 3", "python3, pip, venv", "$APT python3 python3-pip python3-venv"),
    RuntimeEntry("Node.js + npm", "JavaScript / TypeScript runtime", "$APT nodejs npm"),
    RuntimeEntry("C / C++", "gcc, g++, make, cmake, gdb, clang", "$APT build-essential cmake gdb clang"),
    RuntimeEntry("Go", "golang", "$APT golang-go"),
    RuntimeEntry(
      "Rust",
      "rustup + cargo",
      "$APT curl ca-certificates build-essential && curl -sSf https://sh.rustup.rs | sh -s -- -y",
    ),
    RuntimeEntry("Java (JDK)", "OpenJDK", "$APT default-jdk"),
    RuntimeEntry("Ruby", "ruby-full", "$APT ruby-full"),
    RuntimeEntry("PHP", "php-cli", "$APT php-cli"),
    RuntimeEntry("Lua", "lua 5.4", "$APT lua5.4"),
    RuntimeEntry("Git + SSH", "git, openssh-client", "$APT git openssh-client"),
    RuntimeEntry("Editors & tools", "vim, nano, tmux, htop, curl, wget, zip", "$APT vim nano tmux htop curl wget zip unzip"),
    RuntimeEntry(
      "code-server (experimental)",
      "Real VS Code in a browser; supports extensions",
      "$APT curl ca-certificates && curl -fsSL https://code-server.dev/install.sh | sh",
    ),
  )

private enum class CustomKind(val label: String) {
  APT("apt"),
  PIP("pip"),
  NPM("npm"),
  SHELL("shell"),
}

private fun buildCustomCommand(kind: CustomKind, input: String): String =
  when (kind) {
    CustomKind.APT -> "$APT $input"
    CustomKind.PIP -> "$APT python3-pip && pip3 install --break-system-packages $input"
    CustomKind.NPM -> "$APT nodejs npm && npm install -g $input"
    CustomKind.SHELL -> input
  }

fun isAllFilesAccessGranted(context: Context): Boolean =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
  } else {
    // API 26-29: classic runtime storage permission.
    context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
      android.content.pm.PackageManager.PERMISSION_GRANTED
  }

private fun openAllFilesSettings(context: Context) {
  val intents = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      add(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
          .setData(Uri.parse("package:${context.packageName}"))
      )
      add(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
    add(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))
    )
  }
  for (intent in intents) {
    val ok = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (ok) return
  }
}

@Composable
fun AllFilesAccessCard() {
  val palette = LocalIdePalette.current
  val context = LocalContext.current
  var granted by remember { mutableStateOf(isAllFilesAccessGranted(context)) }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) granted = isAllFilesAccessGranted(context)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  Text(
    text =
      if (granted) "All files access: GRANTED. Ubuntu sees your phone storage at /sdcard."
      else "All files access: not granted. Grant it to browse and edit any folder on your phone, and to use /sdcard inside Ubuntu.",
    color = if (granted) palette.addedFg else palette.textMuted,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
  )
  val legacyPermissionLauncher =
    androidx.activity.compose.rememberLauncherForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = isAllFilesAccessGranted(context) }
  Button(
    onClick = {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        openAllFilesSettings(context)
      } else {
        legacyPermissionLauncher.launch(
          arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
          )
        )
      }
    },
    modifier = Modifier.fillMaxWidth().testTag("grant_all_files_button"),
  ) {
    Text(if (granted) "Manage all-files access" else "Grant all-files access")
  }
}

@Composable
fun RuntimesAndPackagesCard(onInstall: (title: String, command: String) -> Unit) {
  val palette = LocalIdePalette.current
  var query by remember { mutableStateOf("") }
  var kind by remember { mutableStateOf(CustomKind.APT) }
  var custom by remember { mutableStateOf("") }

  Text(
    "Installs run inside Ubuntu (set it up first in Terminal > Ubuntu proot).",
    color = palette.textMuted,
    fontSize = 11.sp,
  )
  OutlinedTextField(
    value = query,
    onValueChange = { query = it },
    label = { Text("Search languages & tools") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth().testTag("runtime_search"),
  )
  val shown =
    RuntimeCatalog.filter {
      query.isBlank() || it.name.contains(query, true) || it.blurb.contains(query, true)
    }
  shown.forEach { entry ->
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(entry.name, color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(entry.blurb, color = palette.textMuted, fontSize = 10.sp)
      }
      OutlinedButton(
        onClick = { onInstall("install ${entry.name}", entry.command) },
        border = BorderStroke(0.5.dp, palette.accent),
      ) {
        Text("Install", fontSize = 11.sp)
      }
    }
  }
  if (shown.isEmpty()) {
    Text("No match. Use the custom install below.", color = palette.textMuted, fontSize = 11.sp)
  }

  Text("Install anything", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    CustomKind.entries.forEach { k ->
      FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label, fontSize = 11.sp) })
    }
  }
  OutlinedTextField(
    value = custom,
    onValueChange = { custom = it },
    label = {
      Text(
        when (kind) {
          CustomKind.SHELL -> "Any shell command"
          else -> "Package names (space separated)"
        }
      )
    },
    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp),
    modifier = Modifier.fillMaxWidth().testTag("custom_install_input"),
    keyboardOptions = KeyboardOptions.Default,
  )
  Button(
    onClick = {
      val text = custom.trim()
      if (text.isNotEmpty()) onInstall("${kind.label}: ${text.take(24)}", buildCustomCommand(kind, text))
    },
    enabled = custom.isNotBlank(),
    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).testTag("custom_install_button"),
  ) {
    Text("Run in Ubuntu")
  }
}
