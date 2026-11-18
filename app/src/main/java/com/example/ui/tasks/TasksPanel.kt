package com.example.ui.tasks

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tasks.IdeTask
import com.example.data.tasks.TaskGroup
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun TasksPanel(
  tasks: List<IdeTask>,
  activeFileRelativePath: String?,
  onRunTask: (IdeTask) -> Unit,
  onAddTask: (label: String, command: String, group: TaskGroup, cwd: String, isDefault: Boolean) -> Unit,
  onDeleteTask: (String) -> Unit,
  onOpenTasksJson: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  var showAddDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize().background(palette.sidebarBg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        "VS CODE TASKS",
        color = palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onOpenTasksJson,
          modifier = Modifier.size(32.dp).testTag("tasks_open_json_button"),
        ) {
          Icon(
            Icons.Default.Code,
            contentDescription = "Open .vscode/tasks.json",
            tint = palette.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
        IconButton(
          onClick = { showAddDialog = true },
          modifier = Modifier.size(32.dp).testTag("tasks_add_button"),
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = "Add Task",
            tint = palette.accent,
            modifier = Modifier.size(19.dp),
          )
        }
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        "Run workspace scripts in the integrated terminal using VS Code .vscode/tasks.json definitions.",
        color = palette.textMuted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
          modifier = Modifier.weight(1f).testTag("tasks_primary_add_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Add Task", color = Color.White, fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = onOpenTasksJson,
          modifier = Modifier.weight(1f),
        ) {
          Text("tasks.json", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
        }
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    if (tasks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(
          "No tasks configured yet. Tap 'Add Task' above.",
          color = palette.textMuted,
          fontSize = 12.sp,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(tasks, key = { it.id }) { task ->
          val badgeColor =
            when (task.group) {
              TaskGroup.BUILD -> palette.accent
              TaskGroup.TEST -> palette.modifiedFg
              TaskGroup.RUN -> palette.addedFg
              TaskGroup.NONE -> palette.textMuted
            }
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.inputBg.copy(alpha = 0.45f))
                .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                .padding(10.dp)
                .testTag("task_card_${task.label}"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                  text = task.group.badge,
                  color = badgeColor,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = task.label,
                  color = palette.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (task.isDefault) {
                  Spacer(Modifier.width(6.dp))
                  Text(
                    "DEFAULT",
                    color = palette.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { onRunTask(task) },
                  modifier = Modifier.size(32.dp).testTag("run_task_${task.label}"),
                ) {
                  Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Run ${task.label}",
                    tint = palette.addedFg,
                    modifier = Modifier.size(20.dp),
                  )
                }
                IconButton(
                  onClick = { onDeleteTask(task.id) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete ${task.label}",
                    tint = palette.textMuted,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
            Text(
              text = task.command,
              color = palette.textMuted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (task.cwd.isNotBlank()) {
              Text(
                text = "cwd: ${task.cwd}",
                color = palette.textMuted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddTaskDialog(
      activeFileRelativePath = activeFileRelativePath,
      onConfirm = { label, command, group, cwd, isDefault ->
        onAddTask(label, command, group, cwd, isDefault)
        showAddDialog = false
      },
      onDismiss = { showAddDialog = false },
    )
  }
}

@Composable
fun AddTaskDialog(
  activeFileRelativePath: String?,
  onConfirm: (label: String, command: String, group: TaskGroup, cwd: String, isDefault: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalIdePalette.current
  var label by remember { mutableStateOf("") }
  var command by remember { mutableStateOf("") }
  var cwd by remember { mutableStateOf("") }
  var group by remember { mutableStateOf(TaskGroup.RUN) }
  var isDefault by remember { mutableStateOf(false) }

  val presets =
    listOf(
      Triple("Run Active File", "sh \"${activeFileRelativePath ?: "scripts/hello.sh"}\"", TaskGroup.RUN),
      Triple("Run Python", "python3 ${activeFileRelativePath?.takeIf { it.endsWith(".py") } ?: "main.py"}", TaskGroup.RUN),
      Triple("Run Node", "node ${activeFileRelativePath?.takeIf { it.endsWith(".js") } ?: "app.js"}", TaskGroup.RUN),
      Triple("Git Status", "git status", TaskGroup.BUILD),
      Triple("List Files (dir)", "dir", TaskGroup.NONE),
    )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add VS Code Task") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick Presets:", color = palette.textMuted, fontSize = 11.sp)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          presets.forEach { (pLabel, pCmd, pGroup) ->
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(palette.inputBg)
                  .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                  .clickable {
                    label = pLabel
                    command = pCmd
                    group = pGroup
                  }
                  .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text(pLabel, color = palette.textPrimary, fontSize = 11.sp)
            }
          }
        }

        OutlinedTextField(
          value = label,
          onValueChange = { label = it },
          label = { Text("Task Label (e.g. Build App)", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("add_task_label_input"),
        )
        OutlinedTextField(
          value = command,
          onValueChange = { command = it },
          label = { Text("Shell Command (e.g. python3 main.py)", fontSize = 12.sp) },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("add_task_command_input"),
        )
        OutlinedTextField(
          value = cwd,
          onValueChange = { cwd = it },
          label = { Text("Working Subfolder (optional)", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Text("Task Group:", color = palette.textMuted, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TaskGroup.entries.forEach { item ->
            val selected = item == group
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(if (selected) palette.accent else palette.inputBg)
                  .clickable { group = item }
                  .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
              Text(
                item.badge,
                color = if (selected) Color.White else palette.textPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
          Text("Set as default build/run task (Ctrl+Shift+B)", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = label.isNotBlank() && command.isNotBlank(),
        onClick = { onConfirm(label, command, group, cwd, isDefault) },
        modifier = Modifier.testTag("add_task_confirm_button"),
      ) {
        Text("Save to tasks.json")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
