package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.PocketCodeTheme
import com.example.ui.theme.paletteById
import com.example.ui.workspace.PocketCodeShell
import com.example.ui.workspace.WorkspaceViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Portrait phone navigation: bottom tabs and system Back.
 *
 * The terminal tab is intentionally not covered here: Termux's native PTY library cannot load in a
 * JVM unit test, so terminal navigation has to be checked on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class NavigationFlowTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun setShell() {
    composeRule.setContent {
      val vm: WorkspaceViewModel = viewModel()
      val state by vm.uiState.collectAsStateWithLifecycle()
      PocketCodeTheme(palette = paletteById(state.themeId)) { PocketCodeShell(viewModel = vm) }
    }
    composeRule.waitForIdle()
  }

  private fun tap(tag: String) {
    composeRule.onNodeWithTag(tag).performClick()
    composeRule.waitForIdle()
  }

  private fun pressSystemBack() {
    composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()
  }

  @Test
  fun `switching between bottom tabs and settings never crashes`() {
    setShell()
    listOf(
        "nav_git",
        "nav_tasks",
        "nav_files",
        "nav_editor",
        "nav_git",
        "toolbar_settings_button",
        "nav_tasks",
        "nav_files",
        "nav_git",
        "nav_editor",
      )
      .forEach { tap(it) }
    composeRule.onNodeWithTag("nav_editor").assertIsSelected()
  }

  @Test
  fun `system back from git returns to the editor`() {
    setShell()
    tap("nav_git")
    composeRule.onNodeWithTag("nav_git").assertIsSelected()
    pressSystemBack()
    composeRule.onNodeWithTag("nav_editor").assertIsSelected()
  }

  @Test
  fun `system back from tasks and files returns to the editor`() {
    setShell()
    tap("nav_tasks")
    composeRule.onNodeWithTag("nav_tasks").assertIsSelected()
    pressSystemBack()
    composeRule.onNodeWithTag("nav_editor").assertIsSelected()

    tap("nav_files")
    composeRule.onNodeWithTag("nav_files").assertIsSelected()
    pressSystemBack()
    composeRule.onNodeWithTag("nav_editor").assertIsSelected()
  }
}
