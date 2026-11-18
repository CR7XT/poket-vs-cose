package com.example

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * Saves the stack trace of an uncaught exception to app-private storage so the next launch can show
 * it (with a Copy button). That makes any crash reproducible from a phone without adb/logcat.
 */
object CrashLogger {
  private const val FILE_NAME = "last_crash.txt"

  fun install(context: Context) {
    val appContext = context.applicationContext
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      runCatching {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        File(appContext.filesDir, FILE_NAME)
          .writeText("Time: ${Date()}\nThread: ${thread.name}\n\n$trace")
      }
      previous?.uncaughtException(thread, throwable)
    }
  }

  fun read(context: Context): String? =
    runCatching {
        File(context.applicationContext.filesDir, FILE_NAME)
          .takeIf { it.exists() }
          ?.readText()
          ?.takeIf { it.isNotBlank() }
      }
      .getOrNull()

  fun clear(context: Context) {
    runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
  }
}
