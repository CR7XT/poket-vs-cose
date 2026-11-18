package com.example.ui.explorer

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.FileNode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

private data class FlattenedTreeRow(
  val node: FileNode,
  val depth: Int,
)

private sealed interface ExplorerDialogState {
  data class NewFile(val parentPath: String, val parentDocId: String?) : ExplorerDialogState
  data class NewFolder(val parentPath: String, val parentDocId: String?) : ExplorerDialogState
  data class Rename(val node: FileNode) : ExplorerDialogState
  data class Move(val node: FileNode) : ExplorerDialogState
  data class Delete(val node: FileNode) : ExplorerDialogState
}

@Composable
fun ExplorerSidebar(
  tree: List<FileNode>,
  allFolderPaths: List<String>,
  activeTabPath: String?,
  mountedPhoneFolderName: String?,
  phoneTree: List<FileNode>,
  onOpenFile: (String) -> Unit,
  onCreateFile: (parentPath: String, name: String) -> Unit,
  onCreateFolder: (parentPath: String, name: String) -> Unit,
  onRenameEntry: (path: String, newName: String) -> Unit,
  onMoveEntry: (path: String, targetFolder: String) -> Unit,
  onDeleteEntry: (path: String) -> Unit,
  onImportFiles: () -> Unit,
  onImportZip: () -> Unit,
  onExportZip: () -> Unit,
  onMountPhoneFolder: () -> Unit,
  onUnmountPhoneFolder: () -> Unit,
  onRefreshPhoneFolder: () -> Unit,
  onOpenPhoneFile: (node: FileNode) -> Unit,
  onCreatePhoneEntry: (parentDocId: String?, name: String, isDir: Boolean) -> Unit,
  onRenamePhoneEntry: (docId: String, newName: String) -> Unit,
  onDeletePhoneEntry: (docId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }
  var dialogState by remember { mutableStateOf<ExplorerDialogState?>(null) }
  var viewingPhoneFolder by remember(mountedPhoneFolderName) { mutableStateOf(false) }

  val isPhoneView = viewingPhoneFolder && mountedPhoneFolderName != null
  val activeTree = if (isPhoneView) phoneTree else tree

  val rows =
    remember(activeTree, expandedFolders.toMap()) {
      buildList {
        fun visit(nodes: List<FileNode>, depth: Int) {
          for (node in nodes) {
            add(FlattenedTreeRow(node, depth))
            val key = node.documentId ?: node.relativePath
            if (node.isDirectory && (expandedFolders[key] ?: (depth == 0 && !node.isPhoneDocument))) {
              visit(node.children, depth + 1)
            }
          }
        }
        visit(activeTree, 0)
      }
    }

  Column(
    modifier = modifier.fillMaxSize().background(palette.sidebarBg),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        "EXPLORER",
        color = palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { dialogState = ExplorerDialogState.NewFile("", null) },
          modifier = Modifier.size(32.dp).testTag("explorer_new_file_button"),
        ) {
          Icon(
            Icons.Default.NoteAdd,
            contentDescription = "New File",
            tint = palette.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
        IconButton(
          onClick = { dialogState = ExplorerDialogState.NewFolder("", null) },
          modifier = Modifier.size(32.dp).testTag("explorer_new_folder_button"),
        ) {
          Icon(
            Icons.Default.CreateNewFolder,
            contentDescription = "New Folder",
            tint = palette.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
        if (isPhoneView) {
          IconButton(
            onClick = onRefreshPhoneFolder,
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = "Refresh Phone Folder",
              tint = palette.textPrimary,
              modifier = Modifier.size(17.dp),
            )
          }
        } else {
          IconButton(
            onClick = onImportFiles,
            modifier = Modifier.size(32.dp).testTag("explorer_import_files_button"),
          ) {
            Icon(
              Icons.Default.FileOpen,
              contentDescription = "Import Files",
              tint = palette.textPrimary,
              modifier = Modifier.size(17.dp),
            )
          }
          IconButton(
            onClick = onImportZip,
            modifier = Modifier.size(32.dp).testTag("explorer_import_zip_button"),
          ) {
            Icon(
              Icons.Default.FolderZip,
              contentDescription = "Import ZIP",
              tint = palette.textPrimary,
              modifier = Modifier.size(17.dp),
            )
          }
          IconButton(
            onClick = onExportZip,
            modifier = Modifier.size(32.dp).testTag("explorer_export_zip_button"),
          ) {
            Icon(
              Icons.Default.Archive,
              contentDescription = "Export Project ZIP",
              tint = palette.textPrimary,
              modifier = Modifier.size(17.dp),
            )
          }
        }
      }
    }

    // Source Switcher: Workspace vs Phone Storage Folder (SAF)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(palette.activityBarBg.copy(alpha = 0.65f))
          .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SourceChip(
        label = "Workspace",
        selected = !isPhoneView,
        onClick = { viewingPhoneFolder = false },
        modifier = Modifier.weight(1f).testTag("explorer_tab_workspace"),
      )
      if (mountedPhoneFolderName != null) {
        Row(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isPhoneView) palette.accent else palette.inputBg)
              .clickable { viewingPhoneFolder = true }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("explorer_tab_phone"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = mountedPhoneFolderName,
            color = if (isPhoneView) Color.White else palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Icon(
            Icons.Default.Close,
            contentDescription = "Unmount phone folder",
            tint = if (isPhoneView) Color.White else palette.textMuted,
            modifier =
              Modifier.size(14.dp).clickable {
                viewingPhoneFolder = false
                onUnmountPhoneFolder()
              },
          )
        }
      } else {
        Row(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(palette.inputBg)
              .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
              .clickable { onMountPhoneFolder() }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("explorer_open_phone_folder_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            Icons.Default.FolderShared,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(13.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = "Phone Files",
            color = palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    if (rows.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            if (isPhoneView) "This phone folder is empty."
            else "Workspace is empty.\nCreate a file, import a ZIP, or open a phone folder.",
            color = palette.textMuted,
            fontSize = 13.sp,
          )
          if (!isPhoneView) {
            Spacer(Modifier.height(10.dp))
            Button(
              onClick = onMountPhoneFolder,
              colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
              Text("Open Phone Folder", color = Color.White, fontSize = 12.sp)
            }
          }
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.node.documentId ?: it.node.relativePath }) { row ->
          val node = row.node
          val expandKey = node.documentId ?: node.relativePath
          val isExpanded = expandedFolders[expandKey] ?: (row.depth == 0 && !node.isPhoneDocument)
          val isSelected =
            !node.isDirectory &&
              (node.relativePath == activeTabPath ||
                (node.documentId != null && activeTabPath == "phone://${node.relativePath}"))
          var menuExpanded by remember { mutableStateOf(false) }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (isSelected) palette.selectionBg.copy(alpha = 0.65f)
                  else palette.sidebarBg
                )
                .clickable {
                  if (node.isDirectory) {
                    expandedFolders[expandKey] = !isExpanded
                  } else if (node.isPhoneDocument) {
                    onOpenPhoneFile(node)
                  } else {
                    onOpenFile(node.relativePath)
                  }
                }
                .padding(
                  start = (8 + row.depth * 14).dp,
                  end = 4.dp,
                  top = 6.dp,
                  bottom = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (node.isDirectory) {
              Icon(
                imageVector =
                  if (isExpanded) Icons.Default.KeyboardArrowDown
                  else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(2.dp))
              Icon(
                imageVector =
                  if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = palette.modifiedFg,
                modifier = Modifier.size(17.dp),
              )
            } else {
              Spacer(Modifier.width(18.dp))
              val lang = CodeLanguage.fromFileName(node.name)
              Text(
                text = lang.badge,
                color = lang.badgeColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(22.dp),
              )
            }

            Spacer(Modifier.width(6.dp))
            Text(
              text = node.name,
              color = palette.textPrimary,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )

            Box {
              IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(30.dp),
              ) {
                Icon(
                  Icons.Default.MoreVert,
                  contentDescription = "File actions for ${node.name}",
                  tint = palette.textMuted,
                  modifier = Modifier.size(15.dp),
                )
              }
              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
              ) {
                if (node.isDirectory) {
                  DropdownMenuItem(
                    text = { Text("New File Here") },
                    leadingIcon = { Icon(Icons.Default.NoteAdd, null) },
                    onClick = {
                      menuExpanded = false
                      dialogState = ExplorerDialogState.NewFile(node.relativePath, node.documentId)
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("New Folder Here") },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                    onClick = {
                      menuExpanded = false
                      dialogState = ExplorerDialogState.NewFolder(node.relativePath, node.documentId)
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text("Rename") },
                  leadingIcon = { Icon(Icons.Default.Edit, null) },
                  onClick = {
                    menuExpanded = false
                    dialogState = ExplorerDialogState.Rename(node)
                  },
                )
                if (!node.isPhoneDocument) {
                  DropdownMenuItem(
                    text = { Text("Move To...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                    onClick = {
                      menuExpanded = false
                      dialogState = ExplorerDialogState.Move(node)
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text("Delete") },
                  leadingIcon = { Icon(Icons.Default.Delete, null) },
                  onClick = {
                    menuExpanded = false
                    dialogState = ExplorerDialogState.Delete(node)
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  when (val state = dialogState) {
    is ExplorerDialogState.NewFile -> {
      NameInputDialog(
        title = if (isPhoneView) "New Phone File" else "New File",
        label = "File name (e.g. utils.py)",
        initialValue = "",
        confirmLabel = "Create",
        onConfirm = { name ->
          if (isPhoneView) {
            onCreatePhoneEntry(state.parentDocId, name, false)
          } else {
            onCreateFile(state.parentPath, name)
          }
          dialogState = null
        },
        onDismiss = { dialogState = null },
      )
    }
    is ExplorerDialogState.NewFolder -> {
      NameInputDialog(
        title = if (isPhoneView) "New Phone Folder" else "New Folder",
        label = "Folder name",
        initialValue = "",
        confirmLabel = "Create",
        onConfirm = { name ->
          if (isPhoneView) {
            onCreatePhoneEntry(state.parentDocId, name, true)
          } else {
            onCreateFolder(state.parentPath, name)
          }
          dialogState = null
        },
        onDismiss = { dialogState = null },
      )
    }
    is ExplorerDialogState.Rename -> {
      NameInputDialog(
        title = "Rename ${state.node.name}",
        label = "New name",
        initialValue = state.node.name,
        confirmLabel = "Rename",
        onConfirm = { newName ->
          val docId = state.node.documentId
          if (docId != null) {
            onRenamePhoneEntry(docId, newName)
          } else {
            onRenameEntry(state.node.relativePath, newName)
          }
          dialogState = null
        },
        onDismiss = { dialogState = null },
      )
    }
    is ExplorerDialogState.Move -> {
      MoveTargetDialog(
        entry = state.node,
        folders = allFolderPaths,
        onSelectFolder = { target ->
          onMoveEntry(state.node.relativePath, target)
          dialogState = null
        },
        onDismiss = { dialogState = null },
      )
    }
    is ExplorerDialogState.Delete -> {
      AlertDialog(
        onDismissRequest = { dialogState = null },
        title = { Text("Delete ${state.node.name}?") },
        text = {
          Text(
            if (state.node.isDirectory) "This deletes the folder and its contents."
            else "This permanently deletes ${state.node.relativePath}."
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              val docId = state.node.documentId
              if (docId != null) {
                onDeletePhoneEntry(docId)
              } else {
                onDeleteEntry(state.node.relativePath)
              }
              dialogState = null
            }
          ) {
            Text("Delete", color = palette.deletedFg)
          }
        },
        dismissButton = {
          TextButton(onClick = { dialogState = null }) { Text("Cancel") }
        },
      )
    }
    null -> Unit
  }
}

@Composable
private fun SourceChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(4.dp))
        .background(if (selected) palette.accent else palette.inputBg)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else palette.textPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun NameInputDialog(
  title: String,
  label: String,
  initialValue: String,
  confirmLabel: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var value by remember(initialValue) { mutableStateOf(initialValue) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("explorer_name_input"),
      )
    },
    confirmButton = {
      TextButton(
        enabled = value.trim().isNotEmpty(),
        onClick = { onConfirm(value.trim()) },
        modifier = Modifier.testTag("explorer_dialog_confirm"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun MoveTargetDialog(
  entry: FileNode,
  folders: List<String>,
  onSelectFolder: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalIdePalette.current
  val validFolders =
    folders.filterNot {
      entry.isDirectory && (it == entry.relativePath || it.startsWith("${entry.relativePath}/"))
    }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Move '${entry.name}' to folder") },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        validFolders.forEach { folder ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelectFolder(folder) }
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.Folder,
              contentDescription = null,
              tint = palette.modifiedFg,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              if (folder.isEmpty()) "/ (workspace root)" else "/$folder",
              color = palette.textPrimary,
              fontSize = 13.sp,
            )
          }
        }
        if (validFolders.isEmpty()) {
          Spacer(Modifier.height(4.dp))
          Text("No destination folders available", color = palette.textMuted, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
