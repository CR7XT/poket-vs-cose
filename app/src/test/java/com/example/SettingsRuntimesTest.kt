package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsRuntimesTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `settings shows language installer and storage access, custom install enables on input`() {
    composeRule.setContent {
      val vm: WorkspaceViewModel = viewModel()
      val state by vm.uiState.collectAsStateWithLifecycle()
      PocketCodeTheme(palette = paletteById(state.themeId)) { PocketCodeShell(viewModel = vm) }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("toolbar_settings_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("runtime_search").assertIsDisplayed()
    composeRule.onNodeWithTag("grant_all_files_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("custom_install_button").performScrollTo().assertIsNotEnabled()
    composeRule.onNodeWithTag("custom_install_input").performScrollTo().performTextInput("ripgrep")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("custom_install_button").assertIsEnabled()
  }
}
