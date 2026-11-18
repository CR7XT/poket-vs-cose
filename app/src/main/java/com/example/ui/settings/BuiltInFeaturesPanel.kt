package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.EditorTab
import com.example.ui.theme.LocalIdePalette

@Composable
fun BuiltInFeaturesPanel(
  activeTab: EditorTab?,
  formatterEnabled: Boolean,
  snippetsEnabled: Boolean,
  markdownPreviewEnabled: Boolean,
  isMarkdownPreviewActive: Boolean,
  onToggleFormatter: (Boolean) -> Unit,
  onToggleSnippets: (Boolean) -> Unit,
  onToggleMarkdownPreview: (Boolean) -> Unit,
  onFormatActiveFile: () -> Unit,
  onOpenSnippetsPicker: () -> Unit,
  onToggleMarkdownPreviewPane: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current

  Column(
    modifier = modifier.fillMaxSize().background(palette.sidebarBg),
  ) {
    Text(
      text = "BUILT-IN FEATURES",
      color = palette.textMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
    HorizontalDivider(color = palette.border)

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.accent.copy(alpha = 0.14f))
            .border(0.5.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(10.dp)
            .testTag("built_in_disclaimer_banner"),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          Icons.Default.Info,
          contentDescription = null,
          tint = palette.accent,
          modifier = Modifier.size(16.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text =
            "These are built-in PocketCode toggles bundled in the app, NOT VS Code Marketplace extensions. No external extension marketplace or remote code is downloaded.",
          color = palette.textPrimary,
          fontSize = 11.sp,
          lineHeight = 15.sp,
        )
      }

      FeatureToggleCard(
        icon = Icons.Default.AutoFixHigh,
        title = "Code Formatter",
        description =
          "Formats JSON (strict pretty-print) and normalizes indentation/trailing whitespace for Python, JS/TS, HTML, CSS, Bash, and Markdown.",
        checked = formatterEnabled,
        onCheckedChange = onToggleFormatter,
        switchTag = "toggle_formatter_switch",
        actionContent = {
          Button(
            onClick = onFormatActiveFile,
            enabled = formatterEnabled && activeTab != null,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            modifier = Modifier.fillMaxWidth().testTag("format_active_file_button"),
          ) {
            Text(
              text = activeTab?.let { "Format ${it.name}" } ?: "Open a file to format",
              color = Color.White,
              fontSize = 12.sp,
            )
          }
        },
      )

      FeatureToggleCard(
        icon = Icons.Default.Code,
        title = "Code Snippets",
        description =
          "Provides starter templates for Python, JS/TS async fetch, HTML5 boilerplate, Bash safe scripts, and Markdown blocks.",
        checked = snippetsEnabled,
        onCheckedChange = onToggleSnippets,
        switchTag = "toggle_snippets_switch",
        actionContent = {
          OutlinedButton(
            onClick = onOpenSnippetsPicker,
            enabled = snippetsEnabled && activeTab != null,
            modifier = Modifier.fillMaxWidth().testTag("open_snippets_picker_button"),
          ) {
            Text("Insert Snippet at Cursor", fontSize = 12.sp)
          }
        },
      )

      FeatureToggleCard(
        icon = Icons.Default.Visibility,
        title = "Markdown Live Preview",
        description =
          "Renders headings, lists, code fences, blockquotes, and inline formatting for .md files without leaving the editor.",
        checked = markdownPreviewEnabled,
        onCheckedChange = onToggleMarkdownPreview,
        switchTag = "toggle_markdown_preview_switch",
        actionContent = {
          val isMd = activeTab?.language == CodeLanguage.MARKDOWN
          OutlinedButton(
            onClick = onToggleMarkdownPreviewPane,
            enabled = markdownPreviewEnabled && isMd,
            modifier = Modifier.fillMaxWidth().testTag("toggle_md_preview_action_button"),
          ) {
            Text(
              text =
                when {
                  !isMd -> "Open a .md file to preview"
                  isMarkdownPreviewActive -> "Switch Back to Code"
                  else -> "Preview ${activeTab.name}"
                },
              fontSize = 12.sp,
            )
          }
        },
      )
    }
  }
}

@Composable
private fun FeatureToggleCard(
  icon: ImageVector,
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  switchTag: String,
  actionContent: @Composable () -> Unit,
) {
  val palette = LocalIdePalette.current
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(palette.inputBg.copy(alpha = 0.45f))
        .border(0.5.dp, palette.border, RoundedCornerShape(8.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = title,
          color = palette.textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(switchTag),
      )
    }
    Text(
      text = description,
      color = palette.textMuted,
      fontSize = 11.sp,
      lineHeight = 16.sp,
    )
    actionContent()
  }
}
