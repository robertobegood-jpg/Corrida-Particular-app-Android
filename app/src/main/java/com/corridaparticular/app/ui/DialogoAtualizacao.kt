package com.corridaparticular.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.corridaparticular.app.ui.theme.Oficina
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.corridaparticular.app.util.Atualizacao
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.launch
import java.io.File

/** Versão nova encontrada (compartilhada entre a verificação automática e a manual). */
object EstadoAtualizacao {
    var disponivel by mutableStateOf<Atualizacao.Versao?>(null)
}

/** Verifica ao abrir o app (no máximo a cada 6 h) e mostra o aviso quando houver versão nova. */
@Composable
fun VerificadorAtualizacao() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Atualizacao.verificar(ctx).getOrNull()?.let { EstadoAtualizacao.disponivel = it }
    }
    EstadoAtualizacao.disponivel?.let { v ->
        DialogoAtualizacao(v, onFechar = { EstadoAtualizacao.disponivel = null })
    }
}

@Composable
private fun DialogoAtualizacao(v: Atualizacao.Versao, onFechar: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var progresso by remember { mutableStateOf<Float?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    var aguardandoPermissao by remember { mutableStateOf(false) }

    fun instalarOuPedir(arquivo: File) {
        if (Atualizacao.podeInstalar(ctx)) {
            Atualizacao.instalar(ctx, arquivo)
        } else {
            ctx.toast("Autorize o Corrida Particular a instalar atualizações e volte para o app.")
            aguardandoPermissao = true
            Atualizacao.abrirPermissaoInstalar(ctx)
        }
    }

    // Ao voltar da tela de permissão, segue com a instalação.
    LaunchedEffect(aguardandoPermissao) {
        if (!aguardandoPermissao) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val arquivo = apk
            if (arquivo != null && Atualizacao.podeInstalar(ctx)) {
                aguardandoPermissao = false
                Atualizacao.instalar(ctx, arquivo)
            }
        }
    }

    val c = Oficina.cores
    val baixando = progresso != null && apk == null
    DialogoOficina(
        titulo = "Versão ${v.versao} disponível",
        eyebrow = "Atualização",
        onDismiss = { if (progresso == null) onFechar() },
        acoes = {
            if (progresso == null) {
                BotaoTexto("Pular", { Atualizacao.pularVersao(ctx, v.versao); onFechar() }, cor = c.muted)
                BotaoTexto("Depois", onFechar, cor = c.muted)
            }
            BotaoTexto(
                if (apk != null) "Instalar" else "Atualizar",
                {
                    val pronto = apk
                    if (pronto != null) {
                        instalarOuPedir(pronto)
                    } else {
                        progresso = 0f
                        scope.launch {
                            Atualizacao.baixar(ctx, v) { progresso = it }
                                .onSuccess { arquivo -> progresso = 1f; apk = arquivo; instalarOuPedir(arquivo) }
                                .onFailure { e -> progresso = null; ctx.toast(e.message ?: "Falha no download") }
                        }
                    }
                },
                habilitado = !baixando,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Seus clientes, corridas e configurações continuam salvos na atualização.", style = Oficina.tipos.bodySm, color = c.muted)
            if (v.notas.isNotBlank()) {
                Hud("Novidades")
                Text(
                    v.notas.take(1500),
                    style = Oficina.tipos.bodySm, color = c.ink,
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                )
            }
            progresso?.let { p ->
                Box(Modifier.fillMaxWidth().height(2.dp).background(c.line)) {
                    Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(2.dp).background(c.accent))
                }
                Hud(if (p >= 1f) "Download concluído" else "Baixando · ${(p * 100).toInt()}%")
            }
        }
    }
}
