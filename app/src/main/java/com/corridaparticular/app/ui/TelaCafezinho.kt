@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.corridaparticular.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corridaparticular.app.util.Pix
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast

private val VALORES = listOf(5.0, 10.0, 20.0)

/** Tela de apoio ao desenvolvedor via Pix. Totalmente opcional. */
@Composable
fun TelaCafezinho(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    var valor by remember { mutableStateOf<Double?>(10.0) }
    val codigo = remember(valor) { Pix.copiaECola(valor = valor) }
    val qr = remember(codigo) { Pix.qrCode(codigo).asImageBitmap() }

    Tela("Cafezinho para o Dev", onVoltar = onVoltar) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("☕", fontSize = 48.sp)
            Text(
                "O Corrida Particular é gratuito e de código aberto.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Se o app ajuda no seu dia a dia, você pode pagar um cafezinho ao desenvolvedor pelo Pix. " +
                    "É opcional e nada muda no app.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Secao("Escolha o valor") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VALORES.forEach { v ->
                    FilterChip(selected = valor == v, onClick = { valor = v }, label = { Text(v.reais()) })
                }
                FilterChip(selected = valor == null, onClick = { valor = null }, label = { Text("Outro valor") })
            }
            if (valor == null) {
                Text("Você digita o valor no app do seu banco.", style = MaterialTheme.typography.bodySmall)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Image(qr, contentDescription = "QR Code Pix", modifier = Modifier.size(230.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(Pix.Dev.NOME, fontWeight = FontWeight.SemiBold)
                Text(
                    if (valor != null) valor!!.reais() else "Valor livre",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Abra o app do banco › Pix › Ler QR Code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        BotaoAcao("Copiar Pix copia e cola", Icons.Filled.ContentCopy, {
            copiar(ctx, "Pix copia e cola", codigo)
            ctx.toast("Código copiado. Cole no app do banco em Pix › Copia e cola.")
        }, Modifier.fillMaxWidth(), principal = true)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotaoAcao("Copiar chave", Icons.Filled.Key, {
                copiar(ctx, "Chave Pix", Pix.Dev.CHAVE)
                ctx.toast("Chave Pix copiada")
            }, Modifier.weight(1f))
            BotaoAcao("Compartilhar", Icons.Filled.Share, {
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

        Text(
            "Chave Pix: ${Pix.Dev.CHAVE}\nConfira o nome do recebedor antes de confirmar: ${Pix.Dev.NOME}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Obrigado pelo apoio! 💛",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

private fun copiar(ctx: Context, rotulo: String, texto: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(rotulo, texto))
}
