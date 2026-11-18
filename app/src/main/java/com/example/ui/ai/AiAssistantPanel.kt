package com.example.ui.ai

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiChatMessage
import com.example.data.workspace.EditorTab
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun AiAssistantPanel(
  activeTab: EditorTab?,
  hasGeminiApiKey: Boolean,
  messages: List<AiChatMessage>,
  isLoading: Boolean,
  errorMessage: String?,
  onOpenClaudeCode: () -> Unit,
  onOpenGeminiCli: () -> Unit,
  onOpenSettings: () -> Unit,
  onRunCodeAction: (action: String) -> Unit,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
  onInsertCode: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val clipboard = LocalClipboardManager.current
  var input by remember { mutableStateOf("") }

  Column(modifier = modifier.fillMaxSize().background(palette.sidebarBg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        "AI ASSISTANT & CLIs",
        color = palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      )
      if (messages.isNotEmpty()) {
        IconButton(onClick = onClearChat, modifier = Modifier.size(30.dp)) {
          Icon(
            Icons.Default.DeleteOutline,
            contentDescription = "Clear AI Chat",
            tint = palette.textMuted,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    // CLI Launchers (work with CLI's own browser login; no app API key required)
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        "Terminal AI CLIs (browser OAuth inside CLI)",
        color = palette.textPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Button(
        onClick = onOpenClaudeCode,
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        modifier = Modifier.fillMaxWidth().testTag("open_claude_code_button"),
      ) {
        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Open Claude Code")
      }
      OutlinedButton(
        onClick = onOpenGeminiCli,
        modifier = Modifier.fillMaxWidth().testTag("open_gemini_cli_button"),
      ) {
        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Open Gemini CLI")
      }
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    if (!hasGeminiApiKey) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.editorBg)
            .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = palette.modifiedFg, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(
            "In-App Gemini Chat Disabled",
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Text(
          "No API key is required to use PocketCode or launch Claude Code / Gemini CLI. " +
            "To enable optional Explain / Fix / Refactor chat here, paste your own Gemini API key in Settings (stored encrypted via Android Keystore).",
          color = palette.textMuted,
          fontSize = 12.sp,
        )
        OutlinedButton(
          onClick = onOpenSettings,
          modifier = Modifier.fillMaxWidth().testTag("ai_go_to_settings_button"),
        ) {
          Text("Add Gemini API Key in Settings")
        }
      }
    } else {
      // Selection / File Quick Actions
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val hasSelection = !activeTab?.selectedText.isNullOrBlank()
        listOf("Explain", "Fix", "Refactor").forEach { action ->
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.inputBg)
                .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                .clickable(enabled = !isLoading && activeTab != null) { onRunCodeAction(action) }
                .padding(vertical = 8.dp)
                .testTag("ai_action_${action.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              if (hasSelection) "$action Sel" else action,
              color = if (activeTab != null) palette.textPrimary else palette.textMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }

      if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = palette.accent)
      }

      if (errorMessage != null) {
        Text(
          errorMessage,
          color = palette.deletedFg,
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }

      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (messages.isEmpty()) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                activeTab?.let { "Ask about ${it.name} or tap Explain / Fix / Refactor." }
                  ?: "Open a file or ask a coding question below.",
                color = palette.textMuted,
                fontSize = 12.sp,
              )
            }
          }
        } else {
          items(messages, key = { it.id }) { msg ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (msg.isUser) palette.selectionBg.copy(alpha = 0.5f) else palette.editorBg)
                  .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                  .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                if (msg.isUser) "You" else "Gemini",
                color = if (msg.isUser) palette.accent else palette.addedFg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
              Text(
                msg.text,
                color = palette.textPrimary,
                fontFamily = if (!msg.isUser) JetBrainsMonoFontFamily else null,
                fontSize = 12.sp,
              )
              if (!msg.isUser) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier =
                      Modifier.clip(RoundedCornerShape(4.dp))
                        .background(palette.inputBg)
                        .clickable { clipboard.setText(AnnotatedString(msg.text)) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", color = palette.textPrimary, fontSize = 11.sp)
                  }
                  if (activeTab != null) {
                    val codeBlock = extractFirstCodeBlock(msg.text) ?: msg.text
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(4.dp))
                          .background(palette.accent)
                          .clickable { onInsertCode(codeBlock) }
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Text("Insert in Editor", color = Color.White, fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OutlinedTextField(
          value = input,
          onValueChange = { input = it },
          placeholder = { Text("Ask about selected code...", fontSize = 12.sp) },
          maxLines = 3,
          modifier = Modifier.weight(1f).testTag("ai_chat_input"),
        )
        IconButton(
          onClick = {
            val prompt = input.trim()
            if (prompt.isNotEmpty()) {
              input = ""
              onSendMessage(prompt)
            }
          },
          enabled = !isLoading && input.isNotBlank(),
          modifier = Modifier.testTag("ai_chat_send_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = palette.accent)
        }
      }
    }
  }
}

private fun extractFirstCodeBlock(text: String): String? {
  val regex = Regex("""```[a-zA-Z0-9_-]*\n([\s\S]*?)```""")
  return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
}
