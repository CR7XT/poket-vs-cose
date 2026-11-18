package com.example.data.tasks

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class TaskGroup(val jsonValue: String, val badge: String) {
  BUILD("build", "BUILD"),
  TEST("test", "TEST"),
  RUN("run", "RUN"),
  NONE("none", "TASK");

  companion object {
    fun fromJson(raw: String?): TaskGroup =
      entries.firstOrNull { it.jsonValue.equals(raw, ignoreCase = true) } ?: NONE
  }
}

data class IdeTask(
  val id: String,
  val label: String,
  val command: String,
  val type: String = "shell",
  val group: TaskGroup = TaskGroup.NONE,
  val cwd: String = "",
  val isDefault: Boolean = false,
)

class TaskRepository(private val workspaceRoot: File) {

  private val tasksFile: File
    get() = File(workspaceRoot, ".vscode/tasks.json")

  suspend fun loadTasks(): List<IdeTask> =
    withContext(Dispatchers.IO) {
      ensureStarterTasksFile()
      if (!tasksFile.exists()) return@withContext emptyList()
      runCatching {
        val raw = tasksFile.readText(Charsets.UTF_8)
        val root = JSONObject(raw)
        val arr = root.optJSONArray("tasks") ?: JSONArray()
        buildList {
          for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val label = obj.optString("label").trim()
            val command = obj.optString("command").trim()
            if (label.isEmpty() || command.isEmpty()) continue
            val groupField = obj.opt("group")
            val (groupName, isDefault) =
              when (groupField) {
                is JSONObject ->
                  groupField.optString("kind", "none") to groupField.optBoolean("isDefault", false)
                is String -> groupField to false
                else -> "none" to false
              }
            val optionsObj = obj.optJSONObject("options")
            val cwd = optionsObj?.optString("cwd").orEmpty().removePrefix("\${workspaceFolder}/").removePrefix("\${workspaceFolder}")
            add(
              IdeTask(
                id = "${label}_$i",
                label = label,
                command = command,
                type = obj.optString("type", "shell").ifBlank { "shell" },
                group = TaskGroup.fromJson(groupName),
                cwd = cwd,
                isDefault = isDefault,
              )
            )
          }
        }
      }.getOrDefault(emptyList())
    }

  suspend fun saveTasks(tasks: List<IdeTask>): Unit =
    withContext(Dispatchers.IO) {
      val dir = tasksFile.parentFile
      if (dir != null && !dir.exists()) dir.mkdirs()
      val root = JSONObject().apply { put("version", "2.0.0") }
      val arr = JSONArray()
      tasks.forEach { task ->
        val obj =
          JSONObject().apply {
            put("label", task.label.trim())
            put("type", task.type.ifBlank { "shell" })
            put("command", task.command.trim())
            if (task.isDefault) {
              put(
                "group",
                JSONObject().apply {
                  put("kind", task.group.jsonValue)
                  put("isDefault", true)
                },
              )
            } else {
              put("group", task.group.jsonValue)
            }
            if (task.cwd.isNotBlank()) {
              put(
                "options",
                JSONObject().apply { put("cwd", task.cwd.trim()) },
              )
            }
            put("problemMatcher", JSONArray())
          }
        arr.put(obj)
      }
      root.put("tasks", arr)
      tasksFile.writeText(root.toString(2) + "\n", Charsets.UTF_8)
    }

  suspend fun addTask(
    label: String,
    command: String,
    group: TaskGroup,
    cwd: String = "",
    isDefault: Boolean = false,
  ): List<IdeTask> {
    val existing = loadTasks()
    val cleanedBase =
      if (isDefault) existing.map { it.copy(isDefault = false) } else existing
    val created =
      IdeTask(
        id = "${label.trim()}_${System.currentTimeMillis()}",
        label = label.trim(),
        command = command.trim(),
        group = group,
        cwd = cwd.trim(),
        isDefault = isDefault,
      )
    val updated = cleanedBase + created
    saveTasks(updated)
    return updated
  }

  suspend fun deleteTask(taskId: String): List<IdeTask> {
    val updated = loadTasks().filterNot { it.id == taskId }
    saveTasks(updated)
    return updated
  }

  fun buildExecutionCommand(task: IdeTask): String {
    val cleanCwd = task.cwd.trim().trim('/')
    return if (cleanCwd.isEmpty()) {
      task.command
    } else {
      val safeCwd = cleanCwd.replace("'", "'\\''")
      "cd '$safeCwd' && ${task.command}"
    }
  }

  private fun ensureStarterTasksFile() {
    if (tasksFile.exists()) return
    val dir = tasksFile.parentFile
    if (dir != null && !dir.exists()) dir.mkdirs()
    val starter =
      """
      {
        "version": "2.0.0",
        "tasks": [
          {
            "label": "Run Shell Demo",
            "type": "shell",
            "command": "sh scripts/hello.sh",
            "group": {
              "kind": "run",
              "isDefault": true
            },
            "problemMatcher": []
          },
          {
            "label": "Inspect Workspace Files",
            "type": "shell",
            "command": "ls -la && pwd",
            "group": "build",
            "problemMatcher": []
          },
          {
            "label": "Check Python / Node",
            "type": "shell",
            "command": "command -v python3 node claude gemini || echo 'Use Ubuntu proot or Termux WebSocket for Python/Node/AI CLIs'",
            "group": "test",
            "problemMatcher": []
          }
        ]
      }
      """
        .trimIndent() + "\n"
    tasksFile.writeText(starter, Charsets.UTF_8)
  }
}
