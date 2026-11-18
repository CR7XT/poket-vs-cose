package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.PocketCodeTheme
import com.example.ui.theme.paletteById
import com.example.ui.workspace.PocketCodeShell
import com.example.ui.workspace.WorkspaceViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    CrashLogger.install(this)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      val workspaceViewModel: WorkspaceViewModel = viewModel()
      val state by workspaceViewModel.uiState.collectAsStateWithLifecycle()
      val palette = paletteById(state.themeId)
      var crashReport by remember { mutableStateOf(CrashLogger.read(this@MainActivity)) }

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

        crashReport?.let { report ->
          AlertDialog(
            onDismissRequest = {
              CrashLogger.clear(this@MainActivity)
              crashReport = null
            },
            title = { Text("PocketCode crashed last time") },
            text = {
              Column(
                modifier =
                  Modifier.heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
              ) {
                Text(report, fontSize = 10.sp)
              }
            },
            confirmButton = {
              TextButton(
                onClick = {
                  val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  cm.setPrimaryClip(ClipData.newPlainText("PocketCode crash", report))
                }
              ) {
                Text("Copy")
              }
            },
            dismissButton = {
              TextButton(
                onClick = {
                  CrashLogger.clear(this@MainActivity)
                  crashReport = null
                }
              ) {
                Text("Dismiss")
              }
            },
          )
        }
      }
    }
  }
}
