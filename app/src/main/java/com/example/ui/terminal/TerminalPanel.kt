package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.terminal.RootfsInstallState
import com.example.data.terminal.UbuntuRootfsManager
import com.example.data.terminal.WebSocketTerminalClient
import com.example.data.terminal.WsConnectionState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File
import kotlinx.coroutines.launch

enum class TerminalMode(val label: String) {
  LOCAL_SH("Local PTY (sh)"),
  PROOT_UBUNTU("Ubuntu proot"),
  WEBSOCKET("WebSocket"),
}

data class PtySessionTab(
  val id: Int,
  val title: String,
  val executable: String,
  val args: Array<String>,
  val initialCommand: String? = null,
)

class TerminalLaunchRequest(
  val title: String,
  val command: String,
  val useProotIfAvailable: Boolean = true,
)

@Composable
fun TerminalPanel(
  workspaceRoot: File,
  defaultMode: TerminalMode,
  defaultWebSocketUrl: String,
  windowsCommandAliases: Boolean,
  rootfsManager: UbuntuRootfsManager,
  wsClient: WebSocketTerminalClient,
  pendingLaunchRequest: TerminalLaunchRequest?,
  onLaunchRequestConsumed: () -> Unit,
  onSaveWebSocketUrl: (String) -> Unit,
  modifier: Modifier = Modifier,
  isContainerVisible: Boolean = true,
) {
  val palette = LocalIdePalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var activeMode by remember(defaultMode) { mutableStateOf(defaultMode) }
  val rootfsState by rootfsManager.state.collectAsStateWithLifecycle()
  val prootDiagnostics = remember(rootfsState) { rootfsManager.inspectDiagnostics() }

  val ptyTabs = remember { mutableStateListOf<PtySessionTab>() }
  var activePtyTabId by remember { mutableIntStateOf(-1) }
  var nextTabId by remember { mutableIntStateOf(1) }

  fun createLocalShTab(title: String = "sh #${nextTabId}", initialCommand: String? = null) {
    val tab =
      PtySessionTab(
        id = nextTabId++,
        title = title,
        executable = "/system/bin/sh",
        args = arrayOf("-l"),
        initialCommand = initialCommand,
      )
    ptyTabs += tab
    activePtyTabId = tab.id
    activeMode = TerminalMode.LOCAL_SH
  }

  fun createProotTab(title: String = "ubuntu #${nextTabId}", initialCommand: String? = null) {
    val (exe, args) = rootfsManager.buildProotShellArgs(workspaceRoot, initialCommand)
    val tab =
      PtySessionTab(
        id = nextTabId++,
        title = title,
        executable = exe,
        args = args,
        initialCommand = null,
      )
    ptyTabs += tab
    activePtyTabId = tab.id
    activeMode = TerminalMode.LOCAL_SH
  }

  LaunchedEffect(Unit) {
    if (ptyTabs.isEmpty()) {
      createLocalShTab("sh #1")
    }
  }

  LaunchedEffect(pendingLaunchRequest) {
    val req = pendingLaunchRequest ?: return@LaunchedEffect
    if (req.useProotIfAvailable && prootDiagnostics.isProotAvailable && rootfsManager.isRootfsInstalled()) {
      createProotTab(req.title, req.command)
    } else {
      createLocalShTab(req.title, req.command)
    }
    onLaunchRequestConsumed()
  }

  Column(modifier = modifier.fillMaxSize().background(palette.editorBg)) {
    // Mode Switcher Bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(palette.activityBarBg)
          .border(0.5.dp, palette.border)
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      TerminalMode.entries.forEach { mode ->
        val selected = activeMode == mode
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(if (selected) palette.accent else palette.inputBg)
              .clickable { activeMode = mode }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("terminal_mode_${mode.name.lowercase()}")
        ) {
          Text(
            mode.label,
            color = if (selected) Color.White else palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
      Spacer(Modifier.weight(1f))
      if (activeMode == TerminalMode.LOCAL_SH) {
        IconButton(
          onClick = { createLocalShTab() },
          modifier = Modifier.size(30.dp).testTag("terminal_new_tab_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = "New Shell Tab", tint = palette.textPrimary, modifier = Modifier.size(16.dp))
        }
      }
    }

    when (activeMode) {
      TerminalMode.LOCAL_SH -> {
        // PTY Session Tabs
        if (ptyTabs.isNotEmpty()) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(palette.sidebarBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ptyTabs.forEach { tab ->
              val selected = tab.id == activePtyTabId
              Row(
                modifier =
                  Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (selected) palette.tabActiveBg else palette.tabInactiveBg)
                    .border(0.5.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(4.dp))
                    .clickable { activePtyTabId = tab.id }
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = palette.addedFg, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(tab.title, color = palette.textPrimary, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                if (ptyTabs.size > 1) {
                  Spacer(Modifier.width(4.dp))
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Close ${tab.title}",
                    tint = palette.textMuted,
                    modifier =
                      Modifier.size(13.dp).clickable {
                        ptyTabs.remove(tab)
                        if (activePtyTabId == tab.id) {
                          activePtyTabId = ptyTabs.lastOrNull()?.id ?: -1
                        }
                      },
                  )
                }
              }
            }
          }
        }

        if (ptyTabs.isNotEmpty()) {
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ptyTabs.forEach { tab ->
              val isActive = tab.id == activePtyTabId
              androidx.compose.runtime.key(tab.id) {
                TermuxPtyView(
                  tab = tab,
                  workspaceRoot = workspaceRoot,
                  windowsCommandAliases = windowsCommandAliases,
                  context = context,
                  isVisible = isContainerVisible && isActive,
                  modifier = if (isActive) Modifier.fillMaxSize() else Modifier.size(0.dp),
                )
              }
            }
          }
        }
      }

      TerminalMode.PROOT_UBUNTU -> {
        ProotSetupPane(
          diagnostics = prootDiagnostics,
          rootfsState = rootfsState,
          onDownloadRootfs = { scope.launch { rootfsManager.downloadAndInstallUbuntuArm64() } },
          onDeleteRootfs = { rootfsManager.deleteRootfs() },
          onLaunchUbuntu = { createProotTab("ubuntu") },
          onSetupAiClis = {
            val cmd = rootfsManager.buildSetupAiClisCommand()
            createProotTab("setup-ai-clis", initialCommand = cmd)
          },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }

      TerminalMode.WEBSOCKET -> {
        WebSocketTerminalPane(
          defaultUrl = defaultWebSocketUrl,
          wsClient = wsClient,
          onSaveUrl = onSaveWebSocketUrl,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun TermuxPtyView(
  tab: PtySessionTab,
  workspaceRoot: File,
  windowsCommandAliases: Boolean,
  context: Context,
  isVisible: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  var ctrlDown by remember { mutableStateOf(false) }
  var altDown by remember { mutableStateOf(false) }
  var textSizePx by remember { mutableIntStateOf(28) }
  var sessionRef by remember(tab.id) { mutableStateOf<TerminalSession?>(null) }
  var viewRef by remember(tab.id) { mutableStateOf<TerminalView?>(null) }
  var sessionEndedStatus by remember(tab.id) { mutableStateOf<Int?>(null) }
  var initialCommandSent by remember(tab.id) { mutableStateOf(false) }

  DisposableEffect(tab.id) {
    onDispose {
      sessionRef?.finishIfRunning()
    }
  }

  LaunchedEffect(isVisible, viewRef) {
    val view = viewRef ?: return@LaunchedEffect
    if (isVisible) {
      view.post { view.updateSize() }
    }
  }

  Column(modifier = modifier) {
    if (sessionEndedStatus != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.sidebarBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          "Process exited (code $sessionEndedStatus)",
          color = palette.modifiedFg,
          fontSize = 12.sp,
          fontFamily = JetBrainsMonoFontFamily,
        )
      }
    }

    AndroidView(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      factory = { ctx ->
        val termView = TerminalView(ctx, null)
        // setTextSize MUST precede setTypeface so TerminalRenderer is non-null inside TerminalView
        termView.setTextSize(textSizePx)
        val monoTypeface = ResourcesCompat.getFont(ctx, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        termView.setTypeface(monoTypeface)
        termView.isFocusable = true
        termView.isFocusableInTouchMode = true

        val sessionClient =
          object : TerminalSessionClient {
            override fun onTextChanged(changedSession: TerminalSession) {
              termView.onScreenUpdated()
            }

            override fun onTitleChanged(changedSession: TerminalSession) {}

            override fun onSessionFinished(finishedSession: TerminalSession) {
              sessionEndedStatus = finishedSession.exitStatus
            }

            override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
              if (!text.isNullOrEmpty()) {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Terminal", text))
              }
            }

            override fun onPasteTextFromClipboard(session: TerminalSession?) {
              val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()
              if (!clip.isNullOrEmpty()) {
                session?.write(clip)
              }
            }

            override fun onBell(session: TerminalSession) {}

            override fun onColorsChanged(session: TerminalSession) {
              termView.onScreenUpdated()
            }

            override fun onTerminalCursorStateChange(state: Boolean) {}

            override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE

            override fun logError(tag: String?, message: String?) {}
            override fun logWarn(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
          }

        val viewClient =
          object : TerminalViewClient {
            override fun onScale(scale: Float): Float {
              val next = (textSizePx * scale).toInt().coerceIn(18, 56)
              textSizePx = next
              termView.setTextSize(next)
              return 1.0f
            }

            override fun onSingleTapUp(e: MotionEvent?) {
              termView.requestFocus()
              val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
              imm.showSoftInput(termView, InputMethodManager.SHOW_IMPLICIT)
            }

            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
            override fun shouldEnforceCharBasedInput(): Boolean = true
            override fun shouldUseCtrlSpaceWorkaround(): Boolean = true
            override fun isTerminalViewSelected(): Boolean = true
            override fun copyModeChanged(copyMode: Boolean) {}
            override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
            override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
            override fun onLongPress(event: MotionEvent?): Boolean = false
            override fun readControlKey(): Boolean = ctrlDown.also { if (it) ctrlDown = false }
            override fun readAltKey(): Boolean = altDown.also { if (it) altDown = false }
            override fun readShiftKey(): Boolean = false
            override fun readFnKey(): Boolean = false
            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
            override fun onEmulatorSet() {
              termView.setTerminalCursorBlinkerState(true, true)
              val cmd = tab.initialCommand
              if (!initialCommandSent && !cmd.isNullOrBlank()) {
                initialCommandSent = true
                termView.post {
                  sessionRef?.write("$cmd\n")
                }
              }
            }
            override fun logError(tag: String?, message: String?) {}
            override fun logWarn(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
          }

        termView.setTerminalViewClient(viewClient)
        val envList =
          mutableListOf(
            "HOME=${workspaceRoot.absolutePath}",
            "TERM=xterm-256color",
            "PATH=/system/bin:/system/xbin",
            "PWD=${workspaceRoot.absolutePath}",
          )
        if (windowsCommandAliases && tab.executable == "/system/bin/sh") {
          val rcFile = File(ctx.cacheDir, ".pocketcode_shrc")
          runCatching {
            rcFile.writeText(
              """
              alias cls='clear'
              alias dir='ls -la'
              alias type='cat'
              alias copy='cp'
              alias move='mv'
              alias del='rm'
              alias where='which'
              alias md='mkdir -p'
              """.trimIndent() + "\n",
              Charsets.UTF_8,
            )
            envList += "ENV=${rcFile.absolutePath}"
          }
        }
        val env = envList.toTypedArray()
        val session =
          TerminalSession(
            tab.executable,
            workspaceRoot.absolutePath,
            tab.args,
            env,
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            sessionClient,
          )
        sessionRef = session
        viewRef = termView
        termView.attachSession(session)
        if (isVisible) {
          termView.post { termView.updateSize() }
        }
        termView
      },
      update = { view ->
        viewRef = view
      },
    )

    // Terminal Extra Keys Toolbar
    val extraKeys =
      listOf(
        "ESC" to { sessionRef?.write("\u001B") },
        "TAB" to { sessionRef?.write("\t") },
        "CTRL" to { ctrlDown = !ctrlDown },
        "ALT" to { altDown = !altDown },
        "^C" to { sessionRef?.write("\u0003") },
        "^D" to { sessionRef?.write("\u0004") },
        "^L" to { sessionRef?.write("\u000C") },
        "↑" to { sessionRef?.write("\u001B[A") },
        "↓" to { sessionRef?.write("\u001B[B") },
        "←" to { sessionRef?.write("\u001B[D") },
        "→" to { sessionRef?.write("\u001B[C") },
        "|" to { sessionRef?.write("|") },
        "/" to { sessionRef?.write("/") },
        "~" to { sessionRef?.write("~") },
        "-" to { sessionRef?.write("-") },
      )

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(42.dp)
          .background(palette.sidebarBg)
          .border(0.5.dp, palette.border)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      extraKeys.forEach { (label, action) ->
        val active = (label == "CTRL" && ctrlDown) || (label == "ALT" && altDown)
        Box(
          modifier =
            Modifier.defaultMinSize(minWidth = 38.dp, minHeight = 32.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(if (active) palette.accent else palette.inputBg)
              .clickable { action() }
              .padding(horizontal = 9.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            label,
            color = if (active) Color.White else palette.textPrimary,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun ProotSetupPane(
  diagnostics: com.example.data.terminal.ProotDiagnostics,
  rootfsState: RootfsInstallState,
  onDownloadRootfs: () -> Unit,
  onDeleteRootfs: () -> Unit,
  onLaunchUbuntu: () -> Unit,
  onSetupAiClis: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "Ubuntu 24.04 (arm64) via proot",
      color = palette.textPrimary,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold,
    )

    // Honest Android 10+ W^X & native binary diagnostics card
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(palette.sidebarBg)
          .border(
            1.dp,
            if (diagnostics.isProotAvailable) palette.addedFg else palette.modifiedFg,
            RoundedCornerShape(6.dp),
          )
          .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.WarningAmber,
          contentDescription = null,
          tint = if (diagnostics.isProotAvailable) palette.addedFg else palette.modifiedFg,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          if (diagnostics.isProotAvailable) "proot Native Binary Detected"
          else "proot Native Binary Not Bundled (Android 10+ W^X Notice)",
          color = palette.textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Text(
        "Device ABIs: ${diagnostics.supportedAbis.joinToString(", ")}",
        color = palette.textPrimary,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
      )
      if (!diagnostics.isProotAvailable) {
        Text(
          "Android 10+ (API 29+) blocks execve() on binaries downloaded into writable app storage. " +
            "Running the downloaded Ubuntu rootfs requires NDK-compiled libproot.so + libtalloc.so packaged in jniLibs/arm64-v8a/ " +
            "(nativeLibraryDir: ${diagnostics.nativeLibraryDir}). You can still download & SHA-256 verify the official Ubuntu rootfs below, or use Mode A (Local sh) / Mode C (WebSocket to Termux ttyd).",
          color = palette.textMuted,
          fontSize = 12.sp,
        )
      }
    }

    when (rootfsState) {
      is RootfsInstallState.Idle -> {
        Button(
          onClick = onDownloadRootfs,
          colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
          modifier = Modifier.fillMaxWidth().testTag("proot_download_button"),
        ) {
          Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text("Download & Verify Ubuntu 24.04 arm64")
        }
      }
      is RootfsInstallState.Downloading -> {
        val dlMb = rootfsState.downloadedBytes / (1024f * 1024f)
        val totalMb = rootfsState.totalBytes / (1024f * 1024f)
        Text(
          if (rootfsState.totalBytes > 0)
            "Downloading ${rootfsState.fileName}: %.1f / %.1f MB".format(dlMb, totalMb)
          else "Downloading ${rootfsState.fileName}: %.1f MB".format(dlMb),
          color = palette.textPrimary,
          fontSize = 12.sp,
        )
        if (rootfsState.totalBytes > 0) {
          LinearProgressIndicator(
            progress = { (rootfsState.downloadedBytes.toFloat() / rootfsState.totalBytes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = palette.accent,
          )
        } else {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
        }
      }
      is RootfsInstallState.VerifyingChecksum -> {
        Text("Verifying SHA-256 checksum against Ubuntu SHA256SUMS...", color = palette.textPrimary, fontSize = 12.sp)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
      }
      is RootfsInstallState.Extracting -> {
        Text("Extracting Ubuntu rootfs (${rootfsState.entriesCount} files)...", color = palette.textPrimary, fontSize = 12.sp)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
      }
      is RootfsInstallState.Ready -> {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(palette.sidebarBg)
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Ubuntu 24.04 arm64 rootfs verified and ready.", color = palette.addedFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          if (rootfsState.verifiedSha256 != null) {
            Text(
              "SHA-256: ${rootfsState.verifiedSha256.take(24)}...",
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
            )
          }
          Button(
            onClick = onLaunchUbuntu,
            enabled = diagnostics.isProotAvailable,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            modifier = Modifier.fillMaxWidth().testTag("proot_launch_shell_button"),
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open Ubuntu Shell")
          }
          Button(
            onClick = onSetupAiClis,
            enabled = diagnostics.isProotAvailable,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            modifier = Modifier.fillMaxWidth().testTag("proot_setup_ai_clis_button"),
          ) {
            Text("Setup AI CLIs (Node + Claude Code + Gemini CLI)")
          }
          OutlinedButton(
            onClick = onDeleteRootfs,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Delete Rootfs")
          }
        }
      }
      is RootfsInstallState.Error -> {
        Text("Error: ${rootfsState.message}", color = palette.deletedFg, fontSize = 12.sp)
        Button(onClick = onDownloadRootfs, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Default.Refresh, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Retry Download")
        }
      }
    }
  }
}

@Composable
private fun WebSocketTerminalPane(
  defaultUrl: String,
  wsClient: WebSocketTerminalClient,
  onSaveUrl: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val connectionState by wsClient.connectionState.collectAsStateWithLifecycle()
  val output by wsClient.outputBuffer.collectAsStateWithLifecycle()
  var urlInput by remember(defaultUrl) { mutableStateOf(defaultUrl) }
  var commandInput by remember { mutableStateOf("") }
  val outputScroll = rememberScrollState()

  LaunchedEffect(output) {
    outputScroll.animateScrollTo(outputScroll.maxValue)
  }

  Column(modifier = modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(
        value = urlInput,
        onValueChange = { urlInput = it },
        label = { Text("ttyd / WebSocket URL", fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("ws_url_input"),
      )
      val isConnected = connectionState is WsConnectionState.Connected
      Button(
        onClick = {
          onSaveUrl(urlInput)
          if (isConnected) wsClient.disconnect() else wsClient.connect(urlInput)
        },
        colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) palette.deletedFg else palette.accent),
        modifier = Modifier.testTag("ws_connect_button"),
      ) {
        Text(if (isConnected) "Disconnect" else "Connect", fontSize = 12.sp)
      }
    }

    val statusText =
      when (val s = connectionState) {
        is WsConnectionState.Disconnected -> "Disconnected (e.g. run `ttyd -W bash` in Termux on localhost:7681)"
        is WsConnectionState.Connecting -> "Connecting..."
        is WsConnectionState.Connected -> "Connected: ${s.url}"
        is WsConnectionState.Error -> "Error: ${s.message}"
      }
    Text(
      statusText,
      color = if (connectionState is WsConnectionState.Error) palette.deletedFg else palette.textMuted,
      fontSize = 11.sp,
      fontFamily = JetBrainsMonoFontFamily,
    )

    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF121212))
          .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
          .padding(10.dp)
          .verticalScroll(outputScroll)
    ) {
      Text(
        text = output.ifEmpty { "WebSocket terminal output will appear here." },
        color = palette.textPrimary,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 12.sp,
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(
        value = commandInput,
        onValueChange = { commandInput = it },
        placeholder = { Text("Enter command...", fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("ws_command_input"),
      )
      Button(
        onClick = {
          wsClient.sendCommand("$commandInput\n")
          commandInput = ""
        },
        enabled = connectionState is WsConnectionState.Connected,
      ) {
        Text("Send")
      }
      OutlinedButton(onClick = { wsClient.sendCommand("\u0003") }, enabled = connectionState is WsConnectionState.Connected) {
        Text("^C")
      }
    }
  }
}
