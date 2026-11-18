package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

private data class AccessoryKey(
  val label: String,
  val testId: String,
  val onTap: (SoraEditorController) -> Unit,
)

@Composable
fun KeyboardAccessoryBar(
  controller: SoraEditorController,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  val haptics = LocalHapticFeedback.current
  val keys =
    listOf(
      AccessoryKey("TAB", "key_tab") { it.sendTab() },
      AccessoryKey("ESC", "key_esc") { it.sendEscape() },
      AccessoryKey("CTRL", "key_ctrl") { it.toggleCtrl() },
      AccessoryKey("←", "key_left") { it.moveCursor(CursorDirection.LEFT) },
      AccessoryKey("↑", "key_up") { it.moveCursor(CursorDirection.UP) },
      AccessoryKey("↓", "key_down") { it.moveCursor(CursorDirection.DOWN) },
      AccessoryKey("→", "key_right") { it.moveCursor(CursorDirection.RIGHT) },
      AccessoryKey("{", "key_lbrace") { it.insertSymbol("{") },
      AccessoryKey("}", "key_rbrace") { it.insertSymbol("}") },
      AccessoryKey("(", "key_lparen") { it.insertSymbol("(") },
      AccessoryKey(")", "key_rparen") { it.insertSymbol(")") },
      AccessoryKey("[", "key_lbracket") { it.insertSymbol("[") },
      AccessoryKey("]", "key_rbracket") { it.insertSymbol("]") },
      AccessoryKey("<", "key_lt") { it.insertSymbol("<") },
      AccessoryKey(">", "key_gt") { it.insertSymbol(">") },
      AccessoryKey("\"", "key_dquote") { it.insertSymbol("\"") },
      AccessoryKey("'", "key_squote") { it.insertSymbol("'") },
      AccessoryKey("`", "key_backtick") { it.insertSymbol("`") },
      AccessoryKey("/", "key_slash") { it.insertSymbol("/") },
      AccessoryKey(";", "key_semi") { it.insertSymbol(";") },
      AccessoryKey("=", "key_eq") { it.insertSymbol("=") },
      AccessoryKey(":", "key_colon") { it.insertSymbol(":") },
      AccessoryKey("#", "key_hash") { it.insertSymbol("#") },
    )

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(palette.sidebarBg)
        .border(0.5.dp, palette.border)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    keys.forEach { key ->
      val isCtrlActive = key.label == "CTRL" && controller.ctrlArmed
      Box(
        modifier =
          Modifier.testTag(key.testId)
            .defaultMinSize(minWidth = 38.dp, minHeight = 34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isCtrlActive) palette.accent else palette.inputBg)
            .clickable {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              key.onTap(controller)
            }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = key.label,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = if (isCtrlActive) palette.statusBarFg else palette.textPrimary,
        )
      }
    }
  }
}
