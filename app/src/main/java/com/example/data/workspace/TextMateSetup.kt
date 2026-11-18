package com.example.data.workspace

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

object TextMateSetup {

  @Volatile private var initialized = false
  private val lock = Any()

  private val themeSpecs =
    listOf(
      "dark_plus" to true,
      "tokyo_night" to true,
      "github_dark" to true,
      "light_plus" to false,
    )

  fun ensureInitialized(context: Context) {
    if (initialized) return
    synchronized(lock) {
      if (initialized) return
      val appContext = context.applicationContext
      FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(appContext.assets))

      val themeRegistry = ThemeRegistry.getInstance()
      for ((themeId, isDark) in themeSpecs) {
        val themePath = "textmate/$themeId.json"
        FileProviderRegistry.getInstance().tryGetInputStream(themePath)?.use { stream ->
          val model =
            ThemeModel(IThemeSource.fromInputStream(stream, themePath, null), themeId).apply {
              this.isDark = isDark
            }
          themeRegistry.loadTheme(model)
        }
      }
      themeRegistry.setTheme("dark_plus")
      GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
      initialized = true
    }
  }

  fun setTheme(themeId: String) {
    runCatching { ThemeRegistry.getInstance().setTheme(themeId) }
  }

  fun createColorScheme(): TextMateColorScheme =
    TextMateColorScheme.create(ThemeRegistry.getInstance())

  fun createLanguage(language: CodeLanguage): Language {
    val scope = language.scopeName ?: return EmptyLanguage()
    return runCatching { TextMateLanguage.create(scope, true) }.getOrElse { EmptyLanguage() }
  }
}
