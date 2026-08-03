package ps.anira.aid.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TelegramSendResult {
    data object Success : TelegramSendResult()
    data class Failure(val message: String) : TelegramSendResult()
}

/**
 * يعادل sendBackupToTelegram() بالويب حرفياً: نفس نقطة Bot API الرسمية
 * (sendDocument)، ونفس معالجة الأخطاء (استخراج j.description من رد Telegram
 * عند الفشل). التوكن يذهب بمسار الطلب — هذا هو الاستخدام الرسمي الموثَّق
 * لـ Telegram Bot API، وليس تسريباً أو خطأ.
 */
class TelegramClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun sendDocument(
        botToken: String,
        chatId: String,
        fileName: String,
        jsonContent: String,
        caption: String
    ): TelegramSendResult = withContext(Dispatchers.IO) {
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart(
                    "document", fileName,
                    jsonContent.toRequestBody("application/json".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$botToken/sendDocument")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val ok = runCatching {
                    Json.parseToJsonElement(text).jsonObject["ok"]?.jsonPrimitive?.boolean == true
                }.getOrDefault(false)

                if (ok) {
                    TelegramSendResult.Success
                } else {
                    val description = runCatching {
                        Json.parseToJsonElement(text).jsonObject["description"]?.jsonPrimitive?.content
                    }.getOrNull()
                    TelegramSendResult.Failure(description ?: "تحقّق من صحة التوكن ومعرّف المحادثة.")
                }
            }
        } catch (e: IOException) {
            TelegramSendResult.Failure("تعذّر الاتصال — تحقّق من الإنترنت.")
        }
    }
}
