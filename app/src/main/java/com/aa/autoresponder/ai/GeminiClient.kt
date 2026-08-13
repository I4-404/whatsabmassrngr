package com.aa.autoresponder.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * عميل بسيط لاستدعاء Gemini API (generateContent) لتوليد رد نصي قصير
 * بناءً على رسالة واردة وبرومبت يحدد أسلوب الرد.
 */
object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-2.0-flash"

    suspend fun generateReply(
        apiKey: String,
        systemPrompt: String,
        senderName: String,
        incomingMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("مفتاح Gemini API غير موجود"))
        }
        try {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"

            val fullPrompt = buildString {
                append(systemPrompt.trim())
                append("\n\n")
                append("اسم المرسل: ").append(senderName).append("\n")
                append("الرسالة الواردة: \"").append(incomingMessage).append("\"\n\n")
                append("اكتب الرد المناسب فقط، بدون أي شرح أو علامات اقتباس إضافية.")
            }

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", fullPrompt)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.8)
                    put("maxOutputTokens", 200)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل الاتصال بـ Gemini: ${response.code} - $bodyStr"))
                }
                val json = JSONObject(bodyStr)
                val text = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                Result.success(text.trim().trim('"'))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
