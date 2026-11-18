package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class IdePalette(
  val id: String,
  val displayName: String,
  val isDark: Boolean,
  val editorBg: Color,
  val sidebarBg: Color,
  val activityBarBg: Color,
  val tabActiveBg: Color,
  val tabInactiveBg: Color,
  val inputBg: Color,
  val border: Color,
  val accent: Color,
  val statusBarBg: Color,
  val statusBarFg: Color,
  val textPrimary: Color,
  val textMuted: Color,
  val selectionBg: Color,
  val addedFg: Color,
  val modifiedFg: Color,
  val deletedFg: Color,
  val badgeBg: Color,
)

val VsCodeDarkPlusPalette = IdePalette(
  id = "dark_plus",
  displayName = "VS Code Dark+",
  isDark = true,
  editorBg = Color(0xFF1E1E1E),
  sidebarBg = Color(0xFF252526),
  activityBarBg = Color(0xFF333333),
  tabActiveBg = Color(0xFF1E1E1E),
  tabInactiveBg = Color(0xFF2D2D2D),
  inputBg = Color(0xFF3C3C3C),
  border = Color(0xFF3C3C3C),
  accent = Color(0xFF007ACC),
  statusBarBg = Color(0xFF007ACC),
  statusBarFg = Color(0xFFFFFFFF),
  textPrimary = Color(0xFFCCCCCC),
  textMuted = Color(0xFF858585),
  selectionBg = Color(0xFF264F78),
  addedFg = Color(0xFF89D185),
  modifiedFg = Color(0xFFE2C08D),
  deletedFg = Color(0xFFF48771),
  badgeBg = Color(0xFF007ACC),
)

val TokyoNightPalette = IdePalette(
  id = "tokyo_night",
  displayName = "Tokyo Night",
  isDark = true,
  editorBg = Color(0xFF1A1B26),
  sidebarBg = Color(0xFF16161E),
  activityBarBg = Color(0xFF1F2335),
  tabActiveBg = Color(0xFF1A1B26),
  tabInactiveBg = Color(0xFF24283B),
  inputBg = Color(0xFF292E42),
  border = Color(0xFF292E42),
  accent = Color(0xFF7AA2F7),
  statusBarBg = Color(0xFF3D59A1),
  statusBarFg = Color(0xFFFFFFFF),
  textPrimary = Color(0xFFC0CAF5),
  textMuted = Color(0xFF565F89),
  selectionBg = Color(0xFF283457),
  addedFg = Color(0xFF9ECE6A),
  modifiedFg = Color(0xFFE0AF68),
  deletedFg = Color(0xFFF7768E),
  badgeBg = Color(0xFF7AA2F7),
)

val GitHubDarkPalette = IdePalette(
  id = "github_dark",
  displayName = "GitHub Dark",
  isDark = true,
  editorBg = Color(0xFF0D1117),
  sidebarBg = Color(0xFF161B22),
  activityBarBg = Color(0xFF1F242C),
  tabActiveBg = Color(0xFF0D1117),
  tabInactiveBg = Color(0xFF161B22),
  inputBg = Color(0xFF21262D),
  border = Color(0xFF30363D),
  accent = Color(0xFF2F81F7),
  statusBarBg = Color(0xFF1F6FEB),
  statusBarFg = Color(0xFFFFFFFF),
  textPrimary = Color(0xFFE6EDF3),
  textMuted = Color(0xFF8B949E),
  selectionBg = Color(0xFF264F78),
  addedFg = Color(0xFF3FB950),
  modifiedFg = Color(0xFFD29922),
  deletedFg = Color(0xFFF85149),
  badgeBg = Color(0xFF2F81F7),
)

val VsCodeLightPlusPalette = IdePalette(
  id = "light_plus",
  displayName = "Light+",
  isDark = false,
  editorBg = Color(0xFFFFFFFF),
  sidebarBg = Color(0xFFF3F3F3),
  activityBarBg = Color(0xFFE5E5E5),
  tabActiveBg = Color(0xFFFFFFFF),
  tabInactiveBg = Color(0xFFECECEC),
  inputBg = Color(0xFFFFFFFF),
  border = Color(0xFFD4D4D4),
  accent = Color(0xFF005FB8),
  statusBarBg = Color(0xFF005FB8),
  statusBarFg = Color(0xFFFFFFFF),
  textPrimary = Color(0xFF1F2328),
  textMuted = Color(0xFF6E7681),
  selectionBg = Color(0xFFADD6FF),
  addedFg = Color(0xFF1A7F37),
  modifiedFg = Color(0xFF9A6700),
  deletedFg = Color(0xFFCF222E),
  badgeBg = Color(0xFF005FB8),
)

val AllIdePalettes = listOf(
  VsCodeDarkPlusPalette,
  TokyoNightPalette,
  GitHubDarkPalette,
  VsCodeLightPlusPalette,
)

fun paletteById(id: String): IdePalette =
  AllIdePalettes.firstOrNull { it.id == id } ?: VsCodeDarkPlusPalette
