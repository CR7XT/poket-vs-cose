package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIdePalette = staticCompositionLocalOf { VsCodeDarkPlusPalette }

@Composable
fun PocketCodeTheme(
  palette: IdePalette = VsCodeDarkPlusPalette,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (palette.isDark) {
    darkColorScheme(
      primary = palette.accent,
      onPrimary = Color.White,
      secondary = palette.accent,
      background = palette.editorBg,
      onBackground = palette.textPrimary,
      surface = palette.sidebarBg,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.tabInactiveBg,
      onSurfaceVariant = palette.textMuted,
      outline = palette.border,
      error = palette.deletedFg,
    )
  } else {
    lightColorScheme(
      primary = palette.accent,
      onPrimary = Color.White,
      secondary = palette.accent,
      background = palette.editorBg,
      onBackground = palette.textPrimary,
      surface = palette.sidebarBg,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.tabInactiveBg,
      onSurfaceVariant = palette.textMuted,
      outline = palette.border,
      error = palette.deletedFg,
    )
  }

  CompositionLocalProvider(LocalIdePalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = PocketCodeTypography,
      content = content,
    )
  }
}
