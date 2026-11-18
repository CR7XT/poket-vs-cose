package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.PocketCodeTheme
import com.example.ui.theme.paletteById
import com.example.ui.workspace.PocketCodeShell
import com.example.ui.workspace.WorkspaceViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      val workspaceViewModel: WorkspaceViewModel = viewModel()
      val state by workspaceViewModel.uiState.collectAsStateWithLifecycle()
      val palette = paletteById(state.themeId)

      SideEffect {
        val barStyle =
          if (palette.isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
          } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
          }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
      }

      PocketCodeTheme(palette = palette) {
        PocketCodeShell(viewModel = workspaceViewModel)
      }
    }
  }
}
