package com.example.ui.explorer

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.workspace.SearchFileResult
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIdePalette

@Composable
fun SearchSidebar(
  query: String,
  matchCase: Boolean,
  isSearching: Boolean,
  results: List<SearchFileResult>,
  onQueryChange: (String) -> Unit,
  onToggleMatchCase: () -> Unit,
  onJumpToMatch: (relativePath: String, lineNumber: Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalIdePalette.current

  Column(modifier = modifier.fillMaxSize().background(palette.sidebarBg)) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Text(
        "SEARCH IN FILES",
        color = palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      )
      Spacer(Modifier.height(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(4.dp))
              .background(palette.inputBg)
              .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          if (query.isEmpty()) {
            Text("Search across workspace...", color = palette.textMuted, fontSize = 12.sp)
          }
          BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle =
              TextStyle(
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMonoFontFamily,
              ),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
          )
        }
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(if (matchCase) palette.accent else palette.inputBg)
              .clickable(onClick = onToggleMatchCase)
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("global_search_case_toggle")
        ) {
          Text(
            "Aa",
            color = if (matchCase) palette.statusBarFg else palette.textPrimary,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    if (isSearching) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = palette.accent)
    }

    HorizontalDivider(color = palette.border, thickness = 0.5.dp)

    if (query.isNotBlank() && !isSearching && results.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text("No matches found for \"$query\"", color = palette.textMuted, fontSize = 12.sp)
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        results.forEach { fileResult ->
          item(key = "header_${fileResult.relativePath}") {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(palette.tabInactiveBg.copy(alpha = 0.55f))
                  .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                fileResult.relativePath,
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                "${fileResult.matches.size}",
                color = palette.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          items(
            fileResult.matches,
            key = { "${fileResult.relativePath}:${it.lineNumber}" },
          ) { match ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onJumpToMatch(fileResult.relativePath, match.lineNumber) }
                  .padding(start = 18.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "${match.lineNumber}:",
                color = palette.textMuted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                modifier = Modifier.width(34.dp),
              )
              Text(
                match.preview,
                color = palette.textPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
