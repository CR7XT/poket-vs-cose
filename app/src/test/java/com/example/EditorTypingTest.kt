package com.example

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.PocketCodeTheme
import com.example.ui.theme.paletteById
import com.example.ui.workspace.PocketCodeShell
import com.example.ui.workspace.WorkspaceViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Create a new file, type into the editor, and check the text reaches the view model. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class EditorTypingTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
  private var vmRef: WorkspaceViewModel? = null

  private fun setShell() {
    composeRule.setContent {
      val vm: WorkspaceViewModel = viewModel()
      vmRef = vm
      val state by vm.uiState.collectAsStateWithLifecycle()
      PocketCodeTheme(palette = paletteById(state.themeId)) { PocketCodeShell(viewModel = vm) }
    }
    composeRule.waitForIdle()
  }

  private fun findEditor(v: View): CodeEditor? {
    if (v is CodeEditor) return v
    if (v is ViewGroup) for (i in 0 until v.childCount) findEditor(v.getChildAt(i))?.let { return it }
    return null
  }

  private fun waitUntil(timeoutMs: Long = 8000, cond: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < end) {
      composeRule.waitForIdle()
      org.robolectric.shadows.ShadowLooper.idleMainLooper(300, java.util.concurrent.TimeUnit.MILLISECONDS)
      if (cond()) return
      Thread.sleep(50)
    }
  }

  private fun editor(): CodeEditor? = findEditor(composeRule.activity.window.decorView)

  private fun activeContent(): String? = vmRef!!.uiState.value.activeTab?.currentContent

  @Test
  fun `typing into a freshly created file updates the tab`() {
    setShell()
    composeRule.runOnUiThread { vmRef!!.createFile("", "brand_new_${System.nanoTime()}.py") }
    waitUntil { vmRef!!.uiState.value.activeTab != null && editor() != null }
    val ed = editor()
    assertNotNull("editor view must exist for the new file", ed)
    waitUntil(15000) { ed!!.isEditable }
    val busy = CodeEditor::class.java.getDeclaredField("layoutBusy").apply { isAccessible = true }.get(ed)
    assertEquals(
      "editor must become editable: editable=${ed!!.editable} layoutBusy=$busy formatting=${ed.isFormatting} size=${ed.width}x${ed.height}",
      true,
      ed.isEditable,
    )

    composeRule.runOnUiThread { ed!!.insertText("print(1)", 8) }
    waitUntil { activeContent() == "print(1)" }
    assertEquals("insertText must reach the view model", "print(1)", activeContent())

    // Same path the soft keyboard uses.
    composeRule.runOnUiThread {
      assertEquals("isEditable", true, ed!!.isEditable)
      assertEquals("isEnabled", true, ed.isEnabled)
      val ic = ed.onCreateInputConnection(EditorInfo())
      assertNotNull("editor must provide an input connection", ic)
      ic!!.commitText("\nx = 2", 1)
    }
    waitUntil { activeContent()?.contains("x = 2") == true }
    assertEquals("print(1)\nx = 2", activeContent())
  }
}
