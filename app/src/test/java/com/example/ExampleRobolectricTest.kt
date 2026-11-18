package com.example

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.example.data.secrets.SecurePrefsManager
import com.example.data.tasks.TaskGroup
import com.example.data.tasks.TaskRepository
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.TextMateSetup
import com.example.ui.editor.CodeFormatter
import com.example.ui.workspace.WorkspaceViewModel
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches PocketCode`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PocketCode", appName)
  }

  @Test
  fun `terminal view initializes with setTextSize before setTypeface without NPE`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val termView = TerminalView(context, null)
    termView.setTextSize(28)
    termView.setTypeface(Typeface.MONOSPACE)

    val viewClient =
      object : TerminalViewClient {
        override fun onScale(scale: Float): Float = 1.0f
        override fun onSingleTapUp(e: MotionEvent?) {}
        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
        override fun shouldEnforceCharBasedInput(): Boolean = true
        override fun shouldUseCtrlSpaceWorkaround(): Boolean = true
        override fun isTerminalViewSelected(): Boolean = true
        override fun copyModeChanged(copyMode: Boolean) {}
        override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
        override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
        override fun onLongPress(event: MotionEvent?): Boolean = false
        override fun readControlKey(): Boolean = false
        override fun readAltKey(): Boolean = false
        override fun readShiftKey(): Boolean = false
        override fun readFnKey(): Boolean = false
        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
        override fun onEmulatorSet() {}
        override fun logError(tag: String?, message: String?) {}
        override fun logWarn(tag: String?, message: String?) {}
        override fun logInfo(tag: String?, message: String?) {}
        override fun logDebug(tag: String?, message: String?) {}
        override fun logVerbose(tag: String?, message: String?) {}
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
        override fun logStackTrace(tag: String?, e: Exception?) {}
      }
    termView.setTerminalViewClient(viewClient)

    // Measure & draw at 0x0 (collapsed drawer) and non-zero size
    termView.measure(
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
    )
    termView.layout(0, 0, 0, 0)
    termView.updateSize()
    termView.draw(Canvas())

    termView.measure(
      View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
    )
    termView.layout(0, 0, 800, 400)
    termView.updateSize()
    termView.draw(Canvas())
  }

  @Test
  fun `textmate setup initializes themes and languages cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    TextMateSetup.ensureInitialized(context)
    listOf("dark_plus", "tokyo_night", "github_dark", "light_plus").forEach { themeId ->
      TextMateSetup.setTheme(themeId)
      assertNotNull(TextMateSetup.createColorScheme())
    }
    CodeLanguage.entries.forEach { lang ->
      assertNotNull(TextMateSetup.createLanguage(lang))
    }
  }

  @Test
  fun `secure prefs manager reads and writes safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SecurePrefsManager(context)
    prefs.setGeminiApiKey("test-key-123")
    assertEquals("test-key-123", prefs.getGeminiApiKey())
    prefs.clearAllSecrets()
    assertEquals("", prefs.getGeminiApiKey())
  }

  @Test
  fun `workspace viewmodel constructs cleanly without runtime exceptions`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = WorkspaceViewModel(app)
    assertNotNull(vm.uiState.value)
  }

  @Test
  fun `task repository persists vscode tasks json and builds execution command`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val tempWorkspace = File(context.cacheDir, "test_workspace_tasks").apply {
        deleteRecursively()
        mkdirs()
      }
      val repo = TaskRepository(tempWorkspace)
      val initialTasks = repo.loadTasks()
      assertTrue(initialTasks.isNotEmpty())

      repo.addTask(
        label = "Custom Lint",
        command = "python3 -m py_compile main.py",
        group = TaskGroup.TEST,
        cwd = "scripts",
        isDefault = true,
      )
      val updated = repo.loadTasks()
      val custom = updated.firstOrNull { it.label == "Custom Lint" }
      assertNotNull(custom)
      assertTrue(custom!!.isDefault)
      assertEquals("cd 'scripts' && python3 -m py_compile main.py", repo.buildExecutionCommand(custom))

      repo.deleteTask(custom.id)
      assertFalse(repo.loadTasks().any { it.label == "Custom Lint" })
      tempWorkspace.deleteRecursively()
    }
  }

  @Test
  fun `code formatter formats json cleanly`() {
    val formatted = CodeFormatter.format("{\"a\":1,\"b\":[2,3]}", CodeLanguage.JSON, tabWidth = 2).getOrThrow()
    assertTrue(formatted.contains("\n"))
    assertTrue(formatted.contains("  \"a\": 1"))
  }
}
