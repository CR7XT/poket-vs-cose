package com.example.data.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

sealed interface WsConnectionState {
  data object Disconnected : WsConnectionState
  data object Connecting : WsConnectionState
  data class Connected(val url: String) : WsConnectionState
  data class Error(val message: String) : WsConnectionState
}

class WebSocketTerminalClient(
  private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
  private var webSocket: WebSocket? = null

  private val _connectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
  val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

  private val _outputBuffer = MutableStateFlow("")
  val outputBuffer: StateFlow<String> = _outputBuffer.asStateFlow()

  fun connect(url: String) {
    disconnect()
    val cleanUrl = url.trim()
    if (!cleanUrl.startsWith("ws://") && !cleanUrl.startsWith("wss://")) {
      _connectionState.value = WsConnectionState.Error("URL must start with ws:// or wss://")
      return
    }
    _connectionState.value = WsConnectionState.Connecting
    appendOutput("\r\n[Connecting to $cleanUrl ...]\r\n")

    val request = Request.Builder().url(cleanUrl).build()
    webSocket =
      okHttpClient.newWebSocket(
        request,
        object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = WsConnectionState.Connected(cleanUrl)
            appendOutput("[Connected to external terminal]\r\n")
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
            appendOutput(stripAnsiControlNoise(text))
          }

          override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val decoded = bytes.utf8()
            // ttyd binary protocol prefixes terminal output frames with '0'
            val payload =
              if (decoded.startsWith("0") && decoded.length > 1) decoded.drop(1) else decoded
            appendOutput(stripAnsiControlNoise(payload))
          }

          override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val msg = t.localizedMessage ?: "WebSocket connection failed"
            _connectionState.value = WsConnectionState.Error(msg)
            appendOutput("\r\n[Error: $msg]\r\n")
          }

          override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WsConnectionState.Disconnected
            appendOutput("\r\n[Connection closed ($code): $reason]\r\n")
          }
        },
      )
  }

  fun sendCommand(text: String) {
    webSocket?.send(text)
  }

  fun clearOutput() {
    _outputBuffer.value = ""
  }

  fun disconnect() {
    webSocket?.close(1000, "User disconnected")
    webSocket = null
    if (_connectionState.value !is WsConnectionState.Error) {
      _connectionState.value = WsConnectionState.Disconnected
    }
  }

  private fun appendOutput(chunk: String) {
    _outputBuffer.update { current ->
      val combined = current + chunk
      if (combined.length > MAX_BUFFER_CHARS) combined.takeLast(MAX_BUFFER_CHARS) else combined
    }
  }

  private fun stripAnsiControlNoise(raw: String): String =
    raw.replace(Regex("""\u001B\[[0-9;?]*[A-Za-z]"""), "")

  companion object {
    private const val MAX_BUFFER_CHARS = 48_000
  }
}
