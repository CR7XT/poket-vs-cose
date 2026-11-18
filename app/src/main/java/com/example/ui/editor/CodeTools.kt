package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.workspace.CodeLanguage
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object CodeFormatter {

  fun format(content: String, language: CodeLanguage, tabWidth: Int): Result<String> = runCatching {
    val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
    when (language) {
      CodeLanguage.JSON -> {
        val trimmed = normalized.trim()
        if (trimmed.isEmpty()) return@runCatching ""
        val parsed = JSONTokener(trimmed).nextValue()
        when (parsed) {
          is JSONObject -> parsed.toString(tabWidth) + "\n"
          is JSONArray -> parsed.toString(tabWidth) + "\n"
          else -> throw IllegalArgumentException("JSON root must be an object or array")
        }
      }
      CodeLanguage.MARKDOWN -> {
        val lines = normalized.lines()
        val out = mutableListOf<String>()
        var blankRun = 0
        for (line in lines) {
          // Preserve intentional two-space Markdown line breaks
          val cleaned =
            if (line.endsWith("  ") && line.trim().isNotEmpty()) {
              line.trimEnd() + "  "
            } else {
              line.trimEnd()
            }
          if (cleaned.isEmpty()) {
            blankRun++
            if (blankRun <= 2) out.add("")
          } else {
            blankRun = 0
            out.add(cleaned)
          }
        }
        out.joinToString("\n").trimEnd('\n') + if (normalized.isNotBlank()) "\n" else ""
      }
      CodeLanguage.PYTHON,
      CodeLanguage.BASH,
      CodeLanguage.PLAIN -> {
        val lines = normalized.lines().map { it.trimEnd() }
        val result = lines.joinToString("\n").trimEnd('\n')
        if (result.isEmpty()) "" else "$result\n"
      }
      CodeLanguage.JAVASCRIPT,
      CodeLanguage.TYPESCRIPT,
      CodeLanguage.CSS,
      CodeLanguage.HTML -> {
        formatBraceOrTagLines(normalized, tabWidth, isHtml = language == CodeLanguage.HTML)
      }
    }
  }

  private fun formatBraceOrTagLines(input: String, tabWidth: Int, isHtml: Boolean): String {
    val indentUnit = " ".repeat(tabWidth.coerceIn(2, 4))
    val lines = input.lines()
    val out = mutableListOf<String>()
    var depth = 0
    for (rawLine in lines) {
      val trimmed = rawLine.trim()
      if (trimmed.isEmpty()) {
        if (out.lastOrNull()?.isNotEmpty() == true) out.add("")
        continue
      }
      if (!isHtml) {
        val startsClosing = trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")
        if (startsClosing) depth = (depth - 1).coerceAtLeast(0)
        out.add(indentUnit.repeat(depth) + trimmed)
        val opens = trimmed.count { it == '{' }
        val closes = trimmed.count { it == '}' }
        val delta = opens - closes
        depth =
          if (startsClosing) {
            (depth + delta + 1).coerceAtLeast(0)
          } else {
            (depth + delta).coerceAtLeast(0)
          }
      } else {
        val isClosingTag = trimmed.startsWith("</")
        if (isClosingTag) depth = (depth - 1).coerceAtLeast(0)
        out.add(indentUnit.repeat(depth) + trimmed)
        val isSelfClosing =
          trimmed.endsWith("/>") ||
            trimmed.startsWith("<!") ||
            trimmed.startsWith("<?") ||
            trimmed.contains("</")
        val isOpeningTag = trimmed.startsWith("<") && !isClosingTag && !isSelfClosing
        if (isOpeningTag) depth++
      }
    }
    val joined = out.joinToString("\n").trimEnd('\n')
    return if (joined.isEmpty()) "" else "$joined\n"
  }
}

data class CodeSnippet(
  val id: String,
  val title: String,
  val languageHint: String,
  val body: String,
)

val BuiltInCodeSnippets =
  listOf(
    CodeSnippet(
      id = "py_main",
      title = "Python Main Guard",
      languageHint = "PY",
      body =
        """
        def main() -> None:
            print("Hello from PocketCode")


        if __name__ == "__main__":
            main()
        """.trimIndent(),
    ),
    CodeSnippet(
      id = "py_dataclass",
      title = "Python Dataclass",
      languageHint = "PY",
      body =
        """
        from dataclasses import dataclass

        @dataclass
        class Item:
            name: str
            value: int = 0
        """.trimIndent(),
    ),
    CodeSnippet(
      id = "js_async",
      title = "JS/TS Async Try-Catch",
      languageHint = "JS/TS",
      body =
        """
        async function fetchData(url) {
          try {
            const response = await fetch(url);
            if (!response.ok) throw new Error(`HTTP ${'$'}{response.status}`);
            return await response.json();
          } catch (err) {
            console.error(err);
            return null;
          }
        }
        """.trimIndent(),
    ),
    CodeSnippet(
      id = "html_doc",
      title = "HTML5 Starter",
      languageHint = "HTML",
      body =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Document</title>
        </head>
        <body>
          <h1>Hello PocketCode</h1>
        </body>
        </html>
        """.trimIndent(),
    ),
    CodeSnippet(
      id = "sh_script",
      title = "Bash Safe Script",
      languageHint = "SH",
      body =
        """
        #!/usr/bin/env sh
        set -eu

        echo "Running in: $(pwd)"
        """.trimIndent(),
    ),
    CodeSnippet(
      id = "md_section",
      title = "Markdown Section + Code Block",
      languageHint = "MD",
      body =
        """
        ## Section Title

        - Key point one
        - Key point two

        ```python
        print("example")
        ```
        """.trimIndent(),
    ),
  )

@Composable
fun SnippetPickerDialog(
  onInsertSnippet: (CodeSnippet) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalIdePalette.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Insert Built-in Snippet") },
    text = {
      Column {
        Text(
          "Built-in PocketCode snippets (not Marketplace extensions). Tap to insert at cursor.",
          color = palette.textMuted,
          fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(BuiltInCodeSnippets, key = { it.id }) { snippet ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(palette.inputBg)
                  .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                  .clickable {
                    onInsertSnippet(snippet)
                    onDismiss()
                  }
                  .padding(10.dp)
                  .testTag("snippet_item_${snippet.id}")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  snippet.title,
                  color = palette.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  snippet.languageHint,
                  color = palette.accent,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
              Spacer(Modifier.height(4.dp))
              Text(
                snippet.body.lines().take(2).joinToString("  "),
                color = palette.textMuted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun MarkdownPreviewPane(
  markdown: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val blocks = parseMarkdownBlocks(markdown)

  Column(
    modifier =
      modifier
        .background(palette.editorBg)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag("markdown_preview_pane"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (blocks.isEmpty()) {
      Text("Empty Markdown document", color = palette.textMuted, fontSize = 13.sp)
    }
    blocks.forEach { block ->
      when (block) {
        is MdBlock.Heading -> {
          val size =
            when (block.level) {
              1 -> 22.sp
              2 -> 18.sp
              else -> 15.sp
            }
          Text(
            text = renderInlineMarkdown(block.text, palette.accent, palette.inputBg),
            color = palette.textPrimary,
            fontSize = size,
            fontWeight = FontWeight.Bold,
          )
          if (block.level <= 2) {
            HorizontalDivider(color = palette.border)
          }
        }
        is MdBlock.CodeFence -> {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(palette.sidebarBg)
                .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                .padding(10.dp)
          ) {
            if (block.lang.isNotEmpty()) {
              Text(
                block.lang,
                color = palette.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              )
              Spacer(Modifier.height(4.dp))
            }
            Text(
              text = block.code,
              color = palette.textPrimary,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp,
            )
          }
        }
        is MdBlock.Quote -> {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(palette.sidebarBg.copy(alpha = 0.65f))
                .padding(vertical = 6.dp, horizontal = 8.dp)
          ) {
            Box(modifier = Modifier.width(3.dp).height(18.dp).background(palette.accent))
            Spacer(Modifier.width(10.dp))
            Text(
              text = renderInlineMarkdown(block.text, palette.accent, palette.inputBg),
              color = palette.textMuted,
              fontSize = 13.sp,
            )
          }
        }
        is MdBlock.ListItem -> {
          Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
            Text(
              text = "${block.marker} ",
              color = palette.accent,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            )
            Text(
              text = renderInlineMarkdown(block.text, palette.accent, palette.inputBg),
              color = palette.textPrimary,
              fontSize = 13.sp,
              lineHeight = 19.sp,
            )
          }
        }
        is MdBlock.Divider -> {
          HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
        }
        is MdBlock.Paragraph -> {
          Text(
            text = renderInlineMarkdown(block.text, palette.accent, palette.inputBg),
            color = palette.textPrimary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
          )
        }
      }
    }
  }
}

private sealed interface MdBlock {
  data class Heading(val level: Int, val text: String) : MdBlock

  data class CodeFence(val lang: String, val code: String) : MdBlock

  data class Quote(val text: String) : MdBlock

  data class ListItem(val marker: String, val text: String) : MdBlock

  data object Divider : MdBlock

  data class Paragraph(val text: String) : MdBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
  val lines = markdown.replace("\r\n", "\n").lines()
  val result = mutableListOf<MdBlock>()
  var i = 0
  while (i < lines.size) {
    val line = lines[i]
    val trimmed = line.trim()
    when {
      trimmed.isEmpty() -> i++
      trimmed.startsWith("```") -> {
        val lang = trimmed.removePrefix("```").trim()
        val codeLines = mutableListOf<String>()
        i++
        while (i < lines.size && !lines[i].trim().startsWith("```")) {
          codeLines.add(lines[i])
          i++
        }
        if (i < lines.size) i++
        result.add(MdBlock.CodeFence(lang, codeLines.joinToString("\n")))
      }
      trimmed == "---" || trimmed == "***" -> {
        result.add(MdBlock.Divider)
        i++
      }
      trimmed.startsWith("### ") -> {
        result.add(MdBlock.Heading(3, trimmed.removePrefix("### ").trim()))
        i++
      }
      trimmed.startsWith("## ") -> {
        result.add(MdBlock.Heading(2, trimmed.removePrefix("## ").trim()))
        i++
      }
      trimmed.startsWith("# ") -> {
        result.add(MdBlock.Heading(1, trimmed.removePrefix("# ").trim()))
        i++
      }
      trimmed.startsWith("> ") -> {
        result.add(MdBlock.Quote(trimmed.removePrefix("> ").trim()))
        i++
      }
      trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
        result.add(MdBlock.ListItem("•", trimmed.drop(2).trim()))
        i++
      }
      trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
        val dotIdx = trimmed.indexOf('.')
        val num = trimmed.substring(0, dotIdx + 1)
        result.add(MdBlock.ListItem(num, trimmed.substring(dotIdx + 1).trim()))
        i++
      }
      else -> {
        val para = mutableListOf(trimmed)
        i++
        while (i < lines.size) {
          val next = lines[i].trim()
          if (
            next.isEmpty() ||
              next.startsWith("#") ||
              next.startsWith("```") ||
              next.startsWith("> ") ||
              next.startsWith("- ") ||
              next.startsWith("* ") ||
              next == "---"
          ) {
            break
          }
          para.add(next)
          i++
        }
        result.add(MdBlock.Paragraph(para.joinToString(" ")))
      }
    }
  }
  return result
}

private fun renderInlineMarkdown(
  text: String,
  accentColor: androidx.compose.ui.graphics.Color,
  codeBg: androidx.compose.ui.graphics.Color,
): AnnotatedString = buildAnnotatedString {
  var idx = 0
  while (idx < text.length) {
    when {
      text.startsWith("**", idx) -> {
        val end = text.indexOf("**", idx + 2)
        if (end > idx + 2) {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(idx + 2, end))
          }
          idx = end + 2
        } else {
          append(text[idx])
          idx++
        }
      }
      text[idx] == '`' -> {
        val end = text.indexOf('`', idx + 1)
        if (end > idx + 1) {
          withStyle(
            SpanStyle(
              fontFamily = JetBrainsMonoFontFamily,
              background = codeBg,
              color = accentColor,
            )
          ) {
            append(" ${text.substring(idx + 1, end)} ")
          }
          idx = end + 1
        } else {
          append(text[idx])
          idx++
        }
      }
      text[idx] == '*' -> {
        val end = text.indexOf('*', idx + 1)
        if (end > idx + 1) {
          withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text.substring(idx + 1, end))
          }
          idx = end + 1
        } else {
          append(text[idx])
          idx++
        }
      }
      text[idx] == '[' -> {
        val closeBracket = text.indexOf(']', idx + 1)
        val openParen = if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') closeBracket + 1 else -1
        val closeParen = if (openParen != -1) text.indexOf(')', openParen + 1) else -1
        if (closeBracket > idx && closeParen > openParen) {
          val label = text.substring(idx + 1, closeBracket)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Medium)) {
            append(label)
          }
          idx = closeParen + 1
        } else {
          append(text[idx])
          idx++
        }
      }
      else -> {
        append(text[idx])
        idx++
      }
    }
  }
}
