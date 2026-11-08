package com.corridaparticular.app.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.corridaparticular.app.data.Calculo
import com.corridaparticular.app.data.Config
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.util.dataHoraTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.minutosTexto
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast
import java.io.File

enum class TipoDoc(val titulo: String) {
    ORCAMENTO("Orçamento de corrida"),
    CONFIRMACAO("Confirmação de corrida agendada"),
    RECIBO("Recibo de corrida"),
}

object Compartilhar {

    /** Deixa só os números e coloca o 55 do Brasil quando faltar. */
    fun normalizarFone(f: String): String {
        var d = f.filter { it.isDigit() }.trimStart('0')
        if (d.length in 10..11) d = "55$d"
        return d
    }

    // ---------------- Texto ----------------

    fun texto(c: Corrida, tipo: TipoDoc, cfg: Config = Repo.config.value): String = buildString {
        appendLine("*${tipo.titulo}* 🚗")
        if (c.clienteNome.isNotBlank()) appendLine("Cliente: ${c.clienteNome}")
        appendLine("Data: ${c.dataHora.dataHoraTexto()}")
        appendLine()
        if (c.origem.texto.isNotBlank()) appendLine("📍 Origem: ${c.origem.texto}")
        c.paradas.forEachIndexed { i, p -> appendLine("➕ Parada ${i + 1}: ${p.texto}") }
        if (c.destino.texto.isNotBlank()) appendLine("🏁 Destino: ${c.destino.texto}")
        appendLine()
        if (c.distanciaKm > 0) appendLine("Distância: ${c.distanciaKm.kmTexto()}")
        if (c.minutos > 0) {
            val extra = if (tipo != TipoDoc.RECIBO && c.minutosAtraso >= 1) " (inclui ${minutosTexto(c.minutosAtraso)} de trânsito)" else ""
            appendLine((if (tipo == TipoDoc.RECIBO) "Duração: " else "Tempo previsto: ") + minutosTexto(c.minutos) + extra)
        }
        if (c.bandeira2) appendLine("Tarifa: bandeira 2")
        appendLine()
        val rotulo = if (tipo == TipoDoc.RECIBO) "Valor" else "Valor estimado"
        appendLine("💰 *$rotulo: ${c.valor.reais()}*")
        if (tipo == TipoDoc.RECIBO && c.valorEspera > 0) {
            appendLine("Inclui espera de ${minutosTexto(c.minutosEspera)}: ${c.valorEspera.reais()}")
        }
        if (tipo != TipoDoc.RECIBO) {
            val t = Repo.tarifas.value
            appendLine("⏳ Espera: ${t.esperaGratisMin} min grátis no embarque e em cada parada; depois ${t.esperaPorMinuto.reais()}/min.")
            appendLine("_Previsão com base no trânsito do horário. O valor final pode variar conforme o percurso real._")
        }
        if (cfg.pix.isNotBlank()) appendLine("Pix: ${cfg.pix}")
        if (c.obs.isNotBlank()) { appendLine(); appendLine("Obs.: ${c.obs}") }
        val assinatura = listOf(cfg.nome, cfg.telefone).filter { it.isNotBlank() }.joinToString(" • ")
        if (assinatura.isNotBlank()) { appendLine(); append(assinatura) }
        val carro = listOf(cfg.veiculo, cfg.placa).filter { it.isNotBlank() }.joinToString(" - ")
        if (carro.isNotBlank()) { appendLine(); append("Veículo: $carro") }
    }.trim()

    fun whatsappTexto(ctx: Context, telefone: String, mensagem: String) {
        val fone = normalizarFone(telefone)
        val url = "https://wa.me/$fone?text=${Uri.encode(mensagem)}"
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, mensagem)
            ctx.startActivity(Intent.createChooser(envio, "Enviar mensagem"))
        }
    }

    // ---------------- PDF ----------------

    fun whatsappPdf(ctx: Context, c: Corrida, tipo: TipoDoc) {
        val arquivo = try { gerarPdf(ctx, c, tipo) } catch (e: Exception) {
            ctx.toast("Não foi possível gerar o PDF: ${e.message}")
            return
        }
        compartilharArquivo(ctx, arquivo, "application/pdf", c.telefone, tipo.titulo)
    }

    fun compartilharArquivo(ctx: Context, arquivo: File, mime: String, telefone: String = "", titulo: String = "Enviar") {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", arquivo)
        val base = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val fone = normalizarFone(telefone)
        if (mime == "application/pdf") {
            if (fone.isNotEmpty()) base.putExtra("jid", "$fone@s.whatsapp.net")
            for (pacote in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                try {
                    ctx.startActivity(Intent(base).setPackage(pacote))
                    return
                } catch (e: ActivityNotFoundException) { /* tenta o próximo */ }
            }
        }
        ctx.startActivity(Intent.createChooser(base, titulo))
    }

    fun pastaCompartilhar(ctx: Context): File = File(ctx.cacheDir, "compartilhar").apply { mkdirs() }

    fun gerarPdf(ctx: Context, c: Corrida, tipo: TipoDoc): File {
        val cfg = Repo.config.value
        val t = Repo.tarifas.value
        val doc = PdfDocument()
        val pagina = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val cv = pagina.canvas
        val azul = Color.rgb(31, 58, 95)
        val amarelo = Color.rgb(242, 183, 5)
        val cinza = Color.rgb(110, 110, 110)

        fun paint(tam: Float, cor: Int = Color.BLACK, negrito: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tam
            color = cor
            typeface = Typeface.create(Typeface.SANS_SERIF, if (negrito) Typeface.BOLD else Typeface.NORMAL)
        }

        val margem = 40f
        val largura = 595f - 2 * margem

        // Cabeçalho
        cv.drawRect(0f, 0f, 595f, 110f, Paint().apply { color = azul })
        cv.drawRect(0f, 110f, 595f, 116f, Paint().apply { color = amarelo })
        cv.drawText(cfg.nome.ifBlank { "Corrida Particular" }, margem, 50f, paint(22f, Color.WHITE, true))
        val sub = listOf(cfg.telefone, listOf(cfg.veiculo, cfg.placa).filter { it.isNotBlank() }.joinToString(" - "))
            .filter { it.isNotBlank() }.joinToString("   •   ")
        if (sub.isNotBlank()) cv.drawText(sub, margem, 78f, paint(12f, Color.WHITE))
        cv.drawText(tipo.titulo.uppercase(), margem, 150f, paint(18f, azul, true))
        cv.drawText("Emitido em ${System.currentTimeMillis().dataHoraTexto()}", margem, 168f, paint(10f, cinza))

        var y = 200f

        fun linha(rotulo: String, valor: String) {
            if (valor.isBlank()) return
            cv.drawText(rotulo, margem, y, paint(10f, cinza, true))
            y += 15f
            val p = paint(12.5f)
            var resto = valor
            while (resto.isNotEmpty()) {
                val n = p.breakText(resto, true, largura, null).coerceAtLeast(1)
                var corte = n
                if (n < resto.length) {
                    val espaco = resto.substring(0, n).lastIndexOf(' ')
                    if (espaco > 0) corte = espaco
                }
                cv.drawText(resto.substring(0, corte).trim(), margem, y, p)
                resto = resto.substring(corte).trimStart()
                y += 17f
            }
            y += 8f
        }

        linha("CLIENTE", c.clienteNome)
        linha(if (tipo == TipoDoc.RECIBO) "DATA DA CORRIDA" else "DATA / HORA PREVISTA", c.dataHora.dataHoraTexto())
        linha("ORIGEM", c.origem.texto)
        c.paradas.forEachIndexed { i, p -> linha("PARADA ${i + 1}", p.texto) }
        linha("DESTINO", c.destino.texto)

        cv.drawLine(margem, y, 595f - margem, y, paint(1f, Color.LTGRAY))
        y += 24f

        // Resumo
        fun resumo(rotulo: String, valor: String) {
            cv.drawText(rotulo, margem, y, paint(12f, cinza))
            val pv = paint(12f, Color.BLACK, true)
            cv.drawText(valor, 595f - margem - pv.measureText(valor), y, pv)
            y += 20f
        }
        if (c.distanciaKm > 0) resumo("Distância", c.distanciaKm.kmTexto())
        if (c.minutos > 0) resumo(if (tipo == TipoDoc.RECIBO) "Duração" else "Tempo previsto (com trânsito)", minutosTexto(c.minutos))
        if (tipo != TipoDoc.RECIBO && c.minutosAtraso >= 1) resumo("Atraso estimado por trânsito", minutosTexto(c.minutosAtraso))
        if (c.minutosParado >= 1) resumo("Tempo parado", minutosTexto(c.minutosParado))
        if (tipo == TipoDoc.RECIBO && c.minutosEspera >= 1) resumo("Espera do passageiro", minutosTexto(c.minutosEspera))
        if (tipo == TipoDoc.RECIBO && c.valorEspera > 0) resumo("Valor da espera (incluído)", c.valorEspera.reais())
        if (tipo != TipoDoc.RECIBO && c.distanciaKm > 0) {
            val d = Calculo.estimativa(t, c.distanciaKm, c.minutos, c.bandeira2)
            if (d.total == Calculo.arred(c.valor)) {
                resumo("Bandeirada", d.bandeirada.reais())
                resumo("Quilometragem (${t.porKm.reais()}/km)", d.km.reais())
                resumo("Tempo (${t.porMinuto.reais()}/min)", d.tempo.reais())
                if (d.adicionalB2 > 0) resumo("Adicional bandeira 2", d.adicionalB2.reais())
                if (d.aplicouMinimo) resumo("Valor mínimo aplicado", t.valorMinimo.reais())
            }
        } else if (c.bandeira2) resumo("Tarifa", "Bandeira 2")

        // Valor em destaque
        y += 12f
        cv.drawRoundRect(RectF(margem, y, 595f - margem, y + 70f), 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 244, 204) })
        cv.drawText(if (tipo == TipoDoc.RECIBO) "VALOR DA CORRIDA" else "VALOR ESTIMADO", margem + 18f, y + 28f, paint(11f, cinza, true))
        cv.drawText(c.valor.reais(), margem + 18f, y + 58f, paint(26f, azul, true))
        y += 95f

        if (cfg.pix.isNotBlank()) { cv.drawText("Pagamento via Pix: ${cfg.pix}", margem, y, paint(12f, Color.BLACK, true)); y += 22f }
        if (c.obs.isNotBlank()) linha("OBSERVAÇÕES", c.obs)

        if (tipo != TipoDoc.RECIBO) {
            val aviso = "Valor previsto com base na previsão de trânsito para o horário informado. " +
                "Espera: ${t.esperaGratisMin} min grátis no embarque e em cada parada; depois ${t.esperaPorMinuto.reais()} por minuto, " +
                "somado ao valor final. O valor pode variar conforme o percurso real."
            val p = paint(9.5f, cinza)
            var resto = aviso
            var yy = 790f
            while (resto.isNotEmpty()) {
                val n = p.breakText(resto, true, largura, null).coerceAtLeast(1)
                var corte = n
                if (n < resto.length) { val e = resto.substring(0, n).lastIndexOf(' '); if (e > 0) corte = e }
                cv.drawText(resto.substring(0, corte).trim(), margem, yy, p)
                resto = resto.substring(corte).trimStart()
                yy += 13f
            }
        }

        doc.finishPage(pagina)
        val prefixo = when (tipo) { TipoDoc.RECIBO -> "recibo"; TipoDoc.CONFIRMACAO -> "confirmacao"; else -> "orcamento" }
        val nomeCliente = c.clienteNome.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(20)
        val arquivo = File(pastaCompartilhar(ctx), "${prefixo}_${nomeCliente.ifBlank { "corrida" }}_${System.currentTimeMillis() / 1000}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }
}
