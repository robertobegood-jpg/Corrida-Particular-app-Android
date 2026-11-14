package com.corridaparticular.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

/**
 * Carrega as fontes do sistema Oficina a partir de app/src/main/assets/fonts/.
 * Se um arquivo não estiver lá, usa a fonte do Android no lugar, sem travar o app.
 */
class FontesOficina(context: Context) {
    private val assets = context.applicationContext.assets

    /** Testa abrindo cada arquivo (listar a pasta não funciona em todos os ambientes). */
    private val disponiveis: Set<String> = TODAS.filter { nome ->
        runCatching { assets.open("fonts/$nome").close() }.isSuccess
    }.toSet()

    /** Archivo expandido (wdth 118), peso 620. */
    val display: FontFamily = carregar(
        arquivo = ARCHIVO, peso = 620, variacao = "'wdth' 118, 'wght' 620", reserva = FontFamily.SansSerif
    )
    val serif: FontFamily = carregar(
        arquivo = INSTRUMENT_SERIF, peso = 400, italico = true, reserva = FontFamily.Serif
    )
    val body: FontFamily = carregar(
        arquivo = INTER, peso = 400, variacao = "'wght' 400", reserva = FontFamily.SansSerif
    )
    val mono: FontFamily = carregar(
        arquivo = GEIST_MONO, peso = 500, variacao = "'wght' 500", reserva = FontFamily.Monospace
    )
    val monoNegrito: FontFamily = carregar(
        arquivo = GEIST_MONO, peso = 700, variacao = "'wght' 700", reserva = FontFamily.Monospace
    )

    /** Arquivos que ainda não foram copiados para a pasta de fontes. */
    val faltando: List<String> = TODAS.filterNot { it in disponiveis }

    private fun carregar(
        arquivo: String,
        peso: Int,
        reserva: FontFamily,
        variacao: String? = null,
        italico: Boolean = false
    ): FontFamily {
        if (arquivo !in disponiveis) return reserva
        return try {
            val builder = Typeface.Builder(assets, "fonts/$arquivo")
                .setWeight(peso)
                .setItalic(italico)
            if (variacao != null) builder.setFontVariationSettings(variacao)
            val typeface = builder.build()
            if (typeface != null) FontFamily(typeface) else reserva
        } catch (e: Exception) {
            reserva
        }
    }

    companion object {
        const val ARCHIVO = "archivo.ttf"
        const val INSTRUMENT_SERIF = "instrument_serif_italic.ttf"
        const val INTER = "inter.ttf"
        const val GEIST_MONO = "geist_mono.ttf"
        val TODAS = listOf(ARCHIVO, INSTRUMENT_SERIF, INTER, GEIST_MONO)
    }
}
