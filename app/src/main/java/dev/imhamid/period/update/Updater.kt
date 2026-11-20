package dev.imhamid.period.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.imhamid.period.BuildConfig
import dev.imhamid.period.data.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

sealed interface UpdateState {
    data object None : UpdateState
    /** [shaUrl]: the release's `<apk>.sha256` asset, when it has one. */
    data class Available(val version: String, val url: String, val notes: String, val shaUrl: String? = null) : UpdateState
    data class Downloading(val version: String, val progress: Float) : UpdateState
    data class Ready(val version: String, val file: File) : UpdateState
    data class Failed(val version: String, val url: String, val message: String, val shaUrl: String? = null) : UpdateState
}

object Updater {
    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").split('.', '-', '+')
            .mapNotNull { it.toIntOrNull() }

    fun isNewer(remote: String, local: String): Boolean {
        val a = parts(remote)
        val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Returns Available when a newer GitHub release with an .apk asset exists. */
    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            Api.http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext UpdateState.None
                val o = Api.json.parseToJsonElement(r.body!!.string()).jsonObject
                val tag = o["tag_name"]?.jsonPrimitive?.content ?: return@withContext UpdateState.None
                val assets = o["assets"]?.jsonArray?.map { it.jsonObject }.orEmpty()
                fun name(a: kotlinx.serialization.json.JsonObject) = a["name"]?.jsonPrimitive?.content.orEmpty()
                fun link(a: kotlinx.serialization.json.JsonObject) = a["browser_download_url"]?.jsonPrimitive?.content
                val apkAsset = assets.firstOrNull { name(it).endsWith(".apk") } ?: return@withContext UpdateState.None
                val apk = link(apkAsset) ?: return@withContext UpdateState.None
                val shaUrl = assets.firstOrNull { name(it) == name(apkAsset) + ".sha256" }?.let { link(it) }
                if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext UpdateState.None
                val notes = o["body"]?.jsonPrimitive?.content.orEmpty()
                UpdateState.Available(tag.removePrefix("v"), apk, notes, shaUrl)
            }
        } catch (e: Exception) {
            UpdateState.None
        }
    }

    /** Downloads the APK; when the release publishes a `.sha256` file the download is verified against it. */
    suspend fun download(ctx: Context, version: String, url: String, shaUrl: String? = null, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "luna-$version.apk")
            val req = Request.Builder().url(url).build()
            val digest = MessageDigest.getInstance("SHA-256")
            Api.http.newBuilder().readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build()
                .newCall(req).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    val body = r.body ?: error("empty")
                    val total = body.contentLength().takeIf { it > 0 }
                    var done = 0L
                    var lastEmit = 0L
                    body.byteStream().use { input ->
                        out.outputStream().use { os ->
                            val buf = ByteArray(32 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                os.write(buf, 0, n)
                                digest.update(buf, 0, n)
                                done += n
                                val now = System.currentTimeMillis()
                                if (total != null && now - lastEmit > 60) {
                                    lastEmit = now
                                    onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
            if (shaUrl != null) {
                val expected = fetchChecksum(shaUrl)
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    out.delete()
                    error("checksum mismatch")
                }
            }
            onProgress(1f)
            out
        }

    /** First token of a `sha256sum` style file: "<64 hex>  <file name>". */
    private fun fetchChecksum(url: String): String =
        Api.http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("checksum HTTP ${r.code}")
            val hex = r.body?.string().orEmpty().trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            if (!Regex("[0-9a-fA-F]{64}").matches(hex)) error("bad checksum file")
            hex
        }

    /** @return false when the user first has to allow "install unknown apps". */
    fun install(ctx: Context, file: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
