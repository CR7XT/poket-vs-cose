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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun FindReplaceBar(
  findQuery: String,
  replaceQuery: String,
  matchCase: Boolean,
  useRegex: Boolean,
  onFindQueryChange: (String) -> Unit,
  onReplaceQueryChange: (String) -> Unit,
  onToggleMatchCase: () -> Unit,
  onToggleRegex: () -> Unit,
  onFindPrevious: () -> Unit,
  onFindNext: () -> Unit,
  onReplaceOne: () -> Unit,
  onReplaceAll: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(palette.sidebarBg)
        .border(0.5.dp, palette.border)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      CompactSearchField(
        value = findQuery,
        placeholder = "Find",
        onValueChange = onFindQueryChange,
        modifier = Modifier.weight(1f).testTag("find_query_input"),
      )
      OptionChip(
        label = "Aa",
        active = matchCase,
        onClick = onToggleMatchCase,
        testTag = "find_match_case_toggle",
      )
      OptionChip(
        label = ".*",
        active = useRegex,
        onClick = onToggleRegex,
        testTag = "find_regex_toggle",
      )
      IconButton(
        onClick = onFindPrevious,
        modifier = Modifier.size(34.dp).testTag("find_prev_button"),
      ) {
        Icon(
          Icons.Default.ArrowUpward,
          contentDescription = "Previous match",
          tint = palette.textPrimary,
          modifier = Modifier.size(16.dp),
        )
      }
      IconButton(
        onClick = onFindNext,
        modifier = Modifier.size(34.dp).testTag("find_next_button"),
      ) {
        Icon(
          Icons.Default.ArrowDownward,
          contentDescription = "Next match",
          tint = palette.textPrimary,
          modifier = Modifier.size(16.dp),
        )
      }
      IconButton(
        onClick = onClose,
        modifier = Modifier.size(34.dp).testTag("find_close_button"),
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close find bar",
          tint = palette.textMuted,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      CompactSearchField(
        value = replaceQuery,
        placeholder = "Replace",
        onValueChange = onReplaceQueryChange,
        modifier = Modifier.weight(1f).testTag("replace_query_input"),
      )
      ActionChip("Replace", onClick = onReplaceOne, testTag = "replace_one_button")
      ActionChip("All", onClick = onReplaceAll, testTag = "replace_all_button")
    }
  }
}

@Composable
private fun CompactSearchField(
  value: String,
  placeholder: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current
  Box(
    modifier =
      modifier
        .height(32.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(palette.inputBg)
        .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
        .padding(horizontal = 8.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (value.isEmpty()) {
      Text(placeholder, color = palette.textMuted, fontSize = 12.sp)
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      textStyle =
        TextStyle(
          color = palette.textPrimary,
          fontSize = 12.sp,
          fontFamily = JetBrainsMonoFontFamily,
        ),
      cursorBrush = SolidColor(palette.accent),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun OptionChip(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val palette = LocalIdePalette.current
  Box(
    modifier =
      Modifier.testTag(testTag)
        .height(30.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(if (active) palette.accent else palette.inputBg)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (active) palette.statusBarFg else palette.textPrimary,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun ActionChip(
  label: String,
  onClick: () -> Unit,
  testTag: String,
) {
  val palette = LocalIdePalette.current
  Box(
    modifier =
      Modifier.testTag(testTag)
        .height(30.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(palette.inputBg)
        .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = palette.textPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}
