package com.example.ui.workspace

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.terminal.hiddenButMeasured
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.settings.IdeLayoutMode
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.EditorTab
import com.example.ui.ai.AiAssistantPanel
import com.example.ui.editor.CommandPaletteDialog
import com.example.ui.editor.FindReplaceBar
import com.example.ui.editor.KeyboardAccessoryBar
import com.example.ui.editor.MarkdownPreviewPane
import com.example.ui.editor.PaletteCommand
import com.example.ui.editor.SnippetPickerDialog
import com.example.ui.editor.SoraCodeEditorView
import com.example.ui.editor.SoraEditorController
import com.example.ui.explorer.ExplorerSidebar
import com.example.ui.explorer.SearchSidebar
import com.example.ui.git.GitPanel
import com.example.ui.settings.BuiltInFeaturesPanel
import com.example.ui.settings.OpenSourceLicensesDialog
import com.example.ui.settings.SettingsPanel
import com.example.ui.tasks.AddTaskDialog
import com.example.ui.tasks.TasksPanel
import com.example.ui.terminal.TerminalPanel
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun PocketCodeShell(
  viewModel: WorkspaceViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val palette = LocalIdePalette.current
  val editorController = remember { SoraEditorController() }
  val snackbarHostState = remember { SnackbarHostState() }
  var commandInitialQuery by remember { mutableStateOf("") }
  var showQuickNewFileDialog by remember { mutableStateOf(false) }
  var showQuickAddTaskDialog by remember { mutableStateOf(false) }
  var showPortraitAiSheet by remember { mutableStateOf(false) }
  var showSnippetPicker by remember { mutableStateOf(false) }
  var showLicensesDialog by remember { mutableStateOf(false) }
  var hasOpenedTerminalOnce by remember { mutableStateOf(false) }

  val importFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      viewModel.importFiles(uris)
    }

  val importZipLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) viewModel.importZip(uri)
    }

  val exportZipLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
      if (uri != null) viewModel.exportZip(uri)
    }

  val mountPhoneFolderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) viewModel.mountPhoneFolder(uri)
    }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
      viewModel.fetchDeviceLocation()
    }

  LaunchedEffect(state.statusMessage, state.errorMessage) {
    val msg = state.errorMessage ?: state.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearNotifications()
    }
  }

  LaunchedEffect(
    state.isBottomTerminalOpenLandscape,
    state.pendingTerminalLaunch,
    state.portraitDestination,
  ) {
    if (
      state.isBottomTerminalOpenLandscape ||
        state.pendingTerminalLaunch != null ||
        state.portraitDestination == PortraitDestination.TERMINAL
    ) {
      hasOpenedTerminalOnce = true
    }
  }

  val paletteCommands =
    remember(
      state.activeTabPath,
      state.wordWrap,
      state.fontSizeSp,
      state.formatterEnabled,
      state.snippetsEnabled,
      state.markdownPreviewEnabled,
      state.layoutMode,
    ) {
      listOf(
        PaletteCommand("new_file", "File: New File", "Ctrl+N") { showQuickNewFileDialog = true },
        PaletteCommand("save", "File: Save Active File", "Ctrl+S") { viewModel.saveActiveFile() },
        PaletteCommand("save_all", "File: Save All Files", "") { viewModel.saveAllFiles() },
        PaletteCommand("open_phone_folder", "Workspace: Open Phone Folder (Files)", "") {
          mountPhoneFolderLauncher.launch(null)
        },
        PaletteCommand("tasks_run_default", "Tasks: Run Default Task", "Ctrl+Shift+B") {
          viewModel.runDefaultTask()
        },
        PaletteCommand("tasks_add", "Tasks: Add New Task", "") { showQuickAddTaskDialog = true },
        PaletteCommand("tasks_open_json", "Tasks: Configure tasks.json", "") {
          viewModel.openTasksJson()
        },
        PaletteCommand("find", "Edit: Find and Replace in File", "Ctrl+F") {
          viewModel.setFindBarVisible(true)
        },
        PaletteCommand("format_doc", "Format: Format Active Document", "") {
          viewModel.formatActiveFile(editorController)
        },
        PaletteCommand("insert_snippet", "Snippets: Insert Built-in Snippet", "") {
          if (state.snippetsEnabled) {
            showSnippetPicker = true
          }
        },
        PaletteCommand("toggle_md_preview", "Markdown: Toggle Live Preview", "") {
          viewModel.toggleMarkdownPreview()
        },
        PaletteCommand("find_files", "Search: Find in All Files", "Ctrl+Shift+F") {
          viewModel.selectSidebarPanel(SidebarPanel.SEARCH)
          viewModel.selectPortraitDestination(PortraitDestination.FILES)
        },
        PaletteCommand("undo", "Edit: Undo", "Ctrl+Z") { editorController.undo() },
        PaletteCommand("redo", "Edit: Redo", "Ctrl+Y") { editorController.redo() },
        PaletteCommand("word_wrap", "View: Toggle Word Wrap", "Alt+Z") {
          viewModel.toggleWordWrap()
        },
        PaletteCommand("layout_cycle", "View: Switch Vertical / Horizontal / Auto Layout", "") {
          viewModel.cycleLayoutMode()
        },
        PaletteCommand("layout_vertical", "View: Vertical VS Code Layout (Keyboard & Mouse)", "") {
          viewModel.setLayoutMode(IdeLayoutMode.VERTICAL_VSCODE)
        },
        PaletteCommand("layout_horizontal", "View: Horizontal Split Layout", "") {
          viewModel.setLayoutMode(IdeLayoutMode.HORIZONTAL_SPLIT)
        },
        PaletteCommand("zoom_in", "View: Increase Editor Font Size", "") {
          viewModel.setFontSize(state.fontSizeSp + 2f)
        },
        PaletteCommand("zoom_out", "View: Decrease Editor Font Size", "") {
          viewModel.setFontSize(state.fontSizeSp - 2f)
        },
        PaletteCommand("theme_dark_plus", "Theme: VS Code Dark+", "") {
          viewModel.setTheme("dark_plus")
        },
        PaletteCommand("theme_tokyo", "Theme: Tokyo Night", "") {
          viewModel.setTheme("tokyo_night")
        },
        PaletteCommand("theme_github", "Theme: GitHub Dark", "") {
          viewModel.setTheme("github_dark")
        },
        PaletteCommand("theme_light", "Theme: Light+", "") {
          viewModel.setTheme("light_plus")
        },
        PaletteCommand("import_files", "Workspace: Import Files from Phone", "") {
          importFilesLauncher.launch(arrayOf("*/*"))
        },
        PaletteCommand("import_zip", "Workspace: Import ZIP Archive", "") {
          importZipLauncher.launch(
            arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
          )
        },
        PaletteCommand("export_zip", "Workspace: Export Project as ZIP", "") {
          exportZipLauncher.launch("pocketcode-workspace.zip")
        },
        PaletteCommand("toggle_terminal", "View: Toggle Terminal", "Ctrl+`") {
          viewModel.selectSidebarPanel(SidebarPanel.TERMINAL)
          viewModel.selectPortraitDestination(PortraitDestination.TERMINAL)
        },
        PaletteCommand("read_location", "Device: Grant & Read Location", "") {
          locationPermissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_COARSE_LOCATION,
              Manifest.permission.ACCESS_FINE_LOCATION,
            )
          )
        },
        PaletteCommand("open_claude_code", "AI: Open Claude Code in Terminal", "") {
          viewModel.openClaudeCodeCli()
        },
        PaletteCommand("open_gemini_cli", "AI: Open Gemini CLI in Terminal", "") {
          viewModel.openGeminiCli()
        },
        PaletteCommand("open_ai_panel", "AI: Open AI Assistant Panel", "") {
          viewModel.selectSidebarPanel(SidebarPanel.AI)
          showPortraitAiSheet = true
        },
        PaletteCommand("open_settings", "Preferences: Open Settings & Device Access", "") {
          viewModel.selectSidebarPanel(SidebarPanel.SETTINGS)
          viewModel.selectPortraitDestination(PortraitDestination.SETTINGS)
        },
        PaletteCommand("open_licenses", "About: View Open-Source Licenses", "") {
          showLicensesDialog = true
        },
      )
    }

  val onRequestLocation: () -> Unit = {
    if (viewModel.phoneAccessManager.hasLocationPermission()) {
      viewModel.fetchDeviceLocation()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
        )
      )
    }
  }

  val shellModifier =
    Modifier.fillMaxSize()
      .background(palette.editorBg)
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
          event.key == Key.F1 -> {
            commandInitialQuery = ">"
            viewModel.setCommandPaletteOpen(true)
            true
          }
          event.isCtrlPressed && event.isShiftPressed && event.key == Key.P -> {
            commandInitialQuery = ">"
            viewModel.setCommandPaletteOpen(true)
            true
          }
          event.isCtrlPressed && event.isShiftPressed && event.key == Key.B -> {
            viewModel.runDefaultTask()
            true
          }
          event.isCtrlPressed && event.key == Key.P -> {
            commandInitialQuery = ""
            viewModel.setCommandPaletteOpen(true)
            true
          }
          event.isCtrlPressed && event.key == Key.S -> {
            viewModel.saveActiveFile()
            true
          }
          event.isCtrlPressed && event.key == Key.F -> {
            viewModel.setFindBarVisible(!state.isFindBarVisible)
            true
          }
          event.isCtrlPressed && event.key == Key.B -> {
            viewModel.selectSidebarPanel(state.activeSidebarPanel)
            true
          }
          event.isCtrlPressed && (event.key == Key.Grave || event.key == Key.J) -> {
            viewModel.selectSidebarPanel(SidebarPanel.TERMINAL)
            true
          }
          event.isCtrlPressed && event.key == Key.W -> {
            state.activeTabPath?.let { viewModel.closeTab(it) }
            true
          }
          event.isCtrlPressed && event.key == Key.N -> {
            showQuickNewFileDialog = true
            true
          }
          else -> false
        }
      }

  Scaffold(
    modifier = shellModifier,
    containerColor = palette.editorBg,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding()
    ) {
      val useDesktopVsCodeChrome =
        when (state.layoutMode) {
          IdeLayoutMode.AUTO -> maxWidth >= 600.dp
          IdeLayoutMode.VERTICAL_VSCODE -> true
          IdeLayoutMode.HORIZONTAL_SPLIT -> true
        }
      val isPortraitVerticalChrome = maxWidth < 600.dp

      // System Back: leave the current screen / close the bottom terminal instead of exiting the app.
      // (Esc is deliberately NOT mapped to "back": terminal apps like Claude Code and vim need it.)
      BackHandler(
        enabled =
          !useDesktopVsCodeChrome && state.portraitDestination != PortraitDestination.EDITOR
      ) {
        viewModel.selectPortraitDestination(PortraitDestination.EDITOR)
      }
      BackHandler(enabled = useDesktopVsCodeChrome && state.isBottomTerminalOpenLandscape) {
        viewModel.selectSidebarPanel(SidebarPanel.TERMINAL)
      }

      if (useDesktopVsCodeChrome) {
        val sidebarWidth = if (isPortraitVerticalChrome) 220.dp else 264.dp
        val terminalTargetHeight =
          if (!state.isBottomTerminalOpenLandscape) 0.dp
          else if (isPortraitVerticalChrome) 220.dp
          else 255.dp
        val animatedTerminalHeight by
          animateDpAsState(
            targetValue = terminalTargetHeight,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "split_terminal_height",
          )

        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
          TopCommandBar(
            activeFileName = state.activeTab?.name,
            layoutMode = state.layoutMode,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            canSave = state.activeTab?.isDirty == true,
            wordWrap = state.wordWrap,
            onOpenCommandPalette = { initial ->
              commandInitialQuery = initial
              viewModel.setCommandPaletteOpen(true)
            },
            onCycleLayout = { viewModel.cycleLayoutMode() },
            onUndo = { editorController.undo() },
            onRedo = { editorController.redo() },
            onToggleFind = { viewModel.setFindBarVisible(!state.isFindBarVisible) },
            onToggleWrap = { viewModel.toggleWordWrap() },
            onOpenAi = { viewModel.selectSidebarPanel(SidebarPanel.AI) },
            onOpenSettings = { viewModel.selectSidebarPanel(SidebarPanel.SETTINGS) },
            onSave = { viewModel.saveActiveFile() },
          )

          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ActivityBar(
              activePanel = state.activeSidebarPanel,
              isSidebarOpen = state.isSidebarOpenLandscape,
              isTerminalOpen = state.isBottomTerminalOpenLandscape,
              onSelectPanel = { viewModel.selectSidebarPanel(it) },
            )

            AnimatedVisibility(visible = state.isSidebarOpenLandscape) {
              Box(
                modifier =
                  Modifier.width(sidebarWidth)
                    .fillMaxHeight()
                    .border(0.5.dp, palette.border)
              ) {
                SidebarContent(
                  panel = state.activeSidebarPanel,
                  state = state,
                  viewModel = viewModel,
                  editorController = editorController,
                  onImportFiles = { importFilesLauncher.launch(arrayOf("*/*")) },
                  onImportZip = {
                    importZipLauncher.launch(
                      arrayOf(
                        "application/zip",
                        "application/x-zip-compressed",
                        "application/octet-stream",
                      )
                    )
                  },
                  onExportZip = { exportZipLauncher.launch("pocketcode-workspace.zip") },
                  onMountPhoneFolder = { mountPhoneFolderLauncher.launch(null) },
                  onRequestLocation = onRequestLocation,
                  onOpenSnippetsPicker = { showSnippetPicker = true },
                  onOpenLicenses = { showLicensesDialog = true },
                )
              }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight().imePadding()) {
              EditorWorkspacePane(
                state = state,
                editorController = editorController,
                viewModel = viewModel,
                onNewFile = { showQuickNewFileDialog = true },
                onMountPhoneFolder = { mountPhoneFolderLauncher.launch(null) },
                onAddTask = { showQuickAddTaskDialog = true },
                onOpenSnippetsPicker = { showSnippetPicker = true },
                modifier = Modifier.weight(1f).fillMaxWidth(),
              )
              // Mount TerminalPanel on first open and keep it mounted while collapsed so PTY sessions stay alive
              if (hasOpenedTerminalOnce || animatedTerminalHeight > 0.dp) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(animatedTerminalHeight)
                      .then(if (animatedTerminalHeight > 0.dp) Modifier.border(0.5.dp, palette.border) else Modifier)
                ) {
                  TerminalPanel(
                    workspaceRoot = viewModel.repository.workspaceRoot,
                    defaultMode = state.defaultTerminalMode,
                    defaultWebSocketUrl = state.webSocketUrl,
                    windowsCommandAliases = state.windowsCommandAliases,
                    rootfsManager = viewModel.rootfsManager,
                    wsClient = viewModel.wsTerminalClient,
                    pendingLaunchRequest = state.pendingTerminalLaunch,
                    onLaunchRequestConsumed = { viewModel.clearPendingTerminalLaunch() },
                    onSaveWebSocketUrl = { viewModel.updateWebSocketUrl(it) },
                    isContainerVisible = animatedTerminalHeight > 0.dp,
                    onClose = { viewModel.selectSidebarPanel(SidebarPanel.TERMINAL) },
                  )
                }
              }
            }
          }

          IdeStatusBar(
            branchName = state.branchName,
            tab = state.activeTab,
            fontSizeSp = state.fontSizeSp,
            wordWrap = state.wordWrap,
            layoutLabel = if (isPortraitVerticalChrome) "Vertical" else "Split",
            onCommandBarClick = {
              commandInitialQuery = ">"
              viewModel.setCommandPaletteOpen(true)
            },
          )
        }
      } else {
        var filesModeSearch by remember { mutableStateOf(false) }
        var terminalTall by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
          TopCommandBar(
            activeFileName = state.activeTab?.name,
            layoutMode = state.layoutMode,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            canSave = state.activeTab?.isDirty == true,
            wordWrap = state.wordWrap,
            onOpenCommandPalette = { initial ->
              commandInitialQuery = initial
              viewModel.setCommandPaletteOpen(true)
            },
            onCycleLayout = { viewModel.cycleLayoutMode() },
            onUndo = { editorController.undo() },
            onRedo = { editorController.redo() },
            onToggleFind = { viewModel.setFindBarVisible(!state.isFindBarVisible) },
            onToggleWrap = { viewModel.toggleWordWrap() },
            onOpenAi = { showPortraitAiSheet = true },
            onOpenSettings = { viewModel.selectPortraitDestination(PortraitDestination.SETTINGS) },
            onSave = { viewModel.saveActiveFile() },
          )

          Column(modifier = Modifier.weight(1f).fillMaxWidth().imePadding()) {
            val terminalShown = state.portraitDestination == PortraitDestination.TERMINAL
            // Terminal is a bottom panel (like VS Code): the editor stays visible above it.
            val contentDest =
              if (terminalShown) PortraitDestination.EDITOR else state.portraitDestination
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (contentDest) {
              PortraitDestination.FILES -> {
                Column(modifier = Modifier.fillMaxSize()) {
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(palette.activityBarBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    ModePill(
                      label = "Explorer",
                      selected = !filesModeSearch,
                      onClick = { filesModeSearch = false },
                      testTag = "portrait_explorer_pill",
                    )
                    ModePill(
                      label = "Search in Files",
                      selected = filesModeSearch,
                      onClick = { filesModeSearch = true },
                      testTag = "portrait_search_pill",
                    )
                  }
                  if (filesModeSearch) {
                    SearchSidebar(
                      query = state.searchQuery,
                      matchCase = state.searchMatchCase,
                      isSearching = state.isSearching,
                      results = state.searchResults,
                      onQueryChange = { viewModel.performGlobalSearch(it, state.searchMatchCase) },
                      onToggleMatchCase = {
                        viewModel.performGlobalSearch(state.searchQuery, !state.searchMatchCase)
                      },
                      onJumpToMatch = { path, line -> viewModel.openFile(path, line) },
                    )
                  } else {
                    ExplorerSidebar(
                      tree = state.tree,
                      allFolderPaths = state.allFolderPaths,
                      activeTabPath = state.activeTabPath,
                      mountedPhoneFolderName = state.mountedPhoneFolderName,
                      phoneTree = state.phoneTree,
                      onOpenFile = { viewModel.openFile(it) },
                      onCreateFile = { p, n -> viewModel.createFile(p, n) },
                      onCreateFolder = { p, n -> viewModel.createFolder(p, n) },
                      onRenameEntry = { p, n -> viewModel.renameEntry(p, n) },
                      onMoveEntry = { p, t -> viewModel.moveEntry(p, t) },
                      onDeleteEntry = { p -> viewModel.deleteEntry(p) },
                      onImportFiles = { importFilesLauncher.launch(arrayOf("*/*")) },
                      onImportZip = {
                        importZipLauncher.launch(
                          arrayOf(
                            "application/zip",
                            "application/x-zip-compressed",
                            "application/octet-stream",
                          )
                        )
                      },
                      onExportZip = { exportZipLauncher.launch("pocketcode-workspace.zip") },
                      onMountPhoneFolder = { mountPhoneFolderLauncher.launch(null) },
                      onUnmountPhoneFolder = { viewModel.unmountPhoneFolder() },
                      onRefreshPhoneFolder = { viewModel.refreshPhoneFolder() },
                      onOpenPhoneFile = { viewModel.openPhoneFile(it) },
                      onCreatePhoneEntry = { parentDocId, name, isDir ->
                        viewModel.createPhoneEntry(parentDocId, name, isDir)
                      },
                      onRenamePhoneEntry = { docId, newName ->
                        viewModel.renamePhoneEntry(docId, newName)
                      },
                      onDeletePhoneEntry = { docId -> viewModel.deletePhoneEntry(docId) },
                    )
                  }
                }
              }
              PortraitDestination.EDITOR -> {
                EditorWorkspacePane(
                  state = state,
                  editorController = editorController,
                  viewModel = viewModel,
                  onNewFile = { showQuickNewFileDialog = true },
                  onMountPhoneFolder = { mountPhoneFolderLauncher.launch(null) },
                  onAddTask = { showQuickAddTaskDialog = true },
                  onOpenSnippetsPicker = { showSnippetPicker = true },
                  modifier = Modifier.fillMaxSize(),
                )
              }
              PortraitDestination.TASKS -> {
                TasksPanel(
                  tasks = state.tasks,
                  activeFileRelativePath = state.activeTab?.relativePath,
                  onRunTask = { viewModel.runTask(it) },
                  onAddTask = { l, c, g, cwd, def -> viewModel.addTask(l, c, g, cwd, def) },
                  onDeleteTask = { viewModel.deleteTask(it) },
                  onOpenTasksJson = { viewModel.openTasksJson() },
                )
              }
              // Rendered by the keep-alive terminal host below, so switching screens never
              // tears down running PTY sessions (Claude Code, Gemini CLI, shells).
              PortraitDestination.TERMINAL -> Unit
              PortraitDestination.GIT -> {
                GitPanel(
                  snapshot = state.gitSnapshot,
                  isBusy = state.gitBusy,
                  errorBanner = state.gitError,
                  savedTokenPresent = state.hasGitToken,
                  diffTarget = state.diffTarget,
                  diffLines = state.diffLines,
                  onDismissError = { viewModel.dismissGitError() },
                  onRefresh = { viewModel.refreshGit() },
                  onInitRepo = { viewModel.initGitRepo() },
                  onCloneRepo = { url, token, save -> viewModel.cloneGitRepo(url, token, save) },
                  onStageFile = { viewModel.stageGitFile(it) },
                  onStageAll = { viewModel.stageAllGitFiles() },
                  onUnstageFile = { viewModel.unstageGitFile(it) },
                  onCommit = { msg, author, email -> viewModel.commitGit(msg, author, email) },
                  onOpenDiff = { viewModel.openGitDiff(it) },
                  onCloseDiff = { viewModel.closeGitDiff() },
                )
              }
              PortraitDestination.SETTINGS -> {
                SettingsPanel(
                  themeId = state.themeId,
                  layoutMode = state.layoutMode,
                  windowsCommandAliases = state.windowsCommandAliases,
                  fontSizeSp = state.fontSizeSp,
                  tabWidth = state.tabWidth,
                  wordWrap = state.wordWrap,
                  defaultTerminalMode = state.defaultTerminalMode,
                  webSocketUrl = state.webSocketUrl,
                  hasGeminiApiKey = state.hasGeminiApiKey,
                  geminiModelId = state.geminiModelId,
                  hasGitToken = state.hasGitToken,
                  formatterEnabled = state.formatterEnabled,
                  snippetsEnabled = state.snippetsEnabled,
                  markdownPreviewEnabled = state.markdownPreviewEnabled,
                  mountedPhoneFolderName = state.mountedPhoneFolderName,
                  deviceContext = state.deviceContext,
                  locationSnapshot = state.locationSnapshot,
                  isFetchingLocation = state.isFetchingLocation,
                  onSelectTheme = { viewModel.setTheme(it) },
                  onSelectLayoutMode = { viewModel.setLayoutMode(it) },
                  onToggleWindowsAliases = { viewModel.setWindowsCommandAliases(it) },
                  onFontSizeChange = { viewModel.setFontSize(it) },
                  onTabWidthChange = { viewModel.setTabWidth(it) },
                  onToggleWordWrap = { viewModel.toggleWordWrap() },
                  onSelectDefaultTerminalMode = { viewModel.setDefaultTerminalMode(it) },
                  onSaveWebSocketUrl = { viewModel.updateWebSocketUrl(it) },
                  onSaveGeminiApiKey = { viewModel.saveGeminiApiKey(it) },
                  onSaveGeminiModelId = { viewModel.updateGeminiModelId(it) },
                  onSaveGitToken = { viewModel.saveGitToken(it) },
                  onClearGitToken = { viewModel.clearGitToken() },
                  onToggleFormatter = { viewModel.setFormatterEnabled(it) },
                  onToggleSnippets = { viewModel.setSnippetsEnabled(it) },
                  onToggleMarkdownPreview = { viewModel.setMarkdownPreviewEnabled(it) },
                  onMountPhoneFolder = { mountPhoneFolderLauncher.launch(null) },
                  onUnmountPhoneFolder = { viewModel.unmountPhoneFolder() },
                  onRequestLocation = onRequestLocation,
                  onSaveLocationJson = { viewModel.saveLocationToWorkspace() },
                  onRefreshDeviceContext = { viewModel.refreshDeviceContext() },
                  onOpenLicenses = { showLicensesDialog = true },
                  onInstallInUbuntu = { t, c -> viewModel.launchTerminalCommand(t, c, requireUbuntu = true) },
                )
              }
            }

            }

            // Keep-alive terminal host: mounted on first use; when another screen is selected it
            // takes no layout space and is INVISIBLE, so running sessions survive navigation.
            if (hasOpenedTerminalOnce || terminalShown) {
              Box(
                modifier =
                  if (terminalShown) {
                    Modifier.weight(if (terminalTall) 3f else 1f).fillMaxWidth().clipToBounds()
                  } else {
                    Modifier.hiddenButMeasured()
                  }
              ) {
                TerminalPanel(
                  workspaceRoot = viewModel.repository.workspaceRoot,
                  defaultMode = state.defaultTerminalMode,
                  defaultWebSocketUrl = state.webSocketUrl,
                  windowsCommandAliases = state.windowsCommandAliases,
                  rootfsManager = viewModel.rootfsManager,
                  wsClient = viewModel.wsTerminalClient,
                  pendingLaunchRequest = state.pendingTerminalLaunch,
                  onLaunchRequestConsumed = { viewModel.clearPendingTerminalLaunch() },
                  onSaveWebSocketUrl = { viewModel.updateWebSocketUrl(it) },
                  isContainerVisible = terminalShown,
                  onClose = { viewModel.selectPortraitDestination(PortraitDestination.EDITOR) },
                  isTall = terminalTall,
                  onToggleSize = { terminalTall = !terminalTall },
                )
              }
            }
          }

          IdeStatusBar(
            branchName = state.branchName,
            tab = state.activeTab,
            fontSizeSp = state.fontSizeSp,
            wordWrap = state.wordWrap,
            layoutLabel = "Touch",
            onCommandBarClick = {
              commandInitialQuery = ">"
              viewModel.setCommandPaletteOpen(true)
            },
          )

          NavigationBar(
            containerColor = palette.activityBarBg,
            contentColor = palette.textPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding().height(64.dp),
          ) {
            val items =
              listOf(
                Triple(PortraitDestination.FILES, Icons.Default.Folder, "nav_files"),
                Triple(PortraitDestination.EDITOR, Icons.Default.Code, "nav_editor"),
                Triple(PortraitDestination.TASKS, Icons.Default.PlaylistPlay, "nav_tasks"),
                Triple(PortraitDestination.TERMINAL, Icons.Default.Terminal, "nav_terminal"),
                Triple(PortraitDestination.GIT, Icons.Default.AccountTree, "nav_git"),
              )
            items.forEach { (dest, icon, tag) ->
              NavigationBarItem(
                selected = state.portraitDestination == dest,
                onClick = { viewModel.selectPortraitDestination(dest) },
                icon = { Icon(icon, contentDescription = dest.label) },
                label = { Text(dest.label, fontSize = 10.sp) },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = palette.textPrimary,
                    indicatorColor = palette.accent,
                    unselectedIconColor = palette.textMuted,
                    unselectedTextColor = palette.textMuted,
                  ),
                modifier = Modifier.testTag(tag),
              )
            }
          }
        }
      }
    }
  }

  if (state.isCommandPaletteOpen) {
    CommandPaletteDialog(
      filePaths = state.allFilePaths,
      commands = paletteCommands,
      initialQuery = commandInitialQuery,
      onOpenFile = { viewModel.openFile(it) },
      onDismiss = { viewModel.setCommandPaletteOpen(false) },
    )
  }

  if (showQuickNewFileDialog) {
    var fileName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showQuickNewFileDialog = false },
      title = { Text("New Workspace File") },
      text = {
        OutlinedTextField(
          value = fileName,
          onValueChange = { fileName = it },
          label = { Text("File name (e.g. script.py)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        TextButton(
          enabled = fileName.trim().isNotEmpty(),
          onClick = {
            viewModel.createFile("", fileName.trim())
            showQuickNewFileDialog = false
          },
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        TextButton(onClick = { showQuickNewFileDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showQuickAddTaskDialog) {
    AddTaskDialog(
      activeFileRelativePath = state.activeTab?.relativePath,
      onConfirm = { label, command, group, cwd, isDefault ->
        viewModel.addTask(label, command, group, cwd, isDefault)
        showQuickAddTaskDialog = false
      },
      onDismiss = { showQuickAddTaskDialog = false },
    )
  }

  if (showPortraitAiSheet) {
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showPortraitAiSheet = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.sidebarBg)
            .border(1.dp, palette.border, RoundedCornerShape(6.dp))
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().background(palette.activityBarBg).padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("PocketCode AI Panel", color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          IconButton(onClick = { showPortraitAiSheet = false }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close AI Panel", tint = palette.textPrimary)
          }
        }
        AiAssistantPanel(
          activeTab = state.activeTab,
          hasGeminiApiKey = state.hasGeminiApiKey,
          messages = state.aiMessages,
          isLoading = state.aiBusy,
          errorMessage = state.aiError,
          onOpenClaudeCode = {
            showPortraitAiSheet = false
            viewModel.openClaudeCodeCli()
          },
          onOpenGeminiCli = {
            showPortraitAiSheet = false
            viewModel.openGeminiCli()
          },
          onOpenSettings = {
            showPortraitAiSheet = false
            viewModel.selectPortraitDestination(PortraitDestination.SETTINGS)
          },
          onRunCodeAction = { action -> viewModel.runAiCodeAction(action) },
          onSendMessage = { msg -> viewModel.sendAiChatMessage(msg) },
          onClearChat = { viewModel.clearAiChat() },
          onInsertCode = { code ->
            editorController.insertSymbol(code)
            showPortraitAiSheet = false
          },
        )
      }
    }
  }

  if (showSnippetPicker) {
    SnippetPickerDialog(
      onInsertSnippet = { snippet -> editorController.insertSymbol(snippet.body) },
      onDismiss = { showSnippetPicker = false },
    )
  }

  if (showLicensesDialog) {
    OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
  }
}

@Composable
private fun TopCommandBar(
  activeFileName: String?,
  layoutMode: IdeLayoutMode,
  canUndo: Boolean,
  canRedo: Boolean,
  canSave: Boolean,
  wordWrap: Boolean,
  onOpenCommandPalette: (String) -> Unit,
  onCycleLayout: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onToggleFind: () -> Unit,
  onToggleWrap: () -> Unit,
  onOpenAi: () -> Unit,
  onOpenSettings: () -> Unit,
  onSave: () -> Unit,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(46.dp)
        .background(palette.activityBarBg)
        .border(0.5.dp, palette.border)
        .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier =
        Modifier.weight(1f)
          .height(32.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(palette.inputBg)
          .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
          .clickable { onOpenCommandPalette("") }
          .padding(horizontal = 8.dp)
          .testTag("command_center_button"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Default.Search,
        contentDescription = "Command Palette",
        tint = palette.textMuted,
        modifier = Modifier.size(14.dp),
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = activeFileName?.let { "$it — Ctrl+P or >" } ?: "PocketCode — Ctrl+P or >",
        color = palette.textMuted,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    IconButton(
      onClick = onCycleLayout,
      modifier = Modifier.size(34.dp).testTag("toolbar_layout_button"),
    ) {
      Icon(
        imageVector =
          if (layoutMode == IdeLayoutMode.VERTICAL_VSCODE) Icons.Default.ViewAgenda
          else Icons.Default.Splitscreen,
        contentDescription = "Switch Vertical / Split Layout (${layoutMode.shortLabel})",
        tint = if (layoutMode != IdeLayoutMode.AUTO) palette.accent else palette.textPrimary,
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onUndo,
      enabled = canUndo,
      modifier = Modifier.size(32.dp).testTag("toolbar_undo_button"),
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Undo,
        contentDescription = "Undo",
        tint = if (canUndo) palette.textPrimary else palette.textMuted.copy(alpha = 0.45f),
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onRedo,
      enabled = canRedo,
      modifier = Modifier.size(32.dp).testTag("toolbar_redo_button"),
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Redo",
        tint = if (canRedo) palette.textPrimary else palette.textMuted.copy(alpha = 0.45f),
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onToggleFind,
      modifier = Modifier.size(32.dp).testTag("toolbar_find_button"),
    ) {
      Icon(
        Icons.Default.Search,
        contentDescription = "Find in file",
        tint = palette.textPrimary,
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onToggleWrap,
      modifier = Modifier.size(32.dp).testTag("toolbar_wrap_button"),
    ) {
      Icon(
        Icons.Default.WrapText,
        contentDescription = "Toggle Word Wrap",
        tint = if (wordWrap) palette.accent else palette.textMuted,
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onOpenAi,
      modifier = Modifier.size(32.dp).testTag("toolbar_ai_button"),
    ) {
      Icon(
        Icons.Default.AutoAwesome,
        contentDescription = "Open AI Panel",
        tint = palette.accent,
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onSave,
      enabled = canSave,
      modifier = Modifier.size(32.dp).testTag("toolbar_save_button"),
    ) {
      Icon(
        Icons.Default.Save,
        contentDescription = "Save File",
        tint = if (canSave) palette.accent else palette.textMuted.copy(alpha = 0.45f),
        modifier = Modifier.size(17.dp),
      )
    }

    IconButton(
      onClick = onOpenSettings,
      modifier = Modifier.size(32.dp).testTag("toolbar_settings_button"),
    ) {
      Icon(
        Icons.Default.Settings,
        contentDescription = "Open Settings",
        tint = palette.textPrimary,
        modifier = Modifier.size(17.dp),
      )
    }
  }
}

@Composable
private fun ActivityBar(
  activePanel: SidebarPanel,
  isSidebarOpen: Boolean,
  isTerminalOpen: Boolean,
  onSelectPanel: (SidebarPanel) -> Unit,
) {
  val palette = LocalIdePalette.current
  val items: List<Pair<SidebarPanel, ImageVector>> =
    listOf(
      SidebarPanel.EXPLORER to Icons.Default.Folder,
      SidebarPanel.SEARCH to Icons.Default.Search,
      SidebarPanel.GIT to Icons.Default.AccountTree,
      SidebarPanel.TASKS to Icons.Default.PlaylistPlay,
      SidebarPanel.TERMINAL to Icons.Default.Terminal,
      SidebarPanel.AI to Icons.Default.AutoAwesome,
      SidebarPanel.FEATURES to Icons.Default.Extension,
      SidebarPanel.SETTINGS to Icons.Default.Settings,
    )

  Column(
    modifier =
      Modifier.width(46.dp)
        .fillMaxHeight()
        .background(palette.activityBarBg)
        .border(0.5.dp, palette.border),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    items.forEach { (panel, icon) ->
      val selected =
        if (panel == SidebarPanel.TERMINAL) isTerminalOpen
        else isSidebarOpen && activePanel == panel
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(46.dp)
            .clickable { onSelectPanel(panel) }
            .testTag("activity_bar_${panel.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        if (selected) {
          Box(
            modifier =
              Modifier.align(Alignment.CenterStart)
                .width(2.dp)
                .height(26.dp)
                .background(palette.accent)
          )
        }
        Icon(
          imageVector = icon,
          contentDescription = panel.title,
          tint = if (selected) palette.textPrimary else palette.textMuted,
          modifier = Modifier.size(21.dp),
        )
      }
    }
  }
}

@Composable
private fun SidebarContent(
  panel: SidebarPanel,
  state: WorkspaceUiState,
  viewModel: WorkspaceViewModel,
  editorController: SoraEditorController,
  onImportFiles: () -> Unit,
  onImportZip: () -> Unit,
  onExportZip: () -> Unit,
  onMountPhoneFolder: () -> Unit,
  onRequestLocation: () -> Unit,
  onOpenSnippetsPicker: () -> Unit,
  onOpenLicenses: () -> Unit,
) {
  when (panel) {
    SidebarPanel.EXPLORER,
    SidebarPanel.TERMINAL -> {
      ExplorerSidebar(
        tree = state.tree,
        allFolderPaths = state.allFolderPaths,
        activeTabPath = state.activeTabPath,
        mountedPhoneFolderName = state.mountedPhoneFolderName,
        phoneTree = state.phoneTree,
        onOpenFile = { viewModel.openFile(it) },
        onCreateFile = { p, n -> viewModel.createFile(p, n) },
        onCreateFolder = { p, n -> viewModel.createFolder(p, n) },
        onRenameEntry = { p, n -> viewModel.renameEntry(p, n) },
        onMoveEntry = { p, t -> viewModel.moveEntry(p, t) },
        onDeleteEntry = { p -> viewModel.deleteEntry(p) },
        onImportFiles = onImportFiles,
        onImportZip = onImportZip,
        onExportZip = onExportZip,
        onMountPhoneFolder = onMountPhoneFolder,
        onUnmountPhoneFolder = { viewModel.unmountPhoneFolder() },
        onRefreshPhoneFolder = { viewModel.refreshPhoneFolder() },
        onOpenPhoneFile = { viewModel.openPhoneFile(it) },
        onCreatePhoneEntry = { parentDocId, name, isDir ->
          viewModel.createPhoneEntry(parentDocId, name, isDir)
        },
        onRenamePhoneEntry = { docId, newName -> viewModel.renamePhoneEntry(docId, newName) },
        onDeletePhoneEntry = { docId -> viewModel.deletePhoneEntry(docId) },
      )
    }
    SidebarPanel.SEARCH -> {
      SearchSidebar(
        query = state.searchQuery,
        matchCase = state.searchMatchCase,
        isSearching = state.isSearching,
        results = state.searchResults,
        onQueryChange = { viewModel.performGlobalSearch(it, state.searchMatchCase) },
        onToggleMatchCase = {
          viewModel.performGlobalSearch(state.searchQuery, !state.searchMatchCase)
        },
        onJumpToMatch = { path, line -> viewModel.openFile(path, line) },
      )
    }
    SidebarPanel.GIT ->
      GitPanel(
        snapshot = state.gitSnapshot,
        isBusy = state.gitBusy,
        errorBanner = state.gitError,
        savedTokenPresent = state.hasGitToken,
        diffTarget = state.diffTarget,
        diffLines = state.diffLines,
        onDismissError = { viewModel.dismissGitError() },
        onRefresh = { viewModel.refreshGit() },
        onInitRepo = { viewModel.initGitRepo() },
        onCloneRepo = { url, token, save -> viewModel.cloneGitRepo(url, token, save) },
        onStageFile = { viewModel.stageGitFile(it) },
        onStageAll = { viewModel.stageAllGitFiles() },
        onUnstageFile = { viewModel.unstageGitFile(it) },
        onCommit = { msg, author, email -> viewModel.commitGit(msg, author, email) },
        onOpenDiff = { viewModel.openGitDiff(it) },
        onCloseDiff = { viewModel.closeGitDiff() },
      )
    SidebarPanel.TASKS ->
      TasksPanel(
        tasks = state.tasks,
        activeFileRelativePath = state.activeTab?.relativePath,
        onRunTask = { viewModel.runTask(it) },
        onAddTask = { l, c, g, cwd, def -> viewModel.addTask(l, c, g, cwd, def) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onOpenTasksJson = { viewModel.openTasksJson() },
      )
    SidebarPanel.AI ->
      AiAssistantPanel(
        activeTab = state.activeTab,
        hasGeminiApiKey = state.hasGeminiApiKey,
        messages = state.aiMessages,
        isLoading = state.aiBusy,
        errorMessage = state.aiError,
        onOpenClaudeCode = { viewModel.openClaudeCodeCli() },
        onOpenGeminiCli = { viewModel.openGeminiCli() },
        onOpenSettings = { viewModel.selectSidebarPanel(SidebarPanel.SETTINGS) },
        onRunCodeAction = { action -> viewModel.runAiCodeAction(action) },
        onSendMessage = { msg -> viewModel.sendAiChatMessage(msg) },
        onClearChat = { viewModel.clearAiChat() },
        onInsertCode = { code -> editorController.insertSymbol(code) },
      )
    SidebarPanel.FEATURES ->
      BuiltInFeaturesPanel(
        activeTab = state.activeTab,
        formatterEnabled = state.formatterEnabled,
        snippetsEnabled = state.snippetsEnabled,
        markdownPreviewEnabled = state.markdownPreviewEnabled,
        isMarkdownPreviewActive = state.isMarkdownPreviewActive,
        onToggleFormatter = { viewModel.setFormatterEnabled(it) },
        onToggleSnippets = { viewModel.setSnippetsEnabled(it) },
        onToggleMarkdownPreview = { viewModel.setMarkdownPreviewEnabled(it) },
        onFormatActiveFile = { viewModel.formatActiveFile(editorController) },
        onOpenSnippetsPicker = onOpenSnippetsPicker,
        onToggleMarkdownPreviewPane = { viewModel.toggleMarkdownPreview() },
      )
    SidebarPanel.SETTINGS ->
      SettingsPanel(
        themeId = state.themeId,
        layoutMode = state.layoutMode,
        windowsCommandAliases = state.windowsCommandAliases,
        fontSizeSp = state.fontSizeSp,
        tabWidth = state.tabWidth,
        wordWrap = state.wordWrap,
        defaultTerminalMode = state.defaultTerminalMode,
        webSocketUrl = state.webSocketUrl,
        hasGeminiApiKey = state.hasGeminiApiKey,
        geminiModelId = state.geminiModelId,
        hasGitToken = state.hasGitToken,
        formatterEnabled = state.formatterEnabled,
        snippetsEnabled = state.snippetsEnabled,
        markdownPreviewEnabled = state.markdownPreviewEnabled,
        mountedPhoneFolderName = state.mountedPhoneFolderName,
        deviceContext = state.deviceContext,
        locationSnapshot = state.locationSnapshot,
        isFetchingLocation = state.isFetchingLocation,
        onSelectTheme = { viewModel.setTheme(it) },
        onSelectLayoutMode = { viewModel.setLayoutMode(it) },
        onToggleWindowsAliases = { viewModel.setWindowsCommandAliases(it) },
        onFontSizeChange = { viewModel.setFontSize(it) },
        onTabWidthChange = { viewModel.setTabWidth(it) },
        onToggleWordWrap = { viewModel.toggleWordWrap() },
        onSelectDefaultTerminalMode = { viewModel.setDefaultTerminalMode(it) },
        onSaveWebSocketUrl = { viewModel.updateWebSocketUrl(it) },
        onSaveGeminiApiKey = { viewModel.saveGeminiApiKey(it) },
        onSaveGeminiModelId = { viewModel.updateGeminiModelId(it) },
        onSaveGitToken = { viewModel.saveGitToken(it) },
        onClearGitToken = { viewModel.clearGitToken() },
        onToggleFormatter = { viewModel.setFormatterEnabled(it) },
        onToggleSnippets = { viewModel.setSnippetsEnabled(it) },
        onToggleMarkdownPreview = { viewModel.setMarkdownPreviewEnabled(it) },
        onMountPhoneFolder = onMountPhoneFolder,
        onUnmountPhoneFolder = { viewModel.unmountPhoneFolder() },
        onRequestLocation = onRequestLocation,
        onSaveLocationJson = { viewModel.saveLocationToWorkspace() },
        onRefreshDeviceContext = { viewModel.refreshDeviceContext() },
        onOpenLicenses = onOpenLicenses,
        onInstallInUbuntu = { t, c -> viewModel.launchTerminalCommand(t, c, requireUbuntu = true) },
      )
  }
}

@Composable
private fun EditorWorkspacePane(
  state: WorkspaceUiState,
  editorController: SoraEditorController,
  viewModel: WorkspaceViewModel,
  onNewFile: () -> Unit,
  onMountPhoneFolder: () -> Unit,
  onAddTask: () -> Unit,
  onOpenSnippetsPicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val activeTab = state.activeTab

  Column(modifier = modifier.background(palette.editorBg)) {
    if (state.tabs.isNotEmpty()) {
      EditorTabStrip(
        tabs = state.tabs,
        activeTabPath = state.activeTabPath,
        onSelectTab = { viewModel.selectTab(it) },
        onCloseTab = { viewModel.closeTab(it) },
      )
    }

    // VS Code Breadcrumbs + Quick Actions Bar
    if (activeTab != null) {
      val cleanPath = activeTab.relativePath.removePrefix("phone://")
      val rootLabel = if (activeTab.isPhoneFile) "phone" else "workspace"
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.sidebarBg)
            .border(0.5.dp, palette.border)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "$rootLabel › ${cleanPath.replace("/", " › ")}",
          color = palette.textMuted,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(4.dp))
                .background(palette.inputBg)
                .clickable { viewModel.runDefaultTask() }
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .testTag("editor_run_task_button"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.PlayArrow,
              contentDescription = "Run Default Task",
              tint = palette.addedFg,
              modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text("Run", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          }

          Icon(
            Icons.Default.AddTask,
            contentDescription = "Add VS Code Task",
            tint = palette.textPrimary,
            modifier =
              Modifier.clip(RoundedCornerShape(4.dp))
                .background(palette.inputBg)
                .clickable(onClick = onAddTask)
                .padding(4.dp)
                .size(14.dp)
                .testTag("editor_add_task_button"),
          )

          if (activeTab.language == CodeLanguage.MARKDOWN && state.markdownPreviewEnabled) {
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(if (state.isMarkdownPreviewActive) palette.accent else palette.inputBg)
                  .clickable { viewModel.toggleMarkdownPreview() }
                  .padding(horizontal = 7.dp, vertical = 3.dp)
                  .testTag("editor_markdown_preview_toggle"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Visibility,
                contentDescription = "Toggle Markdown Preview",
                tint = if (state.isMarkdownPreviewActive) Color.White else palette.textPrimary,
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(3.dp))
              Text(
                text = if (state.isMarkdownPreviewActive) "Code" else "Preview",
                color = if (state.isMarkdownPreviewActive) Color.White else palette.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
              )
            }
          }
          if (state.formatterEnabled && !state.isMarkdownPreviewActive) {
            Text(
              text = "Format",
              color = palette.accent,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(palette.inputBg)
                  .clickable { viewModel.formatActiveFile(editorController) }
                  .padding(horizontal = 7.dp, vertical = 3.dp)
                  .testTag("editor_format_button"),
            )
          }
          if (state.snippetsEnabled && !state.isMarkdownPreviewActive) {
            Text(
              text = "Snippets",
              color = palette.textPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(palette.inputBg)
                  .clickable(onClick = onOpenSnippetsPicker)
                  .padding(horizontal = 7.dp, vertical = 3.dp)
                  .testTag("editor_snippets_button"),
            )
          }
        }
      }
    }

    AnimatedVisibility(visible = state.isFindBarVisible && activeTab != null && !state.isMarkdownPreviewActive) {
      FindReplaceBar(
        findQuery = state.findQuery,
        replaceQuery = state.replaceQuery,
        matchCase = state.findMatchCase,
        useRegex = state.findUseRegex,
        onFindQueryChange = { q ->
          viewModel.updateFindOptions(findQuery = q)
          editorController.search(q, state.findMatchCase, state.findUseRegex)
        },
        onReplaceQueryChange = { r -> viewModel.updateFindOptions(replaceQuery = r) },
        onToggleMatchCase = {
          val next = !state.findMatchCase
          viewModel.updateFindOptions(matchCase = next)
          editorController.search(state.findQuery, next, state.findUseRegex)
        },
        onToggleRegex = {
          val next = !state.findUseRegex
          viewModel.updateFindOptions(useRegex = next)
          editorController.search(state.findQuery, state.findMatchCase, next)
        },
        onFindPrevious = { editorController.findPrevious() },
        onFindNext = { editorController.findNext() },
        onReplaceOne = { editorController.replaceCurrent(state.replaceQuery) },
        onReplaceAll = { editorController.replaceAll(state.replaceQuery) },
        onClose = {
          editorController.stopSearch()
          viewModel.setFindBarVisible(false)
        },
      )
    }

    if (activeTab == null) {
      VsCodeWelcomeView(
        onNewFile = onNewFile,
        onOpenPhoneFolder = onMountPhoneFolder,
        onAddTask = onAddTask,
        onOpenTerminal = {
          viewModel.selectSidebarPanel(SidebarPanel.TERMINAL)
          viewModel.selectPortraitDestination(PortraitDestination.TERMINAL)
        },
        onOpenGit = {
          viewModel.selectSidebarPanel(SidebarPanel.GIT)
          viewModel.selectPortraitDestination(PortraitDestination.GIT)
        },
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )
    } else if (
      activeTab.language == CodeLanguage.MARKDOWN &&
        state.markdownPreviewEnabled &&
        state.isMarkdownPreviewActive
    ) {
      MarkdownPreviewPane(
        markdown = activeTab.currentContent,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )
    } else {
      SoraCodeEditorView(
        tab = activeTab,
        themeId = state.themeId,
        fontSizeSp = state.fontSizeSp,
        tabWidth = state.tabWidth,
        wordWrap = state.wordWrap,
        pendingJumpLine = state.pendingJumpLine,
        controller = editorController,
        onTextChanged = { path, text -> viewModel.onEditorTextChanged(path, text) },
        onCursorChanged = { l, c, sel, undo, redo ->
          viewModel.onCursorStateChanged(l, c, sel, undo, redo)
        },
        onFontSizeChanged = { sp -> viewModel.setFontSize(sp) },
        onJumpConsumed = { viewModel.clearPendingJumpLine() },
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )

      KeyboardAccessoryBar(controller = editorController)
    }
  }
}

@Composable
private fun VsCodeWelcomeView(
  onNewFile: () -> Unit,
  onOpenPhoneFolder: () -> Unit,
  onAddTask: () -> Unit,
  onOpenTerminal: () -> Unit,
  onOpenGit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("PocketCode", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Text(
      "Mobile & Desktop VS Code Workspace — works vertically or horizontally with touch, keyboard, and mouse.",
      color = palette.textMuted,
      fontSize = 13.sp,
    )
    Spacer(Modifier.height(4.dp))
    Text("START", color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)

    WelcomeActionRow(Icons.Default.NoteAdd, "New Workspace File", "Ctrl+N", onNewFile)
    WelcomeActionRow(Icons.Default.FolderShared, "Open Phone Folder (Downloads / Documents)", "SAF", onOpenPhoneFolder)
    WelcomeActionRow(Icons.Default.AddTask, "Add VS Code Task (.vscode/tasks.json)", "Tasks", onAddTask)
    WelcomeActionRow(Icons.Default.Terminal, "Open Integrated Terminal", "Ctrl+`", onOpenTerminal)
    WelcomeActionRow(Icons.Default.CloudDownload, "Source Control / Clone Git Repo", "JGit", onOpenGit)
  }
}

@Composable
private fun WelcomeActionRow(
  icon: ImageVector,
  title: String,
  shortcut: String,
  onClick: () -> Unit,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .background(palette.sidebarBg)
        .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(10.dp))
      Text(title, color = palette.textPrimary, fontSize = 13.sp)
    }
    Text(
      shortcut,
      color = palette.textMuted,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 11.sp,
    )
  }
}

@Composable
private fun EditorTabStrip(
  tabs: List<EditorTab>,
  activeTabPath: String?,
  onSelectTab: (String) -> Unit,
  onCloseTab: (String) -> Unit,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(38.dp)
        .background(palette.sidebarBg)
        .horizontalScroll(rememberScrollState()),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    tabs.forEach { tab ->
      val isActive = tab.relativePath == activeTabPath
      Row(
        modifier =
          Modifier.fillMaxHeight()
            .background(if (isActive) palette.tabActiveBg else palette.tabInactiveBg)
            .border(0.5.dp, palette.border)
            .clickable { onSelectTab(tab.relativePath) }
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = tab.language.badge,
          color = tab.language.badgeColor,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = tab.name,
          color = if (isActive) palette.textPrimary else palette.textMuted,
          fontSize = 12.sp,
          maxLines = 1,
        )
        if (tab.isDirty) {
          Spacer(Modifier.width(6.dp))
          Box(
            modifier =
              Modifier.size(7.dp)
                .clip(CircleShape)
                .background(palette.accent)
          )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
          onClick = { onCloseTab(tab.relativePath) },
          modifier = Modifier.size(24.dp),
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "Close ${tab.name}",
            tint = palette.textMuted,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun IdeStatusBar(
  branchName: String,
  tab: EditorTab?,
  fontSizeSp: Float,
  wordWrap: Boolean,
  layoutLabel: String,
  onCommandBarClick: () -> Unit,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(24.dp)
        .background(palette.statusBarBg)
        .clickable(onClick = onCommandBarClick)
        .padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.AccountTree,
        contentDescription = null,
        tint = palette.statusBarFg,
        modifier = Modifier.size(12.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = branchName,
        color = palette.statusBarFg,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = layoutLabel,
        color = palette.statusBarFg.copy(alpha = 0.85f),
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (tab != null) {
        Text(
          text = "Ln ${tab.cursorLine}, Col ${tab.cursorColumn}",
          color = palette.statusBarFg,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
        )
        Text(
          text = "${fontSizeSp.toInt()}sp",
          color = palette.statusBarFg,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
        )
        if (wordWrap) {
          Text(
            text = "Wrap",
            color = palette.statusBarFg,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
          )
        }
        Text(
          text = tab.language.displayName,
          color = palette.statusBarFg,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
        )
      } else {
        Text(
          text = "PocketCode Ready",
          color = palette.statusBarFg,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
        )
      }
    }
  }
}

@Composable
private fun ModePill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val palette = LocalIdePalette.current
  Box(
    modifier =
      Modifier.testTag(testTag)
        .clip(RoundedCornerShape(4.dp))
        .background(if (selected) palette.accent else palette.inputBg)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else palette.textPrimary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}
