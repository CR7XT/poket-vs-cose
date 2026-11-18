package com.example

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.settings.IdeLayoutMode
import com.example.ui.theme.PocketCodeTheme
import com.example.ui.theme.paletteById
import com.example.ui.workspace.PocketCodeShell
import com.example.ui.workspace.WorkspaceViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** On a phone in portrait, "Vertical" / "Horizontal" layout must still leave the editor usable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class VerticalLayoutTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
  private var vmRef: WorkspaceViewModel? = null

  /** Layout mode is persisted in DataStore and shared by every test in this JVM: always restore it. */
  @After
  fun restoreAutoLayout() {
    val vm = vmRef ?: return
    composeRule.runOnUiThread { vm.setLayoutMode(IdeLayoutMode.AUTO) }
    repeat(10) { waitUntil(100) { false } }
  }

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
      composeRule.mainClock.advanceTimeBy(250)
      composeRule.waitForIdle()
      org.robolectric.shadows.ShadowLooper.idleMainLooper(300, java.util.concurrent.TimeUnit.MILLISECONDS)
      if (cond()) return
      Thread.sleep(50)
    }
  }

  private fun editor(): CodeEditor? = findEditor(composeRule.activity.window.decorView)

  private fun editorWidthIn(mode: IdeLayoutMode): Pair<Int, Int> {
    setShell()
    composeRule.runOnUiThread {
      vmRef!!.setLayoutMode(mode)
      vmRef!!.createFile("", "vert_${System.nanoTime()}.py")
    }
    waitUntil { vmRef!!.uiState.value.activeTab != null && editor() != null }
    repeat(10) { waitUntil(100) { false } }
    val ed = editor()
    assertNotNull("editor must exist in $mode", ed)
    val screenW = composeRule.activity.window.decorView.width
    return ed!!.width to screenW
  }

  @Test
  fun `vertical mode on a phone keeps the editor wide enough to use`() {
    val (editorW, screenW) = editorWidthIn(IdeLayoutMode.VERTICAL_VSCODE)
    println("VERTICAL editor=$editorW screen=$screenW")
    assertTrue("editor only $editorW px of $screenW px", editorW >= screenW * 0.6f)
  }

  @Test
  fun `switching layout recreates the editor without losing text or typing`() {
    setShell()
    composeRule.runOnUiThread { vmRef!!.createFile("", "swap_${System.nanoTime()}.py") }
    waitUntil { vmRef!!.uiState.value.activeTab != null && editor() != null }
    repeat(15) { waitUntil(100) { false } }
    val first = editor()!!
    waitUntil(15000) { first.isEditable }
    composeRule.runOnUiThread { first.insertText("hello", 5) }
    waitUntil { vmRef!!.uiState.value.activeTab?.currentContent == "hello" }

    // AUTO on a phone = touch layout; VERTICAL = IDE chrome: the editor view is rebuilt.
    composeRule.runOnUiThread { vmRef!!.setLayoutMode(IdeLayoutMode.VERTICAL_VSCODE) }
    waitUntil { editor() != null && editor() !== first }
    repeat(15) { waitUntil(100) { false } }
    val second = editor()!!
    waitUntil(15000) { second.isEditable && second.text.toString() == "hello" }
    assertTrue("new editor must show the file, was '${second.text}'", second.text.toString() == "hello")
    composeRule.runOnUiThread { second.insertText("!", 1) }
    waitUntil { vmRef!!.uiState.value.activeTab?.currentContent == "hello!" }
    assertTrue(
      "typing after rebuild must reach the tab: '${vmRef!!.uiState.value.activeTab?.currentContent}'",
      vmRef!!.uiState.value.activeTab?.currentContent == "hello!",
    )
  }

  @Test
  fun `horizontal split on a phone keeps the editor wide enough to use`() {
    val (editorW, screenW) = editorWidthIn(IdeLayoutMode.HORIZONTAL_SPLIT)
    println("HORIZONTAL editor=$editorW screen=$screenW")
    assertTrue("editor only $editorW px of $screenW px", editorW >= screenW * 0.6f)
  }
}
