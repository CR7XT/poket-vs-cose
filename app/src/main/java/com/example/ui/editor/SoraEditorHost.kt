package com.example.ui.editor

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.workspace.CodeLanguage
import com.example.data.workspace.EditorTab
import com.example.data.workspace.TextMateSetup
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SelectionMovement

enum class CursorDirection {
  LEFT,
  RIGHT,
  UP,
  DOWN,
}

class SoraEditorController {
  internal var editor: CodeEditor? = null
  var ctrlArmed by mutableStateOf(false)
    private set

  fun toggleCtrl() {
    ctrlArmed = !ctrlArmed
  }

  fun clearCtrl() {
    ctrlArmed = false
  }

  fun undo() {
    editor?.undo()
  }

  fun redo() {
    editor?.redo()
  }

  fun insertSymbol(symbol: String) {
    val ed = editor ?: return
    if (ctrlArmed) {
      ctrlArmed = false
      when (symbol.lowercase()) {
        "a" -> ed.selectAll()
        "c" -> ed.copyText()
        "v" -> ed.pasteText()
        "x" -> ed.cutText()
        "z" -> ed.undo()
        "y" -> ed.redo()
      }
      return
    }
    ed.insertText(symbol, symbol.length)
    ed.requestFocus()
  }

  fun sendTab() {
    val ed = editor ?: return
    ed.onKeyDown(KeyEvent.KEYCODE_TAB, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
    ed.requestFocus()
  }

  fun sendEscape() {
    val ed = editor ?: return
    ed.searcher.stopSearch()
    ed.setSelection(ed.cursor.leftLine, ed.cursor.leftColumn)
    ctrlArmed = false
  }

  fun moveCursor(direction: CursorDirection) {
    val ed = editor ?: return
    val movement =
      when (direction) {
        CursorDirection.LEFT -> SelectionMovement.LEFT
        CursorDirection.RIGHT -> SelectionMovement.RIGHT
        CursorDirection.UP -> SelectionMovement.UP
        CursorDirection.DOWN -> SelectionMovement.DOWN
      }
    ed.moveOrExtendSelection(movement, false)
    ed.requestFocus()
  }

  fun jumpToLine(lineOneBased: Int) {
    val ed = editor ?: return
    val zeroBased = (lineOneBased - 1).coerceIn(0, (ed.lineCount - 1).coerceAtLeast(0))
    ed.setSelection(zeroBased, 0)
    ed.ensurePositionVisible(zeroBased, 0)
  }

  fun search(query: String, matchCase: Boolean, useRegex: Boolean) {
    val ed = editor ?: return
    if (query.isEmpty()) {
      ed.searcher.stopSearch()
      return
    }
    val type =
      if (useRegex) EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
      else EditorSearcher.SearchOptions.TYPE_NORMAL
    ed.searcher.search(query, EditorSearcher.SearchOptions(type, !matchCase))
  }

  fun findNext() {
    editor?.searcher?.gotoNext()
  }

  fun findPrevious() {
    editor?.searcher?.gotoPrevious()
  }

  fun replaceCurrent(replacement: String) {
    editor?.searcher?.replaceCurrentMatch(replacement)
  }

  fun replaceAll(replacement: String) {
    editor?.searcher?.replaceAll(replacement)
  }

  fun stopSearch() {
    editor?.searcher?.stopSearch()
  }

  fun replaceAllText(newText: String) {
    val ed = editor ?: return
    val content = ed.text
    val lastLine = (content.lineCount - 1).coerceAtLeast(0)
    val lastCol = content.getColumnCount(lastLine)
    content.replace(0, 0, lastLine, lastCol, newText)
    ed.requestFocus()
  }
}

@Composable
fun SoraCodeEditorView(
  tab: EditorTab,
  themeId: String,
  fontSizeSp: Float,
  tabWidth: Int,
  wordWrap: Boolean,
  pendingJumpLine: Int?,
  controller: SoraEditorController,
  onTextChanged: (relativePath: String, newText: String) -> Unit,
  onCursorChanged: (line: Int, col: Int, selected: String, canUndo: Boolean, canRedo: Boolean) -> Unit,
  onFontSizeChanged: (Float) -> Unit,
  onJumpConsumed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentTab by rememberUpdatedState(tab)
  val currentOnTextChanged by rememberUpdatedState(onTextChanged)
  val currentOnCursorChanged by rememberUpdatedState(onCursorChanged)
  val currentOnFontSizeChanged by rememberUpdatedState(onFontSizeChanged)

  val suppressChangeRef = remember { booleanArrayOf(false) }
  var loadedPath by remember { mutableStateOf<String?>(null) }
  var loadedLanguage by remember { mutableStateOf<CodeLanguage?>(null) }
  var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }

  DisposableEffect(editorInstance) {
    val ed = editorInstance
    if (ed == null) {
      onDispose {}
    } else {
      val contentReceipt: SubscriptionReceipt<ContentChangeEvent> =
        ed.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
          if (!suppressChangeRef[0]) {
            val text = ed.text.toString()
            currentOnTextChanged(currentTab.relativePath, text)
            val cursor = ed.cursor
            val selected =
              if (cursor.isSelected) {
                runCatching { ed.text.substring(cursor.left, cursor.right) }.getOrDefault("")
              } else {
                ""
              }
            currentOnCursorChanged(
              cursor.leftLine + 1,
              cursor.leftColumn + 1,
              selected,
              ed.canUndo(),
              ed.canRedo(),
            )
          }
        }

      val selectionReceipt: SubscriptionReceipt<SelectionChangeEvent> =
        ed.subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
          val cursor = ed.cursor
          val selected =
            if (cursor.isSelected) {
              runCatching { ed.text.substring(cursor.left, cursor.right) }.getOrDefault("")
            } else {
              ""
            }
          currentOnCursorChanged(
            cursor.leftLine + 1,
            cursor.leftColumn + 1,
            selected,
            ed.canUndo(),
            ed.canRedo(),
          )
        }

      onDispose {
        contentReceipt.unsubscribe()
        selectionReceipt.unsubscribe()
        controller.editor = null
        ed.release()
      }
    }
  }

  LaunchedEffect(tab.relativePath, editorInstance) {
    val ed = editorInstance ?: return@LaunchedEffect
    if (loadedPath != tab.relativePath) {
      suppressChangeRef[0] = true
      ed.setText(tab.currentContent)
      val lang = TextMateSetup.createLanguage(tab.language)
      ed.setEditorLanguage(lang)
      val targetLine = (tab.cursorLine - 1).coerceIn(0, (ed.lineCount - 1).coerceAtLeast(0))
      val maxCol = ed.text.getColumnCount(targetLine)
      val targetCol = (tab.cursorColumn - 1).coerceIn(0, maxCol)
      ed.setSelection(targetLine, targetCol)
      loadedPath = tab.relativePath
      loadedLanguage = tab.language
      suppressChangeRef[0] = false
      currentOnCursorChanged(targetLine + 1, targetCol + 1, "", ed.canUndo(), ed.canRedo())
    } else if (loadedLanguage != tab.language) {
      ed.setEditorLanguage(TextMateSetup.createLanguage(tab.language))
      loadedLanguage = tab.language
    }
  }

  LaunchedEffect(themeId, editorInstance) {
    val ed = editorInstance ?: return@LaunchedEffect
    TextMateSetup.setTheme(themeId)
    ed.colorScheme = TextMateSetup.createColorScheme()
  }

  LaunchedEffect(pendingJumpLine, editorInstance, loadedPath) {
    if (pendingJumpLine != null && loadedPath == tab.relativePath && editorInstance != null) {
      controller.jumpToLine(pendingJumpLine)
      onJumpConsumed()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      TextMateSetup.ensureInitialized(ctx)
      TextMateSetup.setTheme(themeId)
      CodeEditor(ctx).apply {
        colorScheme = TextMateSetup.createColorScheme()
        val mono = ResourcesCompat.getFont(ctx, R.font.jetbrains_mono)
        if (mono != null) {
          typefaceText = mono
          typefaceLineNumber = mono
        }
        setTextSize(fontSizeSp)
        isWordwrap = wordWrap
        isScalable = true
        this.tabWidth = tabWidth
        isLineNumberEnabled = true
        setPinLineNumber(true)
        props.symbolPairAutoCompletion = true
        setOnScaleListenerCompat(this) { scaledSp ->
          currentOnFontSizeChanged(scaledSp)
        }
        controller.editor = this
        editorInstance = this
      }
    },
    update = { ed ->
      controller.editor = ed
      ed.isWordwrap = wordWrap
      ed.tabWidth = tabWidth
      val density = ed.resources.displayMetrics.scaledDensity
      val currentSp = ed.textSizePx / density
      if (kotlin.math.abs(currentSp - fontSizeSp) > 0.4f) {
        ed.setTextSize(fontSizeSp)
      }
    },
  )
}

private fun setOnScaleListenerCompat(editor: CodeEditor, onScaleSp: (Float) -> Unit) {
  var lastReported = -1f
  editor.setOnTouchListener { _, event ->
    if (event.pointerCount >= 2 || event.actionMasked == android.view.MotionEvent.ACTION_UP) {
      val sp = (editor.textSizePx / editor.resources.displayMetrics.scaledDensity).coerceIn(10f, 28f)
      val rounded = kotlin.math.round(sp)
      if (kotlin.math.abs(rounded - lastReported) >= 1f) {
        lastReported = rounded
        onScaleSp(rounded)
      }
    }
    false
  }
}
