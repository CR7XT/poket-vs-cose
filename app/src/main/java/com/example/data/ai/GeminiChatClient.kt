package com.example.data.ai

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AiChatMessage(
  val id: Long,
  val isUser: Boolean,
  val text: String,
)

class GeminiChatClient(
  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build(),
) {

  suspend fun sendPrompt(
    apiKey: String,
    modelId: String,
    history: List<AiChatMessage>,
    userPrompt: String,
  ): String =
    withContext(Dispatchers.IO) {
      val cleanKey = apiKey.trim()
      require(cleanKey.isNotEmpty()) { "Gemini API key is required" }
      val model = modelId.trim().ifEmpty { DEFAULT_MODEL }

      val contents = JSONArray()
      for (msg in history.takeLast(10)) {
        contents.put(
          JSONObject()
            .put("role", if (msg.isUser) "user" else "model")
            .put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
        )
      }
      contents.put(
        JSONObject()
          .put("role", "user")
          .put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
      )

      val payload =
        JSONObject()
          .put(
            "systemInstruction",
            JSONObject()
              .put(
                "parts",
                JSONArray()
                  .put(
                    JSONObject()
                      .put(
                        "text",
                        "You are an expert software engineer inside PocketCode mobile IDE. Provide concise, accurate code explanations, bug fixes, and refactors.",
                      )
                  )
              ),
          )
          .put("contents", contents)

      val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
      val request =
        Request.Builder()
          .url(url)
          .addHeader("x-goog-api-key", cleanKey)
          .addHeader("Content-Type", "application/json")
          .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
          .build()

      okHttpClient.newCall(request).execute().use { response ->
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val apiMsg =
            runCatching { JSONObject(rawBody).getJSONObject("error").getString("message") }
              .getOrNull()
          throw IOException(apiMsg ?: "Gemini API HTTP ${response.code}")
        }
        val root = JSONObject(rawBody)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
          throw IOException("Gemini returned no candidates")
        }
        val parts = candidates.getJSONObject(0).getJSONObject("content").optJSONArray("parts")
        val text =
          buildString {
            if (parts != null) {
              for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
              }
            }
          }.trim()
        if (text.isEmpty()) throw IOException("Empty response from Gemini")
        text
      }
    }

  companion object {
    const val DEFAULT_MODEL = "gemini-flash-latest"
  }
}
