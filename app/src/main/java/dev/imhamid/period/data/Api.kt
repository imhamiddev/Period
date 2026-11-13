package dev.imhamid.period.data

import dev.imhamid.period.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

object Api {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }
    private val JSON_MT = "application/json".toMediaType()

    val configured: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    suspend fun raw(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        if (!configured) throw ApiException(0, "Supabase تنظیم نشده")
        val b = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .header("apikey", BuildConfig.SUPABASE_KEY)
        if (token != null) b.header("Authorization", "Bearer $token")
        headers.forEach { (k, v) -> b.header(k, v) }
        val needsBody = method == "POST" || method == "PATCH" || method == "PUT"
        b.method(method, if (body != null) body.toRequestBody(JSON_MT) else if (needsBody) "".toRequestBody(JSON_MT) else null)
        try {
            http.newCall(b.build()).execute().use { r ->
                val s = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, friendly(parseError(s)))
                s
            }
        } catch (e: java.io.IOException) {
            throw ApiException(-1, "اتصال اینترنت برقرار نیست")
        }
    }

    private fun parseError(s: String): String = runCatching {
        val o = json.parseToJsonElement(s).jsonObject
        (o["msg"] ?: o["message"] ?: o["error_description"] ?: o["error"])?.jsonPrimitive?.content
    }.getOrNull() ?: s

    private fun friendly(m: String): String = when {
        m.contains("Invalid login credentials", true) -> "ایمیل یا رمز عبور اشتباهه"
        m.contains("already registered", true) -> "این ایمیل قبلاً ثبت‌نام کرده"
        m.contains("at least", true) && m.contains("password", true) -> "رمز عبور باید حداقل ۶ کاراکتر باشه"
        m.contains("valid email", true) || m.contains("invalid format", true) -> "ایمیل معتبر نیست"
        m.contains("invalid_code") -> "این کد وجود نداره"
        m.contains("self_code") -> "این کد خودته 😅"
        m.contains("Email not confirmed", true) -> "تایید ایمیل در Supabase فعاله؛ غیرفعالش کن"
        m.contains("rate limit", true) -> "کمی صبر کن و دوباره تلاش کن"
        else -> m.take(140)
    }
}
