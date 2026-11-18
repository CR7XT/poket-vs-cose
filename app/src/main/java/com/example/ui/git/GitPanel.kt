package com.example.ui.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.git.DiffLineType
import com.example.data.git.GitChangeType
import com.example.data.git.GitDiffLine
import com.example.data.git.GitFileChange
import com.example.data.git.GitStatusSnapshot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun GitPanel(
  snapshot: GitStatusSnapshot,
  isBusy: Boolean,
  errorBanner: String?,
  savedTokenPresent: Boolean,
  diffTarget: GitFileChange?,
  diffLines: List<GitDiffLine>,
  onDismissError: () -> Unit,
  onRefresh: () -> Unit,
  onInitRepo: () -> Unit,
  onCloneRepo: (url: String, token: String, saveToken: Boolean) -> Unit,
  onStageFile: (String) -> Unit,
  onStageAll: () -> Unit,
  onUnstageFile: (String) -> Unit,
  onCommit: (message: String, authorName: String, authorEmail: String) -> Unit,
  onOpenDiff: (GitFileChange) -> Unit,
  onCloseDiff: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  var showCloneDialog by remember { mutableStateOf(false) }
  var commitMessage by remember { mutableStateOf("") }
  var authorName by remember { mutableStateOf("PocketCode User") }
  var authorEmail by remember { mutableStateOf("developer@pocketcode.local") }

  Column(modifier = modifier.fillMaxSize().background(palette.sidebarBg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        "SOURCE CONTROL",
        color = palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { showCloneDialog = true },
          enabled = !isBusy,
          modifier = Modifier.size(32.dp).testTag("git_clone_toolbar_button"),
        ) {
          Icon(
            Icons.Default.CloudDownload,
            contentDescription = "Clone HTTPS Repository",
            tint = palette.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
        IconButton(
          onClick = onRefresh,
          enabled = !isBusy,
          modifier = Modifier.size(32.dp).testTag("git_refresh_button"),
        ) {
          Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh Git Status",
            tint = palette.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }

    if (isBusy) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = palette.accent)
    }

    if (errorBanner != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF5A1D1D))
            .border(0.5.dp, palette.deletedFg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = palette.deletedFg,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          errorBanner,
          color = Color.White,
          fontSize = 12.sp,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismissError, modifier = Modifier.size(26.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
        }
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    if (!snapshot.isRepo) {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          "This workspace does not have a Git repository yet.",
          color = palette.textPrimary,
          fontSize = 13.sp,
        )
        Button(
          onClick = onInitRepo,
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
          modifier = Modifier.fillMaxWidth().testTag("git_init_button"),
        ) {
          Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text("Initialize Repository")
        }
        OutlinedButton(
          onClick = { showCloneDialog = true },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().testTag("git_clone_button"),
        ) {
          Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text("Clone Repository (HTTPS)")
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.AccountTree,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(15.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                "Branch: ${snapshot.branchName}",
                color = palette.textPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }

            OutlinedTextField(
              value = commitMessage,
              onValueChange = { commitMessage = it },
              label = { Text("Commit message", fontSize = 12.sp) },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth().testTag("git_commit_message_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = authorName,
                onValueChange = { authorName = it },
                label = { Text("Author", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = authorEmail,
                onValueChange = { authorEmail = it },
                label = { Text("Email", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }

            Button(
              onClick = {
                val msg = commitMessage
                onCommit(msg, authorName, authorEmail)
                commitMessage = ""
              },
              enabled = !isBusy && commitMessage.isNotBlank() && snapshot.staged.isNotEmpty(),
              colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
              modifier = Modifier.fillMaxWidth().testTag("git_commit_button"),
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Commit Staged (${snapshot.staged.size})")
            }
          }
          HorizontalDivider(color = palette.border, thickness = 0.5.dp)
        }

        // Staged Changes
        item {
          SectionHeader(
            title = "STAGED CHANGES (${snapshot.staged.size})",
            actionIcon = null,
            actionDescription = null,
            onAction = null,
          )
        }
        items(snapshot.staged, key = { "staged_${it.path}" }) { change ->
          GitChangeRow(
            change = change,
            actionIcon = Icons.Default.Remove,
            actionDesc = "Unstage ${change.path}",
            onAction = { onUnstageFile(change.path) },
            onClick = { onOpenDiff(change) },
          )
        }

        // Unstaged & Untracked Changes
        item {
          SectionHeader(
            title = "CHANGES (${snapshot.unstaged.size})",
            actionIcon = if (snapshot.unstaged.isNotEmpty()) Icons.Default.DoneAll else null,
            actionDescription = "Stage All Changes",
            onAction = onStageAll,
          )
        }
        items(snapshot.unstaged, key = { "unstaged_${it.path}" }) { change ->
          GitChangeRow(
            change = change,
            actionIcon = Icons.Default.Add,
            actionDesc = "Stage ${change.path}",
            onAction = { onStageFile(change.path) },
            onClick = { onOpenDiff(change) },
          )
        }

        // Commit History
        item {
          HorizontalDivider(color = palette.border, thickness = 0.5.dp)
          SectionHeader(
            title = "COMMITS (${snapshot.commits.size})",
            actionIcon = null,
            actionDescription = null,
            onAction = null,
          )
        }
        if (snapshot.commits.isEmpty()) {
          item {
            Text(
              "No commits yet.",
              color = palette.textMuted,
              fontSize = 12.sp,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        } else {
          items(snapshot.commits, key = { it.shortId }) { commit ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  commit.shortId,
                  color = palette.accent,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                )
                Text(commit.formattedDate, color = palette.textMuted, fontSize = 11.sp)
              }
              Text(
                commit.message,
                color = palette.textPrimary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Text(commit.author, color = palette.textMuted, fontSize = 11.sp)
            }
            HorizontalDivider(color = palette.border.copy(alpha = 0.4f), thickness = 0.5.dp)
          }
        }
      }
    }
  }

  if (showCloneDialog) {
    CloneRepoDialog(
      hasSavedToken = savedTokenPresent,
      onConfirm = { url, token, saveToken ->
        showCloneDialog = false
        onCloneRepo(url, token, saveToken)
      },
      onDismiss = { showCloneDialog = false },
    )
  }

  if (diffTarget != null) {
    GitDiffDialog(
      change = diffTarget,
      lines = diffLines,
      onDismiss = onCloseDiff,
    )
  }
}

@Composable
private fun SectionHeader(
  title: String,
  actionIcon: androidx.compose.ui.graphics.vector.ImageVector?,
  actionDescription: String?,
  onAction: (() -> Unit)?,
) {
  val palette = LocalIdePalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(palette.tabInactiveBg.copy(alpha = 0.5f))
        .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      title,
      color = palette.textMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
    )
    if (actionIcon != null && onAction != null) {
      IconButton(
        onClick = onAction,
        modifier = Modifier.size(26.dp).testTag("git_stage_all_button"),
      ) {
        Icon(
          actionIcon,
          contentDescription = actionDescription,
          tint = palette.textPrimary,
          modifier = Modifier.size(15.dp),
        )
      }
    }
  }
}

@Composable
private fun GitChangeRow(
  change: GitFileChange,
  actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
  actionDesc: String,
  onAction: () -> Unit,
  onClick: () -> Unit,
) {
  val palette = LocalIdePalette.current
  val badgeColor =
    when (change.type) {
      GitChangeType.ADDED, GitChangeType.UNTRACKED -> palette.addedFg
      GitChangeType.MODIFIED -> palette.modifiedFg
      GitChangeType.DELETED -> palette.deletedFg
    }
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      change.type.badge,
      color = badgeColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(18.dp),
    )
    Text(
      change.path,
      color = palette.textPrimary,
      fontSize = 12.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    IconButton(onClick = onAction, modifier = Modifier.size(30.dp)) {
      Icon(
        actionIcon,
        contentDescription = actionDesc,
        tint = palette.textPrimary,
        modifier = Modifier.size(15.dp),
      )
    }
  }
}

@Composable
private fun CloneRepoDialog(
  hasSavedToken: Boolean,
  onConfirm: (url: String, token: String, saveToken: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var url by remember { mutableStateOf("") }
  var token by remember { mutableStateOf("") }
  var saveToken by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Clone HTTPS Repository") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("HTTPS Git URL") },
          placeholder = { Text("https://github.com/user/repo.git") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("git_clone_url_input"),
        )
        OutlinedTextField(
          value = token,
          onValueChange = { token = it },
          label = {
            Text(
              if (hasSavedToken) "Token (leave blank to reuse saved)"
              else "Personal Access Token (optional)"
            )
          },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("git_clone_token_input"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = saveToken, onCheckedChange = { saveToken = it })
          Text("Save token in Android Keystore", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = url.trim().startsWith("https://"),
        onClick = { onConfirm(url.trim(), token.trim(), saveToken) },
        modifier = Modifier.testTag("git_clone_confirm_button"),
      ) {
        Text("Clone")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun GitDiffDialog(
  change: GitFileChange,
  lines: List<GitDiffLine>,
  onDismiss: () -> Unit,
) {
  val palette = LocalIdePalette.current
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(12.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(palette.editorBg)
          .border(1.dp, palette.border, RoundedCornerShape(8.dp)),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.activityBarBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            change.path,
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            if (change.staged) "Staged Diff (HEAD → Index)" else "Working Tree Diff",
            color = palette.textMuted,
            fontSize = 11.sp,
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("git_diff_close_button")) {
          Icon(Icons.Default.Close, contentDescription = "Close Diff", tint = palette.textPrimary)
        }
      }

      HorizontalDivider(color = palette.border, thickness = 0.5.dp)

      LazyColumn(
        modifier =
          Modifier.fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
      ) {
        items(lines.size) { index ->
          val line = lines[index]
          val rowBg =
            when (line.type) {
              DiffLineType.ADD -> if (palette.isDark) Color(0xFF1E3A24) else Color(0xFFE6FFEC)
              DiffLineType.DELETE -> if (palette.isDark) Color(0xFF3E1F1F) else Color(0xFFFFEBE9)
              DiffLineType.HUNK -> if (palette.isDark) Color(0xFF1F3044) else Color(0xFFDDF4FF)
              DiffLineType.HEADER -> palette.sidebarBg
              DiffLineType.CONTEXT -> palette.editorBg
            }
          val textColor =
            when (line.type) {
              DiffLineType.ADD -> palette.addedFg
              DiffLineType.DELETE -> palette.deletedFg
              DiffLineType.HUNK -> palette.accent
              DiffLineType.HEADER -> palette.textMuted
              DiffLineType.CONTEXT -> palette.textPrimary
            }
          val prefix =
            when (line.type) {
              DiffLineType.ADD -> "+"
              DiffLineType.DELETE -> "-"
              DiffLineType.CONTEXT -> " "
              else -> ""
            }
          Row(
            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 8.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = line.oldLine?.toString()?.padStart(4) ?: "    ",
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = line.newLine?.toString()?.padStart(4) ?: "    ",
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "$prefix${line.text}",
              color = textColor,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 12.sp,
            )
          }
        }
      }
    }
  }
}
