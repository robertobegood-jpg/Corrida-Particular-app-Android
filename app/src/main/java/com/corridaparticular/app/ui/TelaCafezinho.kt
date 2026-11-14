@file:OptIn(ExperimentalLayoutApi::class)

package com.corridaparticular.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.util.Pix
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast

private val VALORES = listOf(5.0, 10.0, 20.0)

/** Tela de apoio ao desenvolvedor via Pix. Totalmente opcional. */
@Composable
fun TelaCafezinho(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    var valor by remember { mutableStateOf<Double?>(10.0) }
    val codigo = remember(valor) { Pix.copiaECola(valor = valor) }
    val qr = remember(codigo) { Pix.qrCode(codigo).asImageBitmap() }

    Tela(
        titulo = "Pague um *cafezinho*.",
        eyebrow = "Mais · apoio",
        onVoltar = onVoltar,
        subtitulo = "O Corrida Particular é gratuito e de código aberto. Se ele ajuda no seu dia a dia, " +
            "você pode apoiar o desenvolvimento pelo Pix. É opcional e nada muda no app.",
    ) {
        Secao("Valor") {
            Segmentado(
                opcoes = VALORES + listOf<Double?>(null),
                selecionada = valor,
                rotulo = { it?.reais() ?: "Outro" },
                onSelecionar = { valor = it },
            )
            if (valor == null) Text("Você digita o valor no app do seu banco.", style = Oficina.tipos.bodySm, color = c.muted)
        }

        Moldura {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // O QR fica sempre em fundo branco, para qualquer app de banco ler.
                Box(Modifier.background(Color.White).padding(8.dp)) {
                    Image(qr, contentDescription = "QR Code Pix", modifier = Modifier.size(220.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Hud("Recebedor")
            Text(Pix.Dev.NOME, style = Oficina.tipos.body, color = c.ink)
            Spacer(Modifier.height(10.dp))
            Hud("Valor")
            Text(valor?.reais() ?: "Livre", style = Oficina.tipos.heading, color = c.inkStrong)
            Spacer(Modifier.height(10.dp))
            Text("Abra o app do banco › Pix › Ler QR Code.", style = Oficina.tipos.bodySm, color = c.muted)
        }

        BotaoPrimario("Copiar Pix copia e cola", {
            copiar(ctx, "Pix copia e cola", codigo)
            ctx.toast("Código copiado. Cole no app do banco em Pix › Copia e cola.")
        }, Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotaoSecundario("Copiar chave", {
                copiar(ctx, "Chave Pix", Pix.Dev.CHAVE)
                ctx.toast("Chave Pix copiada")
            }, Modifier.weight(1f))
            BotaoSecundario("Compartilhar", {
                val texto = "Pix para o desenvolvedor do app Corrida Particular ☕\n\n" +
                    "Chave: ${Pix.Dev.CHAVE}\nNome: ${Pix.Dev.NOME}\n\nCopia e cola:\n$codigo"
                ctx.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto),
                        "Compartilhar Pix",
                    )
                )
            }, Modifier.weight(1f))
        }

        Column {
            Hud("Chave Pix")
            Text(Pix.Dev.CHAVE, style = Oficina.tipos.bodySm, color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text("Confira o nome do recebedor antes de confirmar. Obrigado pelo apoio!", style = Oficina.tipos.bodySm, color = c.muted)
        }
    }
}

private fun copiar(ctx: Context, rotulo: String, texto: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(rotulo, texto))
}
