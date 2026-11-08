package com.corridaparticular.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verifica se há uma versão nova publicada nas Releases do GitHub,
 * baixa o APK e abre o instalador do Android.
 */
object Atualizacao {

    private const val API = "https://api.github.com/repos/robertobegood-jpg/Corrida-Particular-app-Android/releases/latest"
    private const val PREFS = "atualizacao"
    private const val INTERVALO_MS = 6 * 60 * 60 * 1000L   // no máximo uma consulta a cada 6 h

    data class Versao(
        val versao: String,      // ex.: "1.4"
        val urlApk: String,
        val tamanho: Long,
        val notas: String,
        val urlPagina: String,
    )

    fun versaoInstalada(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    /** "1.10" > "1.9"; ignora o "v" e sufixos como "-beta". */
    fun maisNova(a: String, b: String): Boolean {
        fun partes(v: String) = v.trim().removePrefix("v").removePrefix("V")
            .split('.', '-', ' ').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val x = partes(a); val y = partes(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pularVersao(ctx: Context, versao: String) =
        prefs(ctx).edit().putString("pular", versao).apply()

    /**
     * Retorna a versão nova, se houver.
     * @param manual true ignora o intervalo e a versão pulada (botão "Verificar atualizações").
     */
    suspend fun verificar(ctx: Context, manual: Boolean = false): Result<Versao?> = withContext(Dispatchers.IO) {
        val p = prefs(ctx)
        val agora = System.currentTimeMillis()
        if (!manual && agora - p.getLong("ultima", 0L) < INTERVALO_MS) return@withContext Result.success(null)
        runCatching {
            val con = URL(API).openConnection() as HttpURLConnection
            val json = try {
                con.connectTimeout = 10_000
                con.readTimeout = 15_000
                con.setRequestProperty("Accept", "application/vnd.github+json")
                if (con.responseCode == 404) return@runCatching null
                if (con.responseCode !in 200..299) error("GitHub respondeu ${con.responseCode}")
                JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            } finally {
                con.disconnect()
            }
            p.edit().putLong("ultima", agora).apply()

            val versao = json.optString("tag_name").removePrefix("v").removePrefix("V")
            if (versao.isBlank() || !maisNova(versao, versaoInstalada(ctx))) return@runCatching null
            if (!manual && p.getString("pular", null) == versao) return@runCatching null

            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0)).mapNotNull { assets?.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return@runCatching null   // Release ainda sem APK (compilando)

            Versao(
                versao = versao,
                urlApk = apk.optString("browser_download_url"),
                tamanho = apk.optLong("size", 0L),
                notas = json.optString("body").trim(),
                urlPagina = json.optString("html_url"),
            )
        }
    }

    private fun pasta(ctx: Context) = File(ctx.cacheDir, "atualizacao").apply { mkdirs() }

    /** Baixa o APK informando o progresso (0f a 1f). */
    suspend fun baixar(ctx: Context, v: Versao, progresso: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            pasta(ctx).listFiles()?.forEach { it.delete() }
            val destino = File(pasta(ctx), "CorridaParticular-${v.versao}.apk")
            val con = URL(v.urlApk).openConnection() as HttpURLConnection
            try {
                con.connectTimeout = 15_000
                con.readTimeout = 30_000
                con.instanceFollowRedirects = true
                if (con.responseCode !in 200..299) error("Download falhou (${con.responseCode})")
                val total = con.contentLengthLong.takeIf { it > 0 } ?: v.tamanho
                con.inputStream.use { entrada ->
                    destino.outputStream().use { saida ->
                        val buf = ByteArray(64 * 1024)
                        var lidos = 0L
                        while (true) {
                            val n = entrada.read(buf)
                            if (n < 0) break
                            saida.write(buf, 0, n)
                            lidos += n
                            if (total > 0) withContext(Dispatchers.Main) { progresso((lidos.toFloat() / total).coerceIn(0f, 1f)) }
                        }
                    }
                }
            } finally {
                con.disconnect()
            }
            if (v.tamanho > 0 && destino.length() != v.tamanho) error("Download incompleto. Tente de novo.")
            destino
        }
    }

    /** O Android exige que o usuário autorize o app a instalar atualizações (só na 1ª vez). */
    fun podeInstalar(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun abrirPermissaoInstalar(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun instalar(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
