package com.corridaparticular.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.Normalizer
import java.util.Locale

/**
 * Gera o código Pix "copia e cola" (BR Code estático, padrão EMV do Banco Central)
 * e o QR Code correspondente.
 */
object Pix {

    /** Dados do desenvolvedor para o "Cafezinho". */
    object Dev {
        const val NOME = "Roberto Santos da Silva"
        const val CHAVE = "robertobegood+itau@gmail.com"
        const val CIDADE = "Sao Paulo"
    }

    private fun campo(id: String, valor: String): String =
        id + valor.length.toString().padStart(2, '0') + valor

    /** Remove acentos e caracteres fora do padrão, em maiúsculas, limitado ao tamanho do campo. */
    private fun limpar(texto: String, max: Int): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .uppercase(Locale.ROOT)
            .trim()
            .take(max)

    /**
     * @param valor valor em reais; null deixa o pagador digitar o valor no app do banco.
     */
    fun copiaECola(
        chave: String = Dev.CHAVE,
        nome: String = Dev.NOME,
        cidade: String = Dev.CIDADE,
        valor: Double? = null,
        txid: String = "***",
    ): String {
        val conta = campo("00", "br.gov.bcb.pix") + campo("01", chave.trim())
        val sb = StringBuilder()
            .append(campo("00", "01"))
            .append(campo("26", conta))
            .append(campo("52", "0000"))
            .append(campo("53", "986"))
        if (valor != null && valor > 0) sb.append(campo("54", String.format(Locale.US, "%.2f", valor)))
        sb.append(campo("58", "BR"))
            .append(campo("59", limpar(nome, 25)))
            .append(campo("60", limpar(cidade, 15)))
            .append(campo("62", campo("05", txid)))
            .append("6304")
        return sb.toString() + crc16(sb.toString())
    }

    /** CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF), exigido pelo BR Code. */
    fun crc16(texto: String): String {
        var crc = 0xFFFF
        for (b in texto.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase(Locale.ROOT).padStart(4, '0')
    }

    fun qrCode(conteudo: String, tamanho: Int = 720): Bitmap {
        val dicas = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val m = QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, dicas)
        val px = IntArray(m.width * m.height) { i ->
            if (m.get(i % m.width, i / m.width)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(px, m.width, m.height, Bitmap.Config.ARGB_8888)
    }
}
