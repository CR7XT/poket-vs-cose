package com.example.ui.editor

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.workspace.CodeLanguage
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

data class PaletteCommand(
  val id: String,
  val label: String,
  val shortcutHint: String,
  val action: () -> Unit,
)

@Composable
fun CommandPaletteDialog(
  filePaths: List<String>,
  commands: List<PaletteCommand>,
  initialQuery: String = "",
  onOpenFile: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalIdePalette.current
  var query by remember(initialQuery) { mutableStateOf(initialQuery) }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  val isCommandMode = query.trimStart().startsWith(">")
  val filterText = if (isCommandMode) query.trimStart().drop(1).trim() else query.trim()

  val filteredCommands =
    remember(filterText, commands, isCommandMode) {
      if (!isCommandMode) emptyList()
      else {
        commands
          .mapNotNull { cmd ->
            val score = fuzzyScore(filterText, cmd.label)
            if (score >= 0) score to cmd else null
          }
          .sortedByDescending { it.first }
          .map { it.second }
      }
    }

  val filteredFiles =
    remember(filterText, filePaths, isCommandMode) {
      if (isCommandMode) emptyList()
      else {
        filePaths
          .mapNotNull { path ->
            val score = fuzzyScore(filterText, path)
            if (score >= 0) score to path else null
          }
          .sortedByDescending { it.first }
          .map { it.second }
      }
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f))
          .clickable(onClick = onDismiss)
          .padding(horizontal = 16.dp, vertical = 48.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(max = 460.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.sidebarBg)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .clickable(enabled = false) {},
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(palette.activityBarBg)
              .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.inputBg)
                .border(1.dp, palette.accent, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            if (query.isEmpty()) {
              Text(
                "Type file name to open, or '>' for commands",
                color = palette.textMuted,
                fontSize = 13.sp,
              )
            }
            BasicTextField(
              value = query,
              onValueChange = { query = it },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = palette.textPrimary,
                  fontSize = 13.sp,
                  fontFamily = JetBrainsMonoFontFamily,
                ),
              cursorBrush = SolidColor(palette.accent),
              modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).testTag("command_palette_input"),
            )
          }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
          if (isCommandMode) {
            if (filteredCommands.isEmpty()) {
              item {
                Text(
                  "No matching commands",
                  color = palette.textMuted,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(16.dp),
                )
              }
            } else {
              items(filteredCommands, key = { it.id }) { cmd ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        onDismiss()
                        cmd.action()
                      }
                      .padding(horizontal = 14.dp, vertical = 11.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = palette.accent,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      cmd.label,
                      color = palette.textPrimary,
                      fontSize = 13.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  if (cmd.shortcutHint.isNotEmpty()) {
                    Text(
                      cmd.shortcutHint,
                      color = palette.textMuted,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 11.sp,
                    )
                  }
                }
              }
            }
          } else {
            if (filteredFiles.isEmpty()) {
              item {
                Text(
                  "No matching files",
                  color = palette.textMuted,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(16.dp),
                )
              }
            } else {
              items(filteredFiles, key = { it }) { path ->
                val fileName = path.substringAfterLast('/')
                val lang = CodeLanguage.fromFileName(fileName)
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        onOpenFile(path)
                        onDismiss()
                      }
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    lang.badge,
                    color = lang.badgeColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      fileName,
                      color = palette.textPrimary,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (path.contains('/')) {
                      Text(
                        path.substringBeforeLast('/'),
                        color = palette.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun fuzzyScore(pattern: String, target: String): Int {
  if (pattern.isEmpty()) return 1
  val p = pattern.lowercase()
  val t = target.lowercase()
  var pIdx = 0
  var score = 0
  var consecutive = 0
  for (tIdx in t.indices) {
    if (pIdx < p.length && t[tIdx] == p[pIdx]) {
      score += 10 + consecutive * 4
      if (tIdx == 0 || t[tIdx - 1] == '/' || t[tIdx - 1] == '_' || t[tIdx - 1] == ' ') {
        score += 8
      }
      consecutive++
      pIdx++
    } else {
      consecutive = 0
    }
  }
  return if (pIdx == p.length) score else -1
}
