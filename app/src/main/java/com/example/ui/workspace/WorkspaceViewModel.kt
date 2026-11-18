package com.example.ui.workspace

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiChatMessage
import com.example.data.ai.GeminiChatClient
import com.example.data.device.DeviceContextSnapshot
import com.example.data.device.DeviceLocationSnapshot
import com.example.data.device.PhoneAccessManager
import com.example.data.git.GitDiffLine
import com.example.data.git.GitFileChange
import com.example.data.git.GitRepository
import com.example.data.git.GitStatusSnapshot
import com.example.data.secrets.SecurePrefsManager
import com.example.data.settings.IdeLayoutMode
import com.example.data.settings.SettingsDataStore
import com.example.data.tasks.IdeTask
import com.example.data.tasks.TaskGroup
import com.example.data.tasks.TaskRepository
import com.example.data.terminal.UbuntuRootfsManager
import com.example.data.terminal.WebSocketTerminalClient
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.EditorTab
import com.example.data.workspace.FileNode
import com.example.data.workspace.SearchFileResult
import com.example.data.workspace.WorkspaceRepository
import com.example.ui.editor.CodeFormatter
import com.example.ui.editor.SoraEditorController
import com.example.ui.terminal.TerminalLaunchRequest
import com.example.ui.terminal.TerminalMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SidebarPanel(val title: String) {
  EXPLORER("Explorer"),
  SEARCH("Search"),
  GIT("Source Control"),
  TASKS("Tasks"),
  TERMINAL("Terminal"),
  AI("AI Assistant"),
  FEATURES("Built-in Features"),
  SETTINGS("Settings"),
}

enum class PortraitDestination(val label: String) {
  FILES("Files"),
  EDITOR("Editor"),
  TASKS("Tasks"),
  TERMINAL("Terminal"),
  GIT("Git"),
  SETTINGS("Settings"),
}

data class WorkspaceUiState(
  val isLoading: Boolean = true,
  val tree: List<FileNode> = emptyList(),
  val allFilePaths: List<String> = emptyList(),
  val allFolderPaths: List<String> = listOf(""),
  val tabs: List<EditorTab> = emptyList(),
  val activeTabPath: String? = null,
  val themeId: String = "dark_plus",
  val layoutMode: IdeLayoutMode = IdeLayoutMode.AUTO,
  val windowsCommandAliases: Boolean = true,
  val fontSizeSp: Float = 14f,
  val tabWidth: Int = 2,
  val wordWrap: Boolean = false,
  val formatterEnabled: Boolean = true,
  val snippetsEnabled: Boolean = true,
  val markdownPreviewEnabled: Boolean = true,
  val isMarkdownPreviewActive: Boolean = false,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val isFindBarVisible: Boolean = false,
  val findQuery: String = "",
  val replaceQuery: String = "",
  val findMatchCase: Boolean = false,
  val findUseRegex: Boolean = false,
  val isCommandPaletteOpen: Boolean = false,
  val activeSidebarPanel: SidebarPanel = SidebarPanel.EXPLORER,
  val portraitDestination: PortraitDestination = PortraitDestination.EDITOR,
  val isSidebarOpenLandscape: Boolean = true,
  val searchQuery: String = "",
  val searchMatchCase: Boolean = false,
  val searchResults: List<SearchFileResult> = emptyList(),
  val isSearching: Boolean = false,
  val pendingJumpLine: Int? = null,
  val branchName: String = "no-git",
  val gitSnapshot: GitStatusSnapshot = GitStatusSnapshot(),
  val gitBusy: Boolean = false,
  val gitError: String? = null,
  val diffTarget: GitFileChange? = null,
  val diffLines: List<GitDiffLine> = emptyList(),
  val tasks: List<IdeTask> = emptyList(),
  val mountedPhoneFolderUri: String? = null,
  val mountedPhoneFolderName: String? = null,
  val phoneTree: List<FileNode> = emptyList(),
  val deviceContext: DeviceContextSnapshot =
    DeviceContextSnapshot(
      networkSummary = "Checking...",
      freeStorageMb = 0L,
      hardwareKeyboardConnected = false,
      pointerDeviceSummary = "Touch",
      locationPermissionGranted = false,
    ),
  val locationSnapshot: DeviceLocationSnapshot? = null,
  val isFetchingLocation: Boolean = false,
  val defaultTerminalMode: TerminalMode = TerminalMode.LOCAL_SH,
  val webSocketUrl: String = "ws://localhost:7681/ws",
  val isBottomTerminalOpenLandscape: Boolean = false,
  val pendingTerminalLaunch: TerminalLaunchRequest? = null,
  val hasGeminiApiKey: Boolean = false,
  val hasGitToken: Boolean = false,
  val geminiModelId: String = GeminiChatClient.DEFAULT_MODEL,
  val aiMessages: List<AiChatMessage> = emptyList(),
  val aiBusy: Boolean = false,
  val aiError: String? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
) {
  val activeTab: EditorTab?
    get() = tabs.firstOrNull { it.relativePath == activeTabPath }
}

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

  val repository = WorkspaceRepository(application)
  val gitRepository = GitRepository(repository.workspaceRoot)
  val taskRepository = TaskRepository(repository.workspaceRoot)
  val phoneAccessManager = PhoneAccessManager(application)
  val securePrefs = SecurePrefsManager(application)
  val settingsStore = SettingsDataStore(application)
  val rootfsManager = UbuntuRootfsManager(application)
  val wsTerminalClient = WebSocketTerminalClient()
  val geminiChatClient = GeminiChatClient()

  private var fontSaveJob: Job? = null

  private val _uiState =
    MutableStateFlow(
      WorkspaceUiState(
        hasGeminiApiKey = securePrefs.getGeminiApiKey().isNotBlank(),
        hasGitToken = securePrefs.getGitToken().isNotBlank(),
        deviceContext = phoneAccessManager.inspectDeviceContext(),
      )
    )
  val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      settingsStore.settingsFlow.collect { settings ->
        val prevUri = _uiState.value.mountedPhoneFolderUri
        _uiState.update { state ->
          state.copy(
            themeId = settings.themeId,
            layoutMode = settings.layoutMode,
            windowsCommandAliases = settings.windowsCommandAliases,
            fontSizeSp = settings.fontSizeSp,
            tabWidth = settings.tabWidth,
            wordWrap = settings.wordWrap,
            defaultTerminalMode = settings.defaultTerminalMode,
            webSocketUrl = settings.webSocketUrl,
            geminiModelId = settings.geminiModelId,
            formatterEnabled = settings.formatterEnabled,
            snippetsEnabled = settings.snippetsEnabled,
            markdownPreviewEnabled = settings.markdownPreviewEnabled,
            mountedPhoneFolderUri = settings.mountedPhoneFolderUri,
            mountedPhoneFolderName = settings.mountedPhoneFolderName,
            isMarkdownPreviewActive =
              if (settings.markdownPreviewEnabled) state.isMarkdownPreviewActive else false,
          )
        }
        if (settings.mountedPhoneFolderUri != null && settings.mountedPhoneFolderUri != prevUri) {
          refreshPhoneFolderInternal(settings.mountedPhoneFolderUri)
        }
      }
    }

    viewModelScope.launch {
      runCatching {
        repository.ensureStarterFiles()
        val loadedTasks = taskRepository.loadTasks()
        _uiState.update { it.copy(tasks = loadedTasks) }
        refreshTreeInternal()
        refreshGitInternal()
        val paths = _uiState.value.allFilePaths
        val defaultPath = paths.firstOrNull { it == "main.py" } ?: paths.firstOrNull()
        if (defaultPath != null) {
          // Startup open must not yank the user off a screen they already navigated to.
          openFileInternal(defaultPath, targetLine = null, focusEditor = false)
        }
      }.onFailure { err ->
        _uiState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
      }
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  fun refreshTree() {
    viewModelScope.launch { refreshTreeInternal() }
  }

  private suspend fun refreshTreeInternal() {
    val tree = repository.listTree()
    val files = repository.listAllFilePaths()
    val folders = repository.listAllFolderPaths()
    _uiState.update {
      it.copy(tree = tree, allFilePaths = files, allFolderPaths = folders)
    }
  }

  fun openFile(relativePath: String, targetLine: Int? = null) {
    viewModelScope.launch { openFileInternal(relativePath, targetLine) }
  }

  private suspend fun openFileInternal(
    relativePath: String,
    targetLine: Int?,
    focusEditor: Boolean = true,
  ) {
    val existing = _uiState.value.tabs.firstOrNull { it.relativePath == relativePath }
    if (existing != null) {
      _uiState.update {
        it.copy(
          activeTabPath = relativePath,
          portraitDestination =
            if (focusEditor) PortraitDestination.EDITOR else it.portraitDestination,
          pendingJumpLine = targetLine,
          isCommandPaletteOpen = false,
          isMarkdownPreviewActive = if (targetLine != null) false else it.isMarkdownPreviewActive,
        )
      }
      return
    }
    runCatching {
      val content = repository.readFile(relativePath)
      val name = relativePath.substringAfterLast('/')
      val newTab =
        EditorTab(
          relativePath = relativePath,
          name = name,
          language = CodeLanguage.fromFileName(name),
          savedContent = content,
          currentContent = content,
        )
      _uiState.update {
        it.copy(
          tabs = it.tabs + newTab,
          activeTabPath = relativePath,
          portraitDestination =
            if (focusEditor) PortraitDestination.EDITOR else it.portraitDestination,
          pendingJumpLine = targetLine,
          isCommandPaletteOpen = false,
          errorMessage = null,
        )
      }
    }.onFailure { err ->
      _uiState.update {
        it.copy(errorMessage = err.localizedMessage ?: "Failed to open $relativePath")
      }
    }
  }

  fun clearPendingJumpLine() {
    _uiState.update { it.copy(pendingJumpLine = null) }
  }

  fun selectTab(relativePath: String) {
    _uiState.update { it.copy(activeTabPath = relativePath) }
  }

  fun closeTab(relativePath: String) {
    _uiState.update { state ->
      val remaining = state.tabs.filterNot { it.relativePath == relativePath }
      val nextActive =
        if (state.activeTabPath == relativePath) {
          remaining.lastOrNull()?.relativePath
        } else {
          state.activeTabPath
        }
      state.copy(tabs = remaining, activeTabPath = nextActive)
    }
  }

  fun onEditorTextChanged(relativePath: String, newContent: String) {
    _uiState.update { state ->
      state.copy(
        tabs =
          state.tabs.map { tab ->
            if (tab.relativePath == relativePath) tab.copy(currentContent = newContent) else tab
          }
      )
    }
  }

  fun onCursorStateChanged(
    line: Int,
    column: Int,
    selectedText: String,
    canUndo: Boolean,
    canRedo: Boolean,
  ) {
    _uiState.update { state ->
      val activePath = state.activeTabPath ?: return@update state
      state.copy(
        canUndo = canUndo,
        canRedo = canRedo,
        tabs =
          state.tabs.map { tab ->
            if (tab.relativePath == activePath) {
              tab.copy(cursorLine = line, cursorColumn = column, selectedText = selectedText)
            } else {
              tab
            }
          },
      )
    }
  }

  fun saveActiveFile() {
    val active = _uiState.value.activeTab ?: return
    viewModelScope.launch {
      runCatching {
        val treeUri = active.phoneTreeUri
        val docId = active.phoneDocumentId
        if (treeUri != null && docId != null) {
          phoneAccessManager.writePhoneDocument(treeUri, docId, active.currentContent)
        } else {
          repository.writeFile(active.relativePath, active.currentContent)
          refreshTreeInternal()
          refreshGitInternal()
          if (active.relativePath == ".vscode/tasks.json") {
            _uiState.update { it.copy(tasks = taskRepository.loadTasks()) }
          }
        }
        _uiState.update { state ->
          state.copy(
            tabs =
              state.tabs.map { tab ->
                if (tab.relativePath == active.relativePath) {
                  tab.copy(savedContent = active.currentContent)
                } else {
                  tab
                }
              },
            statusMessage = "Saved ${active.name}",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to save file") }
      }
    }
  }

  fun saveAllFiles() {
    val dirtyTabs = _uiState.value.tabs.filter { it.isDirty }
    if (dirtyTabs.isEmpty()) {
      _uiState.update { it.copy(statusMessage = "All files are already saved") }
      return
    }
    viewModelScope.launch {
      runCatching {
        dirtyTabs.forEach { tab ->
          val treeUri = tab.phoneTreeUri
          val docId = tab.phoneDocumentId
          if (treeUri != null && docId != null) {
            phoneAccessManager.writePhoneDocument(treeUri, docId, tab.currentContent)
          } else {
            repository.writeFile(tab.relativePath, tab.currentContent)
          }
        }
        refreshTreeInternal()
        refreshGitInternal()
        _uiState.update { state ->
          state.copy(
            tabs = state.tabs.map { it.copy(savedContent = it.currentContent) },
            statusMessage = "Saved ${dirtyTabs.size} file(s)",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Save all failed") }
      }
    }
  }

  fun createFile(parentPath: String, fileName: String) {
    viewModelScope.launch {
      runCatching {
        val createdPath = repository.createFile(parentPath, fileName)
        refreshTreeInternal()
        openFileInternal(createdPath, null)
        _uiState.update { it.copy(statusMessage = "Created $createdPath", errorMessage = null) }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Create file failed") }
      }
    }
  }

  fun createFolder(parentPath: String, folderName: String) {
    viewModelScope.launch {
      runCatching {
        val createdPath = repository.createFolder(parentPath, folderName)
        refreshTreeInternal()
        _uiState.update { it.copy(statusMessage = "Created folder $createdPath", errorMessage = null) }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Create folder failed") }
      }
    }
  }

  fun renameEntry(relativePath: String, newName: String) {
    viewModelScope.launch {
      runCatching {
        val updatedPath = repository.renameEntry(relativePath, newName)
        refreshTreeInternal()
        _uiState.update { state ->
          val updatedTabs =
            state.tabs.map { tab ->
              when {
                tab.relativePath == relativePath -> {
                  val name = updatedPath.substringAfterLast('/')
                  tab.copy(
                    relativePath = updatedPath,
                    name = name,
                    language = CodeLanguage.fromFileName(name),
                  )
                }
                tab.relativePath.startsWith("$relativePath/") -> {
                  val suffix = tab.relativePath.removePrefix(relativePath)
                  tab.copy(relativePath = updatedPath + suffix)
                }
                else -> tab
              }
            }
          val nextActive =
            when {
              state.activeTabPath == relativePath -> updatedPath
              state.activeTabPath?.startsWith("$relativePath/") == true ->
                updatedPath + state.activeTabPath.removePrefix(relativePath)
              else -> state.activeTabPath
            }
          state.copy(
            tabs = updatedTabs,
            activeTabPath = nextActive,
            statusMessage = "Renamed to $updatedPath",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Rename failed") }
      }
    }
  }

  fun moveEntry(relativePath: String, targetFolder: String) {
    viewModelScope.launch {
      runCatching {
        val updatedPath = repository.moveEntry(relativePath, targetFolder)
        refreshTreeInternal()
        _uiState.update { state ->
          val updatedTabs =
            state.tabs.map { tab ->
              when {
                tab.relativePath == relativePath -> tab.copy(relativePath = updatedPath)
                tab.relativePath.startsWith("$relativePath/") ->
                  tab.copy(relativePath = updatedPath + tab.relativePath.removePrefix(relativePath))
                else -> tab
              }
            }
          val nextActive =
            when {
              state.activeTabPath == relativePath -> updatedPath
              state.activeTabPath?.startsWith("$relativePath/") == true ->
                updatedPath + state.activeTabPath.removePrefix(relativePath)
              else -> state.activeTabPath
            }
          state.copy(
            tabs = updatedTabs,
            activeTabPath = nextActive,
            statusMessage = "Moved to $updatedPath",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Move failed") }
      }
    }
  }

  fun deleteEntry(relativePath: String) {
    viewModelScope.launch {
      runCatching {
        repository.deleteEntry(relativePath)
        refreshTreeInternal()
        _uiState.update { state ->
          val remaining =
            state.tabs.filterNot {
              it.relativePath == relativePath || it.relativePath.startsWith("$relativePath/")
            }
          val nextActive =
            if (remaining.any { it.relativePath == state.activeTabPath }) {
              state.activeTabPath
            } else {
              remaining.lastOrNull()?.relativePath
            }
          state.copy(
            tabs = remaining,
            activeTabPath = nextActive,
            statusMessage = "Deleted $relativePath",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Delete failed") }
      }
    }
  }

  fun importFiles(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      runCatching {
        val count = repository.importFiles(uris)
        refreshTreeInternal()
        _uiState.update { it.copy(statusMessage = "Imported $count file(s)", errorMessage = null) }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "File import failed") }
      }
    }
  }

  fun importZip(uri: Uri) {
    viewModelScope.launch {
      runCatching {
        val count = repository.importZip(uri)
        refreshTreeInternal()
        _uiState.update {
          it.copy(statusMessage = "Extracted $count file(s) from ZIP", errorMessage = null)
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "ZIP import failed") }
      }
    }
  }

  fun exportZip(uri: Uri) {
    viewModelScope.launch {
      runCatching {
        // Persist unsaved buffers first so exported zip matches editor state
        _uiState.value.tabs.filter { it.isDirty }.forEach { tab ->
          repository.writeFile(tab.relativePath, tab.currentContent)
        }
        val count = repository.exportZip(uri)
        _uiState.update { state ->
          state.copy(
            tabs = state.tabs.map { it.copy(savedContent = it.currentContent) },
            statusMessage = "Exported $count file(s) to ZIP",
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "ZIP export failed") }
      }
    }
  }

  fun performGlobalSearch(query: String, matchCase: Boolean) {
    _uiState.update {
      it.copy(searchQuery = query, searchMatchCase = matchCase, isSearching = query.isNotBlank())
    }
    if (query.isBlank()) {
      _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
      return
    }
    viewModelScope.launch {
      val results = repository.searchAcrossFiles(query, matchCase)
      _uiState.update { it.copy(searchResults = results, isSearching = false) }
    }
  }

  fun setTheme(themeId: String) {
    _uiState.update { it.copy(themeId = themeId) }
    viewModelScope.launch { settingsStore.updateTheme(themeId) }
  }

  fun setLayoutMode(mode: IdeLayoutMode) {
    _uiState.update { it.copy(layoutMode = mode, statusMessage = "Layout: ${mode.shortLabel}") }
    viewModelScope.launch { settingsStore.updateLayoutMode(mode) }
  }

  fun cycleLayoutMode() {
    val modes = IdeLayoutMode.entries
    val next = modes[(modes.indexOf(_uiState.value.layoutMode) + 1) % modes.size]
    setLayoutMode(next)
  }

  fun setWindowsCommandAliases(enabled: Boolean) {
    _uiState.update { it.copy(windowsCommandAliases = enabled) }
    viewModelScope.launch { settingsStore.updateWindowsAliases(enabled) }
  }

  fun setFontSize(sp: Float) {
    val clamped = sp.coerceIn(10f, 28f)
    _uiState.update { it.copy(fontSizeSp = clamped) }
    fontSaveJob?.cancel()
    fontSaveJob =
      viewModelScope.launch {
        delay(250)
        settingsStore.updateFontSize(clamped)
      }
  }

  // --- VS Code Tasks (.vscode/tasks.json) ---
  fun addTask(
    label: String,
    command: String,
    group: TaskGroup,
    cwd: String,
    isDefault: Boolean,
  ) {
    viewModelScope.launch {
      runCatching {
        val updated = taskRepository.addTask(label, command, group, cwd, isDefault)
        refreshTreeInternal()
        _uiState.update { it.copy(tasks = updated, statusMessage = "Added task '$label'") }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to add task") }
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      runCatching {
        val updated = taskRepository.deleteTask(taskId)
        _uiState.update { it.copy(tasks = updated, statusMessage = "Task removed") }
      }
    }
  }

  fun runTask(task: IdeTask) {
    val cmd = taskRepository.buildExecutionCommand(task)
    launchTerminalCommand(title = task.label.take(18), command = cmd)
  }

  fun runDefaultTask() {
    val tasks = _uiState.value.tasks
    val target = tasks.firstOrNull { it.isDefault } ?: tasks.firstOrNull()
    if (target != null) {
      runTask(target)
    } else {
      _uiState.update { it.copy(statusMessage = "No tasks configured yet — add one in Tasks") }
    }
  }

  fun openTasksJson() {
    viewModelScope.launch {
      taskRepository.loadTasks()
      refreshTreeInternal()
      openFileInternal(".vscode/tasks.json", targetLine = null)
    }
  }

  // --- Phone Folder (SAF), Location & Hardware Context ---
  fun mountPhoneFolder(treeUri: Uri) {
    viewModelScope.launch {
      runCatching {
        val name = phoneAccessManager.persistTreePermission(treeUri)
        val uriString = treeUri.toString()
        settingsStore.updateMountedPhoneFolder(uriString, name)
        refreshPhoneFolderInternal(uriString)
        _uiState.update {
          it.copy(
            mountedPhoneFolderUri = uriString,
            mountedPhoneFolderName = name,
            statusMessage = "Mounted phone folder '$name'",
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Unable to mount phone folder") }
      }
    }
  }

  fun unmountPhoneFolder() {
    viewModelScope.launch {
      settingsStore.updateMountedPhoneFolder(null, null)
      _uiState.update {
        it.copy(
          mountedPhoneFolderUri = null,
          mountedPhoneFolderName = null,
          phoneTree = emptyList(),
          statusMessage = "Phone folder unmounted",
        )
      }
    }
  }

  fun refreshPhoneFolder() {
    val uri = _uiState.value.mountedPhoneFolderUri ?: return
    viewModelScope.launch { refreshPhoneFolderInternal(uri) }
  }

  private suspend fun refreshPhoneFolderInternal(uriString: String) {
    runCatching {
      val tree = phoneAccessManager.listPhoneTree(uriString)
      _uiState.update { it.copy(phoneTree = tree) }
    }.onFailure { err ->
      _uiState.update {
        it.copy(errorMessage = err.localizedMessage ?: "Failed to read mounted phone folder")
      }
    }
  }

  fun openPhoneFile(node: FileNode) {
    val treeUri = _uiState.value.mountedPhoneFolderUri ?: return
    val docId = node.documentId ?: return
    val tabKey = "phone://${node.relativePath}"
    val existing = _uiState.value.tabs.firstOrNull { it.relativePath == tabKey }
    if (existing != null) {
      _uiState.update {
        it.copy(
          activeTabPath = tabKey,
          portraitDestination = PortraitDestination.EDITOR,
          isCommandPaletteOpen = false,
        )
      }
      return
    }
    viewModelScope.launch {
      runCatching {
        val content = phoneAccessManager.readPhoneDocument(treeUri, docId)
        val newTab =
          EditorTab(
            relativePath = tabKey,
            name = node.name,
            language = CodeLanguage.fromFileName(node.name),
            savedContent = content,
            currentContent = content,
            phoneTreeUri = treeUri,
            phoneDocumentId = docId,
          )
        _uiState.update {
          it.copy(
            tabs = it.tabs + newTab,
            activeTabPath = tabKey,
            portraitDestination = PortraitDestination.EDITOR,
            isCommandPaletteOpen = false,
            errorMessage = null,
          )
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Cannot open ${node.name}") }
      }
    }
  }

  fun createPhoneEntry(parentDocId: String?, name: String, isDir: Boolean) {
    val treeUri = _uiState.value.mountedPhoneFolderUri ?: return
    viewModelScope.launch {
      runCatching {
        phoneAccessManager.createPhoneDocument(treeUri, parentDocId, name, isDir)
        refreshPhoneFolderInternal(treeUri)
        _uiState.update { it.copy(statusMessage = "Created '$name' on phone storage") }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Create failed on phone storage") }
      }
    }
  }

  fun renamePhoneEntry(docId: String, newName: String) {
    val treeUri = _uiState.value.mountedPhoneFolderUri ?: return
    viewModelScope.launch {
      runCatching {
        phoneAccessManager.renamePhoneDocument(treeUri, docId, newName)
        refreshPhoneFolderInternal(treeUri)
        _uiState.update { it.copy(statusMessage = "Renamed to '$newName'") }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Rename failed on phone storage") }
      }
    }
  }

  fun deletePhoneEntry(docId: String) {
    val treeUri = _uiState.value.mountedPhoneFolderUri ?: return
    viewModelScope.launch {
      runCatching {
        phoneAccessManager.deletePhoneDocument(treeUri, docId)
        refreshPhoneFolderInternal(treeUri)
        _uiState.update { state ->
          val remaining = state.tabs.filterNot { it.phoneDocumentId == docId }
          val nextActive =
            if (remaining.any { it.relativePath == state.activeTabPath }) state.activeTabPath
            else remaining.lastOrNull()?.relativePath
          state.copy(tabs = remaining, activeTabPath = nextActive, statusMessage = "Deleted phone entry")
        }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Delete failed on phone storage") }
      }
    }
  }

  fun refreshDeviceContext() {
    _uiState.update { it.copy(deviceContext = phoneAccessManager.inspectDeviceContext()) }
  }

  fun fetchDeviceLocation() {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isFetchingLocation = true,
          deviceContext = phoneAccessManager.inspectDeviceContext(),
        )
      }
      val snapshot = phoneAccessManager.fetchCurrentLocation()
      _uiState.update {
        it.copy(
          isFetchingLocation = false,
          locationSnapshot = snapshot ?: it.locationSnapshot,
          statusMessage =
            if (snapshot != null) "Location updated (${snapshot.provider})"
            else "No GPS/Network fix available yet — ensure device Location is enabled",
        )
      }
    }
  }

  fun saveLocationToWorkspace() {
    val loc = _uiState.value.locationSnapshot ?: return
    viewModelScope.launch {
      runCatching {
        repository.writeFile("location.json", loc.toJsonString())
        refreshTreeInternal()
        openFileInternal("location.json", targetLine = null)
        _uiState.update { it.copy(statusMessage = "Saved location.json in workspace") }
      }.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed saving location.json") }
      }
    }
  }

  fun setTabWidth(width: Int) {
    val normalized = if (width == 4) 4 else 2
    _uiState.update { it.copy(tabWidth = normalized) }
    viewModelScope.launch { settingsStore.updateTabWidth(normalized) }
  }

  fun toggleWordWrap() {
    val next = !_uiState.value.wordWrap
    _uiState.update { it.copy(wordWrap = next) }
    viewModelScope.launch { settingsStore.updateWordWrap(next) }
  }

  fun setFormatterEnabled(enabled: Boolean) {
    _uiState.update { it.copy(formatterEnabled = enabled) }
    viewModelScope.launch { settingsStore.updateFormatterEnabled(enabled) }
  }

  fun setSnippetsEnabled(enabled: Boolean) {
    _uiState.update { it.copy(snippetsEnabled = enabled) }
    viewModelScope.launch { settingsStore.updateSnippetsEnabled(enabled) }
  }

  fun setMarkdownPreviewEnabled(enabled: Boolean) {
    _uiState.update {
      it.copy(
        markdownPreviewEnabled = enabled,
        isMarkdownPreviewActive = if (enabled) it.isMarkdownPreviewActive else false,
      )
    }
    viewModelScope.launch { settingsStore.updateMarkdownPreviewEnabled(enabled) }
  }

  fun toggleMarkdownPreview() {
    if (!_uiState.value.markdownPreviewEnabled) return
    _uiState.update { it.copy(isMarkdownPreviewActive = !it.isMarkdownPreviewActive) }
  }

  fun formatActiveFile(controller: SoraEditorController) {
    val state = _uiState.value
    if (!state.formatterEnabled) {
      _uiState.update { it.copy(statusMessage = "Code Formatter is disabled in Built-in Features") }
      return
    }
    val tab = state.activeTab ?: return
    CodeFormatter.format(tab.currentContent, tab.language, state.tabWidth)
      .onSuccess { formatted ->
        if (formatted != tab.currentContent) {
          controller.replaceAllText(formatted)
        }
        _uiState.update { it.copy(statusMessage = "Formatted ${tab.name}", errorMessage = null) }
      }
      .onFailure { err ->
        _uiState.update {
          it.copy(errorMessage = err.localizedMessage ?: "Format failed for ${tab.name}")
        }
      }
  }

  fun setFindBarVisible(visible: Boolean) {
    _uiState.update { it.copy(isFindBarVisible = visible) }
  }

  fun updateFindOptions(
    findQuery: String? = null,
    replaceQuery: String? = null,
    matchCase: Boolean? = null,
    useRegex: Boolean? = null,
  ) {
    _uiState.update {
      it.copy(
        findQuery = findQuery ?: it.findQuery,
        replaceQuery = replaceQuery ?: it.replaceQuery,
        findMatchCase = matchCase ?: it.findMatchCase,
        findUseRegex = useRegex ?: it.findUseRegex,
      )
    }
  }

  fun setCommandPaletteOpen(open: Boolean) {
    _uiState.update { it.copy(isCommandPaletteOpen = open) }
  }

  fun selectSidebarPanel(panel: SidebarPanel) {
    if (panel == SidebarPanel.TERMINAL) {
      _uiState.update { it.copy(isBottomTerminalOpenLandscape = !it.isBottomTerminalOpenLandscape) }
      return
    }
    _uiState.update { state ->
      if (state.activeSidebarPanel == panel && state.isSidebarOpenLandscape) {
        state.copy(isSidebarOpenLandscape = false)
      } else {
        state.copy(activeSidebarPanel = panel, isSidebarOpenLandscape = true)
      }
    }
  }

  fun launchTerminalCommand(title: String, command: String, requireUbuntu: Boolean = false) {
    _uiState.update {
      it.copy(
        pendingTerminalLaunch =
          TerminalLaunchRequest(title = title, command = command, requireUbuntu = requireUbuntu),
        portraitDestination = PortraitDestination.TERMINAL,
        isBottomTerminalOpenLandscape = true,
      )
    }
  }

  fun clearPendingTerminalLaunch() {
    _uiState.update { it.copy(pendingTerminalLaunch = null) }
  }

  fun updateWebSocketUrl(url: String) {
    _uiState.update { it.copy(webSocketUrl = url) }
    viewModelScope.launch { settingsStore.updateWebSocketUrl(url) }
  }

  fun setDefaultTerminalMode(mode: TerminalMode) {
    _uiState.update { it.copy(defaultTerminalMode = mode) }
    viewModelScope.launch { settingsStore.updateTerminalMode(mode) }
  }

  override fun onCleared() {
    super.onCleared()
    wsTerminalClient.disconnect()
  }

  fun selectPortraitDestination(destination: PortraitDestination) {
    _uiState.update { it.copy(portraitDestination = destination) }
  }

  fun clearNotifications() {
    _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun dismissGitError() {
    _uiState.update { it.copy(gitError = null) }
  }

  fun refreshGit() {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true) }
      refreshGitInternal()
      _uiState.update { it.copy(gitBusy = false) }
    }
  }

  private suspend fun refreshGitInternal() {
    runCatching {
      val snapshot = gitRepository.inspectStatus()
      _uiState.update {
        it.copy(
          gitSnapshot = snapshot,
          branchName = snapshot.branchName,
          gitError = null,
        )
      }
    }.onFailure { err ->
      _uiState.update {
        it.copy(gitError = err.localizedMessage ?: "Failed to read Git status")
      }
    }
  }

  fun initGitRepo() {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val snapshot = gitRepository.initRepository()
        _uiState.update {
          it.copy(
            gitSnapshot = snapshot,
            branchName = snapshot.branchName,
            gitBusy = false,
            statusMessage = "Initialized Git repository on '${snapshot.branchName}'",
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "git init failed")
        }
      }
    }
  }

  fun cloneGitRepo(url: String, tokenInput: String, saveToken: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      val effectiveToken =
        tokenInput.ifBlank { securePrefs.getGitToken() }.takeIf { it.isNotBlank() }
      if (tokenInput.isNotBlank() && saveToken) {
        securePrefs.setGitToken(tokenInput)
        _uiState.update { it.copy(hasGitToken = true) }
      }
      runCatching {
        val snapshot = gitRepository.cloneRepository(url, effectiveToken)
        refreshTreeInternal()
        _uiState.update {
          it.copy(
            gitSnapshot = snapshot,
            branchName = snapshot.branchName,
            gitBusy = false,
            statusMessage = "Cloned repository successfully",
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "git clone failed")
        }
      }
    }
  }

  fun stageGitFile(path: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val snapshot = gitRepository.stageFile(path)
        _uiState.update { it.copy(gitSnapshot = snapshot, gitBusy = false) }
      }.onFailure { err ->
        _uiState.update { it.copy(gitBusy = false, gitError = err.localizedMessage ?: "Stage failed") }
      }
    }
  }

  fun stageAllGitFiles() {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val snapshot = gitRepository.stageAll()
        _uiState.update { it.copy(gitSnapshot = snapshot, gitBusy = false) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "Stage all failed")
        }
      }
    }
  }

  fun unstageGitFile(path: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val snapshot = gitRepository.unstageFile(path)
        _uiState.update { it.copy(gitSnapshot = snapshot, gitBusy = false) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "Unstage failed")
        }
      }
    }
  }

  fun commitGit(message: String, authorName: String, authorEmail: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val snapshot = gitRepository.commit(message, authorName, authorEmail)
        _uiState.update {
          it.copy(
            gitSnapshot = snapshot,
            branchName = snapshot.branchName,
            gitBusy = false,
            statusMessage = "Committed changes",
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "Commit failed")
        }
      }
    }
  }

  fun openGitDiff(change: GitFileChange) {
    viewModelScope.launch {
      _uiState.update { it.copy(gitBusy = true, gitError = null) }
      runCatching {
        val lines = gitRepository.computeDiff(change)
        _uiState.update { it.copy(diffTarget = change, diffLines = lines, gitBusy = false) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(gitBusy = false, gitError = err.localizedMessage ?: "Unable to compute diff")
        }
      }
    }
  }

  fun closeGitDiff() {
    _uiState.update { it.copy(diffTarget = null, diffLines = emptyList()) }
  }

  fun openClaudeCodeCli() {
    val cmd =
      "if command -v claude >/dev/null 2>&1; then claude; else echo 'claude CLI not found in current PATH. Switch to Ubuntu proot tab and run Setup AI CLIs (requires libproot.so), or use Termux + ttyd in WebSocket mode.'; fi"
    launchTerminalCommand("claude-code", cmd)
  }

  fun openGeminiCli() {
    val cmd =
      "if command -v gemini >/dev/null 2>&1; then gemini; else echo 'gemini CLI not found in current PATH. Switch to Ubuntu proot tab and run Setup AI CLIs (requires libproot.so), or use Termux + ttyd in WebSocket mode.'; fi"
    launchTerminalCommand("gemini-cli", cmd)
  }

  fun saveGeminiApiKey(key: String) {
    securePrefs.setGeminiApiKey(key)
    _uiState.update {
      it.copy(
        hasGeminiApiKey = key.trim().isNotEmpty(),
        aiError = null,
        statusMessage = if (key.trim().isEmpty()) "Gemini API key cleared" else "Gemini API key saved (encrypted)",
      )
    }
  }

  fun saveGitToken(token: String) {
    securePrefs.setGitToken(token)
    _uiState.update {
      it.copy(
        hasGitToken = token.trim().isNotEmpty(),
        statusMessage = if (token.trim().isEmpty()) "Git token cleared" else "Git token saved (encrypted)",
      )
    }
  }

  fun clearGitToken() {
    saveGitToken("")
  }

  fun updateGeminiModelId(modelId: String) {
    val clean = modelId.trim().ifEmpty { GeminiChatClient.DEFAULT_MODEL }
    _uiState.update { it.copy(geminiModelId = clean, statusMessage = "Gemini model set to $clean") }
    viewModelScope.launch { settingsStore.updateGeminiModelId(clean) }
  }

  fun clearAiChat() {
    _uiState.update { it.copy(aiMessages = emptyList(), aiError = null) }
  }

  fun runAiCodeAction(action: String) {
    val tab = _uiState.value.activeTab ?: return
    val snippet = tab.selectedText.ifBlank { tab.currentContent.take(6000) }
    val scopeDesc = if (tab.selectedText.isNotBlank()) "selected code" else "file ${tab.name}"
    val prompt =
      "$action the following ${tab.language.displayName} $scopeDesc from `${tab.relativePath}`:\n\n```${tab.language.displayName.lowercase()}\n$snippet\n```"
    sendAiChatMessage(prompt)
  }

  fun sendAiChatMessage(prompt: String) {
    val key = securePrefs.getGeminiApiKey()
    if (key.isBlank()) {
      _uiState.update {
        it.copy(aiError = "Gemini API key is missing. Add it in Settings first.")
      }
      return
    }
    val userMsg = AiChatMessage(System.currentTimeMillis(), isUser = true, text = prompt)
    val priorHistory = _uiState.value.aiMessages
    _uiState.update {
      it.copy(
        aiMessages = it.aiMessages + userMsg,
        aiBusy = true,
        aiError = null,
      )
    }
    viewModelScope.launch {
      runCatching {
        geminiChatClient.sendPrompt(
          apiKey = key,
          modelId = _uiState.value.geminiModelId,
          history = priorHistory,
          userPrompt = prompt,
        )
      }.onSuccess { reply ->
        val modelMsg = AiChatMessage(System.currentTimeMillis() + 1L, isUser = false, text = reply)
        _uiState.update { it.copy(aiMessages = it.aiMessages + modelMsg, aiBusy = false) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(aiBusy = false, aiError = err.localizedMessage ?: "Gemini request failed")
        }
      }
    }
  }
}
