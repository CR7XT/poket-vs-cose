package com.example.data.workspace

import androidx.compose.ui.graphics.Color

enum class CodeLanguage(
  val displayName: String,
  val badge: String,
  val badgeColor: Color,
  val scopeName: String?,
) {
  PYTHON("Python", "PY", Color(0xFF3572A5), "source.python"),
  JAVASCRIPT("JavaScript", "JS", Color(0xFFF1E05A), "source.js"),
  TYPESCRIPT("TypeScript", "TS", Color(0xFF3178C6), "source.ts"),
  JSON("JSON", "{}", Color(0xFFCBCB41), "source.json"),
  HTML("HTML", "<>", Color(0xFFE34C26), "text.html.basic"),
  CSS("CSS", "#", Color(0xFF563D7C), "source.css"),
  MARKDOWN("Markdown", "MD", Color(0xFF519ABA), "text.html.markdown"),
  BASH("Bash", "\$_", Color(0xFF89E051), "source.shell"),
  PLAIN("Plain Text", "TXT", Color(0xFF858585), null);

  companion object {
    fun fromFileName(name: String): CodeLanguage {
      val ext = name.substringAfterLast('.', "").lowercase()
      return when (ext) {
        "py", "pyw" -> PYTHON
        "js", "mjs", "cjs", "jsx" -> JAVASCRIPT
        "ts", "tsx" -> TYPESCRIPT
        "json", "jsonc" -> JSON
        "html", "htm" -> HTML
        "css", "scss" -> CSS
        "md", "markdown" -> MARKDOWN
        "sh", "bash", "zsh", "ps1", "bat", "cmd" -> BASH
        else -> PLAIN
      }
    }
  }
}

data class FileNode(
  val relativePath: String,
  val name: String,
  val isDirectory: Boolean,
  val sizeBytes: Long,
  val lastModified: Long,
  val children: List<FileNode> = emptyList(),
  val documentId: String? = null,
) {
  val isPhoneDocument: Boolean
    get() = documentId != null
}

data class EditorTab(
  val relativePath: String,
  val name: String,
  val language: CodeLanguage,
  val savedContent: String,
  val currentContent: String,
  val cursorLine: Int = 1,
  val cursorColumn: Int = 1,
  val selectedText: String = "",
  val phoneTreeUri: String? = null,
  val phoneDocumentId: String? = null,
) {
  val isDirty: Boolean
    get() = savedContent != currentContent

  val isPhoneFile: Boolean
    get() = phoneTreeUri != null && phoneDocumentId != null
}

data class SearchMatchLine(
  val lineNumber: Int,
  val preview: String,
)

data class SearchFileResult(
  val relativePath: String,
  val fileName: String,
  val matches: List<SearchMatchLine>,
)
