package com.example.data.workspace

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkspaceRepository(private val context: Context) {

  val workspaceRoot: File by lazy {
    File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() }
  }

  suspend fun ensureStarterFiles(): Unit =
    withContext(Dispatchers.IO) {
      val marker = File(context.filesDir, ".starter_initialized")
      if (!marker.exists() && (workspaceRoot.listFiles()?.isEmpty() != false)) {
        writeInternal("README.md", SAMPLE_README)
        writeInternal("main.py", SAMPLE_PYTHON)
        writeInternal("app.js", SAMPLE_JS)
        writeInternal("package.json", SAMPLE_JSON)
        writeInternal("scripts/hello.sh", SAMPLE_BASH)
        marker.createNewFile()
      }
    }

  suspend fun listTree(): List<FileNode> =
    withContext(Dispatchers.IO) {
      buildTree(workspaceRoot)
    }

  suspend fun listAllFilePaths(): List<String> =
    withContext(Dispatchers.IO) {
      val result = mutableListOf<String>()
      collectFilePaths(workspaceRoot, result)
      result.sorted()
    }

  suspend fun listAllFolderPaths(): List<String> =
    withContext(Dispatchers.IO) {
      val result = mutableListOf("")
      collectFolderPaths(workspaceRoot, result)
      result.sorted()
    }

  suspend fun readFile(relativePath: String): String =
    withContext(Dispatchers.IO) {
      val target = resolveSafe(relativePath)
      if (!target.exists() || target.isDirectory) {
        throw IOException("File not found: $relativePath")
      }
      target.readText(Charsets.UTF_8)
    }

  suspend fun writeFile(relativePath: String, content: String): Unit =
    withContext(Dispatchers.IO) {
      writeInternal(relativePath, content)
    }

  suspend fun createFile(parentRelativePath: String, fileName: String): String =
    withContext(Dispatchers.IO) {
      val cleanName = sanitizeName(fileName)
      val rel = if (parentRelativePath.isBlank()) cleanName else "$parentRelativePath/$cleanName"
      val target = resolveSafe(rel)
      if (target.exists()) throw IOException("Already exists: $rel")
      target.parentFile?.mkdirs()
      target.writeText("", Charsets.UTF_8)
      rel
    }

  suspend fun createFolder(parentRelativePath: String, folderName: String): String =
    withContext(Dispatchers.IO) {
      val cleanName = sanitizeName(folderName)
      val rel = if (parentRelativePath.isBlank()) cleanName else "$parentRelativePath/$cleanName"
      val target = resolveSafe(rel)
      if (target.exists()) throw IOException("Already exists: $rel")
      if (!target.mkdirs()) throw IOException("Unable to create folder: $rel")
      rel
    }

  suspend fun renameEntry(relativePath: String, newName: String): String =
    withContext(Dispatchers.IO) {
      val cleanNewName = sanitizeName(newName)
      val source = resolveSafe(relativePath)
      if (!source.exists()) throw IOException("Entry not found: $relativePath")
      val parentRel = relativePath.substringBeforeLast('/', "")
      val newRel = if (parentRel.isBlank()) cleanNewName else "$parentRel/$cleanNewName"
      val dest = resolveSafe(newRel)
      if (dest.exists()) throw IOException("Target already exists: $newRel")
      if (!source.renameTo(dest)) throw IOException("Failed to rename $relativePath")
      newRel
    }

  suspend fun moveEntry(relativePath: String, targetFolderRelativePath: String): String =
    withContext(Dispatchers.IO) {
      val source = resolveSafe(relativePath)
      if (!source.exists()) throw IOException("Entry not found: $relativePath")
      val name = source.name
      val newRel =
        if (targetFolderRelativePath.isBlank()) name else "$targetFolderRelativePath/$name"
      if (newRel == relativePath) return@withContext relativePath
      if (source.isDirectory && newRel.startsWith("$relativePath/")) {
        throw IOException("Cannot move a folder inside itself")
      }
      val dest = resolveSafe(newRel)
      if (dest.exists()) throw IOException("Destination already exists: $newRel")
      dest.parentFile?.mkdirs()
      if (!source.renameTo(dest)) throw IOException("Failed to move $relativePath")
      newRel
    }

  suspend fun deleteEntry(relativePath: String): Unit =
    withContext(Dispatchers.IO) {
      val target = resolveSafe(relativePath)
      if (!target.exists()) return@withContext
      if (!target.deleteRecursively()) throw IOException("Failed to delete $relativePath")
    }

  suspend fun searchAcrossFiles(query: String, matchCase: Boolean): List<SearchFileResult> =
    withContext(Dispatchers.IO) {
      if (query.isBlank()) return@withContext emptyList()
      val paths = mutableListOf<String>()
      collectFilePaths(workspaceRoot, paths)
      val results = mutableListOf<SearchFileResult>()
      for (rel in paths.sorted()) {
        val file = resolveSafe(rel)
        if (file.length() > MAX_SEARCH_FILE_BYTES) continue
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull() ?: continue
        val matches = mutableListOf<SearchMatchLine>()
        lines.forEachIndexed { idx, line ->
          if (line.contains(query, ignoreCase = !matchCase)) {
            matches += SearchMatchLine(lineNumber = idx + 1, preview = line.trim().take(140))
          }
        }
        if (matches.isNotEmpty()) {
          results += SearchFileResult(rel, file.name, matches)
        }
      }
      results
    }

  suspend fun importFiles(uris: List<Uri>): Int =
    withContext(Dispatchers.IO) {
      var count = 0
      for (uri in uris) {
        val name = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}.txt"
        val safeName = sanitizeName(name)
        val dest = resolveSafe(safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
          dest.outputStream().use { output -> input.copyTo(output) }
          count++
        }
      }
      count
    }

  suspend fun importZip(uri: Uri): Int =
    withContext(Dispatchers.IO) {
      val rootCanonical = workspaceRoot.canonicalFile
      val rootPrefix = rootCanonical.path + File.separator
      var extractedCount = 0
      val rawStream =
        context.contentResolver.openInputStream(uri)
          ?: throw IOException("Unable to open selected ZIP archive")
      ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
          val entryName = entry.name.replace('\\', '/')
          if (entryName.isNotBlank() && !entryName.startsWith("__MACOSX")) {
            val destFile = File(rootCanonical, entryName).canonicalFile
            // Strict Zip-Slip prevention
            if (!destFile.path.startsWith(rootPrefix) && destFile != rootCanonical) {
              throw SecurityException("Blocked unsafe ZIP entry path: $entryName")
            }
            if (entry.isDirectory) {
              destFile.mkdirs()
            } else {
              destFile.parentFile?.mkdirs()
              BufferedOutputStream(destFile.outputStream()).use { out ->
                zis.copyTo(out)
              }
              extractedCount++
            }
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
      extractedCount
    }

  suspend fun exportZip(uri: Uri): Int =
    withContext(Dispatchers.IO) {
      val outStream =
        context.contentResolver.openOutputStream(uri)
          ?: throw IOException("Unable to open destination ZIP file")
      var fileCount = 0
      ZipOutputStream(BufferedOutputStream(outStream)).use { zos ->
        fileCount = zipDirectoryRecursive(workspaceRoot, "", zos)
      }
      fileCount
    }

  private fun zipDirectoryRecursive(dir: File, prefix: String, zos: ZipOutputStream): Int {
    var count = 0
    val children = dir.listFiles()?.sortedBy { it.name.lowercase() } ?: return 0
    for (child in children) {
      if (child.name == ".git") continue
      val entryName = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
      if (child.isDirectory) {
        count += zipDirectoryRecursive(child, entryName, zos)
      } else {
        zos.putNextEntry(ZipEntry(entryName))
        child.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
        count++
      }
    }
    return count
  }

  private fun writeInternal(relativePath: String, content: String) {
    val target = resolveSafe(relativePath)
    target.parentFile?.mkdirs()
    target.writeText(content, Charsets.UTF_8)
  }

  private fun resolveSafe(relativePath: String): File {
    val rootCanonical = workspaceRoot.canonicalFile
    val candidate = File(rootCanonical, relativePath).canonicalFile
    val prefix = rootCanonical.path + File.separator
    if (candidate != rootCanonical && !candidate.path.startsWith(prefix)) {
      throw SecurityException("Invalid workspace path: $relativePath")
    }
    return candidate
  }

  private fun sanitizeName(name: String): String {
    val trimmed = name.trim().replace('/', '_').replace('\\', '_')
    require(trimmed.isNotEmpty() && trimmed != "." && trimmed != "..") {
      "Invalid file or folder name"
    }
    return trimmed
  }

  private fun buildTree(dir: File): List<FileNode> {
    val children = dir.listFiles() ?: return emptyList()
    return children
      .filter { it.name != ".git" }
      .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
      .map { file ->
        val rel = file.relativeTo(workspaceRoot).path.replace('\\', '/')
        FileNode(
          relativePath = rel,
          name = file.name,
          isDirectory = file.isDirectory,
          sizeBytes = if (file.isDirectory) 0L else file.length(),
          lastModified = file.lastModified(),
          children = if (file.isDirectory) buildTree(file) else emptyList(),
        )
      }
  }

  private fun collectFilePaths(dir: File, out: MutableList<String>) {
    val children = dir.listFiles() ?: return
    for (child in children) {
      if (child.name == ".git") continue
      if (child.isDirectory) {
        collectFilePaths(child, out)
      } else {
        out += child.relativeTo(workspaceRoot).path.replace('\\', '/')
      }
    }
  }

  private fun collectFolderPaths(dir: File, out: MutableList<String>) {
    val children = dir.listFiles() ?: return
    for (child in children) {
      if (child.isDirectory && child.name != ".git") {
        out += child.relativeTo(workspaceRoot).path.replace('\\', '/')
        collectFolderPaths(child, out)
      }
    }
  }

  private fun queryDisplayName(uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (index >= 0) return cursor.getString(index)
        }
      }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  companion object {
    private const val MAX_SEARCH_FILE_BYTES = 512 * 1024L

    private val SAMPLE_README =
      """
      # PocketCode Workspace

      Welcome to **PocketCode**, a mobile-first VS Code-style code editor on Android.

      ## Included Sample Files
      - `main.py` — Sample Python CLI utility
      - `app.js` — Sample JavaScript module
      - `package.json` — Project metadata JSON
      - `scripts/hello.sh` — Sample Bash script

      ## Quick Tips
      - Tap the **Command Center** at the top to fuzzy-search files or run `>` commands.
      - Pinch inside the editor to zoom font size cleanly.
      - Use the accessory bar above the keyboard for `Tab`, `Esc`, `Ctrl`, arrows, and brackets.
      """
        .trimIndent() + "\n"

    private val SAMPLE_PYTHON =
      """
      from dataclasses import dataclass
      from typing import List


      @dataclass
      class Task:
          title: str
          done: bool = False


      def summarize(tasks: List[Task]) -> str:
          completed = sum(1 for item in tasks if item.done)
          return f"{completed}/{len(tasks)} tasks complete"


      if __name__ == "__main__":
          sample = [
              Task("Configure Sora TextMate editor", done=True),
              Task("Initialize Git repository", done=False),
              Task("Launch terminal workspace", done=False),
          ]
          print("PocketCode:", summarize(sample))
      """
        .trimIndent() + "\n"

    private val SAMPLE_JS =
      """
      // Sample workspace script
      export function formatStatus(branch, dirtyCount) {
        const marker = dirtyCount > 0 ? `* (${'$'}{dirtyCount} unsaved)` : "";
        return `${'$'}{branch}${'$'}{marker}`;
      }

      const demo = formatStatus("main", 1);
      console.log("Workspace status:", demo);
      """
        .trimIndent() + "\n"

    private val SAMPLE_JSON =
      """
      {
        "name": "pocketcode-starter",
        "version": "1.0.0",
        "description": "Starter workspace for PocketCode Android IDE",
        "scripts": {
          "start": "node app.js",
          "python": "python3 main.py"
        }
      }
      """
        .trimIndent() + "\n"

    private val SAMPLE_BASH =
      """
      #!/usr/bin/env sh
      echo "Hello from PocketCode workspace!"
      pwd
      ls -la
      """
        .trimIndent() + "\n"
  }
}
