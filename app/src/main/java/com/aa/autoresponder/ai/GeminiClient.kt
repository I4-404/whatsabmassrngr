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

    private val MODEL_FALLBACKS = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-2.5-flash")

    suspend fun generateReply(
        apiKey: String,
        systemPrompt: String,
        senderName: String,
        conversationHistory: List<Pair<Boolean, String>>, // true = من المرسل, false = رد سابق مني
        modelIndex: Int = 0,
        attempt: Int = 1
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("مفتاح Gemini API غير موجود"))
        }
        val model = MODEL_FALLBACKS.getOrElse(modelIndex) { MODEL_FALLBACKS.last() }
        try {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val historyText = conversationHistory.joinToString("\n") { (fromSender, text) ->
                if (fromSender) "$senderName: $text" else "أنا (نور): $text"
            }

            val fullPrompt = buildString {
                append(systemPrompt.trim())
                append("\n\n--- المحادثة حتى الآن ---\n")
                append(historyText)
                append("\n\n اكتب ردك التالي على آخر رسالة فقط، مع مراعاة سياق المحادثة كلها فوق. اكتب الرد فقط بدون أي شرح أو علامات اقتباس إضافية.")
            }

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", fullPrompt)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.8)
                    put("maxOutputTokens", 800)
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", 0) // تعطيل التفكير الداخلي عشان الرد ميتقطعش
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // 503 = ازدحام مؤقت في السيرفر، جرب مرة واحدة تانية بعد ثانيتين
                    if (response.code == 503 && attempt < 2) {
                        kotlinx.coroutines.delay(2000)
                        return@withContext generateReply(apiKey, systemPrompt, senderName, conversationHistory, modelIndex, attempt + 1)
                    }
                    // 404 = الموديل ده اتقفل، جرب الموديل الاحتياطي التالي في القائمة
                    if (response.code == 404 && modelIndex < MODEL_FALLBACKS.lastIndex) {
                        return@withContext generateReply(apiKey, systemPrompt, senderName, conversationHistory, modelIndex + 1, attempt)
                    }
                    return@withContext Result.failure(Exception("فشل الاتصال بـ Gemini: ${response.code} - $bodyStr"))
                }
                val json = JSONObject(bodyStr)

                // لو الطلب اتحجب بالكامل قبل توليد أي رد (مثلاً بسبب فلتر الأمان)
                val promptFeedback = json.optJSONObject("promptFeedback")
                val blockReason = promptFeedback?.optString("blockReason")
                if (!blockReason.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("تم حجب الرسالة من Gemini (blockReason: $blockReason)"))
                }

                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Gemini لم يُرجع أي رد (استجابة فارغة)"))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val finishReason = firstCandidate.optString("finishReason")
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                if (parts == null || parts.length() == 0) {
                    return@withContext Result.failure(
                        Exception("Gemini لم يُرجع نصًا (finishReason: $finishReason) - غالبًا تم حجب الرد بواسطة فلاتر الأمان")
                    )
                }

                val text = parts.getJSONObject(0).optString("text")
                if (text.isBlank()) {
                    return@withContext Result.failure(Exception("Gemini أرجع نصًا فارغًا"))
                }
                Result.success(text.trim().trim('"'))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}