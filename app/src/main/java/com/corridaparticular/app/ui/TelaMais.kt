@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.agenda.Lembrete
import com.corridaparticular.app.data.Calculo
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.data.Tarifas
import com.corridaparticular.app.net.TomTom
import com.corridaparticular.app.util.Atualizacao
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.util.decimal
import com.corridaparticular.app.util.paraDouble
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val URL_GITHUB = "https://github.com/robertobegood-jpg/Corrida-Particular-app-Android"

@Composable
fun TelaMais(state: AppState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var sub by rememberSaveable { mutableStateOf("") }
    if (sub.isNotEmpty()) BackHandler { sub = "" }
    when (sub) {
        "config" -> TelaConfig { sub = "" }
        "tarifas" -> TelaTarifas { sub = "" }
        "backup" -> TelaBackup { sub = "" }
        "cafe" -> TelaCafezinho { sub = "" }
        else -> Tela("Mais") {
            ItemMenu(Icons.Filled.AttachMoney, "Tarifas", "Bandeirada, valor por km, minuto, hora parada e bandeira 2") { sub = "tarifas" }
            ItemMenu(Icons.Filled.Settings, "Configurações", "Seus dados, chave TomTom e lembretes") { sub = "config" }
            ItemMenu(Icons.Filled.Backup, "Backup", "Exportar e importar clientes, corridas e ajustes") { sub = "backup" }
            ItemMenu(Icons.Filled.History, "Histórico de corridas", "Ganhos do dia e do mês") { state.aba = Aba.AGENDA }
            ItemMenu(Icons.Filled.Coffee, "Pagar um cafezinho ao Dev", "Gostou do app? Apoie o desenvolvimento pelo Pix") { sub = "cafe" }
            ItemMenu(Icons.Filled.SystemUpdate, "Verificar atualizações", "Versão instalada: ${Atualizacao.versaoInstalada(ctx)}") {
                scope.launch {
                    ctx.toast("Verificando…")
                    Atualizacao.verificar(ctx, manual = true)
                        .onSuccess { v -> if (v != null) EstadoAtualizacao.disponivel = v else ctx.toast("Você já está na versão mais recente") }
                        .onFailure { ctx.toast("Sem conexão com o GitHub. Tente mais tarde.") }
                }
            }
            ItemMenu(Icons.Filled.Code, "Código aberto", "Ver o projeto no GitHub, sugerir melhorias ou relatar problemas") {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_GITHUB)))
            }
            Text(
                "Corrida Particular • versão 1.4",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ItemMenu(icone: ImageVector, titulo: String, descricao: String, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(titulo, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(descricao) },
            leadingContent = { Icon(icone, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
        )
    }
}

// ------------------------------------------------------------------ Configurações

@Composable
private fun TelaConfig(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg = remember { Repo.config.value }
    var nome by remember { mutableStateOf(cfg.nome) }
    var fone by remember { mutableStateOf(cfg.telefone) }
    var veiculo by remember { mutableStateOf(cfg.veiculo) }
    var placa by remember { mutableStateOf(cfg.placa) }
    var pix by remember { mutableStateOf(cfg.pix) }
    var chave by remember { mutableStateOf(cfg.chaveTomTom) }
    var lembrete by remember { mutableStateOf(cfg.lembreteMin.toString()) }
    var testando by remember { mutableStateOf(false) }

    fun salvar() {
        Repo.salvarConfig(
            cfg.copy(
                nome = nome.trim(), telefone = fone.trim(), veiculo = veiculo.trim(), placa = placa.trim(),
                pix = pix.trim(), chaveTomTom = chave.trim(),
                lembreteMin = lembrete.trim().toIntOrNull()?.coerceIn(0, 24 * 60) ?: 30,
            )
        )
        Lembrete.reagendarTodas(ctx)
        ctx.toast("Configurações salvas")
        onVoltar()
    }

    Tela("Configurações", onVoltar = onVoltar, acoes = {
        IconButton(onClick = { salvar() }) { Icon(Icons.Filled.Check, "Salvar") }
    }) {
        Secao("Seus dados (aparecem no orçamento e no recibo)") {
            CampoTexto("Seu nome ou nome da empresa", nome, { nome = it })
            CampoTexto("Seu WhatsApp", fone, { fone = it }, teclado = KeyboardType.Phone)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Veículo", veiculo, { veiculo = it }, Modifier.weight(1.4f))
                CampoTexto("Placa", placa, { placa = it.uppercase() }, Modifier.weight(1f))
            }
            CampoTexto("Chave Pix (opcional)", pix, { pix = it })
        }

        Secao("Rotas e trânsito – TomTom") {
            Text(
                "Para buscar endereços e calcular a rota com previsão de trânsito, crie uma chave gratuita no site da TomTom " +
                    "(menu “API Keys” depois do cadastro) e cole abaixo.",
                style = MaterialTheme.typography.bodySmall,
            )
            CampoTexto("Chave da API TomTom", chave, { chave = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotaoAcao("Criar chave", Icons.Filled.OpenInBrowser, {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.tomtom.com/")))
                }, Modifier.weight(1f))
                BotaoAcao(if (testando) "Testando…" else "Testar chave", Icons.Filled.NetworkCheck, {
                    testando = true
                    scope.launch {
                        try {
                            val r = TomTom.buscar(chave.trim(), "Avenida Paulista, São Paulo")
                            ctx.toast(if (r.isNotEmpty()) "Chave funcionando ✓" else "A chave respondeu, mas sem resultados")
                        } catch (e: Exception) {
                            ctx.toast(e.message ?: "Falhou")
                        } finally {
                            testando = false
                        }
                    }
                }, Modifier.weight(1f), habilitado = chave.isNotBlank() && !testando)
            }
        }

        Secao("Agenda") {
            CampoTexto("Avisar antes da corrida", lembrete, { lembrete = it.filter { c -> c.isDigit() } }, teclado = KeyboardType.Number, sufixo = "minutos")
        }

        Button(onClick = { salvar() }, Modifier.fillMaxWidth().height(52.dp)) { Text("Salvar") }
    }
}

// ------------------------------------------------------------------ Tarifas

@Composable
private fun TelaTarifas(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val t = remember { Repo.tarifas.value }
    var bandeirada by remember { mutableStateOf(t.bandeirada.decimal()) }
    var porKm by remember { mutableStateOf(t.porKm.decimal()) }
    var porMin by remember { mutableStateOf(t.porMinuto.decimal()) }
    var porHora by remember { mutableStateOf(t.porHoraParada.decimal()) }
    var esperaGratis by remember { mutableStateOf(t.esperaGratisMin.toString()) }
    var esperaMin by remember { mutableStateOf(t.esperaPorMinuto.decimal()) }
    var minimo by remember { mutableStateOf(t.valorMinimo.decimal()) }
    var b2Pct by remember { mutableStateOf(t.bandeira2Pct.decimal(0)) }
    var b2Ini by remember { mutableStateOf(t.b2HoraInicio.toString()) }
    var b2Fim by remember { mutableStateOf(t.b2HoraFim.toString()) }
    var b2Dom by remember { mutableStateOf(t.b2Domingo) }

    fun montar(): Tarifas = Tarifas(
        bandeirada = bandeirada.paraDouble() ?: t.bandeirada,
        porKm = porKm.paraDouble() ?: t.porKm,
        porMinuto = porMin.paraDouble() ?: t.porMinuto,
        porHoraParada = porHora.paraDouble() ?: t.porHoraParada,
        esperaGratisMin = esperaGratis.toIntOrNull()?.coerceIn(0, 120) ?: t.esperaGratisMin,
        esperaPorMinuto = esperaMin.paraDouble() ?: t.esperaPorMinuto,
        valorMinimo = minimo.paraDouble() ?: t.valorMinimo,
        bandeira2Pct = b2Pct.paraDouble() ?: t.bandeira2Pct,
        b2HoraInicio = b2Ini.toIntOrNull()?.coerceIn(0, 23) ?: t.b2HoraInicio,
        b2HoraFim = b2Fim.toIntOrNull()?.coerceIn(0, 23) ?: t.b2HoraFim,
        b2Domingo = b2Dom,
    )

    fun salvar() {
        Repo.salvarTarifas(montar())
        ctx.toast("Tarifas salvas")
        onVoltar()
    }

    Tela("Tarifas", onVoltar = onVoltar, acoes = {
        IconButton(onClick = { salvar() }) { Icon(Icons.Filled.Check, "Salvar") }
    }) {
        Secao("Valores") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Bandeirada", bandeirada, { bandeirada = it }, Modifier.weight(1f), KeyboardType.Decimal, prefixo = "R$ ")
                CampoTexto("Por km", porKm, { porKm = it }, Modifier.weight(1f), KeyboardType.Decimal, prefixo = "R$ ")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Por minuto", porMin, { porMin = it }, Modifier.weight(1f), KeyboardType.Decimal, prefixo = "R$ ")
                CampoTexto("Hora parada", porHora, { porHora = it }, Modifier.weight(1f), KeyboardType.Decimal, prefixo = "R$ ")
            }
            CampoTexto("Valor mínimo", minimo, { minimo = it }, teclado = KeyboardType.Decimal, prefixo = "R$ ")
            Text(
                "• Orçamento: bandeirada + km + minutos de viagem (com trânsito).\n" +
                    "• Taxímetro: bandeirada + km rodado + tempo parado (abaixo de 10 km/h), cobrado pela hora parada.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Secao("Espera do passageiro") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Tempo grátis", esperaGratis, { esperaGratis = it.filter { c -> c.isDigit() }.take(3) }, Modifier.weight(1f), KeyboardType.Number, sufixo = "min")
                CampoTexto("Depois, por minuto", esperaMin, { esperaMin = it }, Modifier.weight(1f), KeyboardType.Decimal, prefixo = "R$ ")
            }
            Text(
                "No taxímetro, toque em “Aguardando passageiro” ao chegar no embarque ou numa parada. " +
                    "O tempo grátis vale para cada espera; passou dele, cada minuto é somado ao valor da corrida.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Secao("Bandeira 2") {
            CampoTexto("Acréscimo", b2Pct, { b2Pct = it }, teclado = KeyboardType.Decimal, sufixo = "%")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Das (hora)", b2Ini, { b2Ini = it.filter { c -> c.isDigit() }.take(2) }, Modifier.weight(1f), KeyboardType.Number, sufixo = "h")
                CampoTexto("Até (hora)", b2Fim, { b2Fim = it.filter { c -> c.isDigit() }.take(2) }, Modifier.weight(1f), KeyboardType.Number, sufixo = "h")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Domingo o dia todo", Modifier.weight(1f))
                Switch(checked = b2Dom, onCheckedChange = { b2Dom = it })
            }
            Text("Use o mesmo horário nos dois campos para desativar o horário noturno.", style = MaterialTheme.typography.bodySmall)
        }

        val ex = Calculo.estimativa(montar(), 10.0, 25.0, false)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "Exemplo: corrida de 10 km e 25 min = ${ex.total.reais()}",
                Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold,
            )
        }

        Button(onClick = { salvar() }, Modifier.fillMaxWidth().height(52.dp)) { Text("Salvar tarifas") }
    }
}

// ------------------------------------------------------------------ Backup

@Composable
private fun TelaBackup(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val clientes by Repo.clientes.collectAsState()
    val corridas by Repo.corridas.collectAsState()
    var pendente by remember { mutableStateOf<String?>(null) }

    fun nomeArquivo() = "backup_corridas_" + SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) + ".json"

    val salvarArquivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(Repo.exportarJson().toByteArray()) }
            }.onSuccess { ctx.toast("Backup salvo") }.onFailure { ctx.toast("Erro ao salvar: ${it.message}") }
        }
    }
    val abrirArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }
                .onSuccess { pendente = it }
                .onFailure { ctx.toast("Não foi possível ler o arquivo") }
        }
    }

    Tela("Backup", onVoltar = onVoltar) {
        Secao("Seus dados") {
            LinhaInfo("Clientes", clientes.size.toString())
            LinhaInfo("Corridas (agenda + histórico)", corridas.size.toString())
        }
        Secao("Fazer backup") {
            Text("Gera um arquivo com clientes, endereços, corridas, tarifas e configurações.", style = MaterialTheme.typography.bodySmall)
            BotaoAcao("Salvar arquivo no celular", Icons.Filled.SaveAlt, { salvarArquivo.launch(nomeArquivo()) }, Modifier.fillMaxWidth(), principal = true)
            BotaoAcao("Enviar para Drive, e-mail ou WhatsApp", Icons.Filled.Share, {
                val f = File(Compartilhar.pastaCompartilhar(ctx), nomeArquivo())
                f.writeText(Repo.exportarJson())
                Compartilhar.compartilharArquivo(ctx, f, "application/json", titulo = "Enviar backup")
            }, Modifier.fillMaxWidth())
        }
        Secao("Restaurar") {
            Text(
                "Atenção: restaurar substitui todos os dados atuais pelos do arquivo.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
            BotaoAcao("Importar arquivo de backup", Icons.Filled.Restore, {
                abrirArquivo.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            }, Modifier.fillMaxWidth())
        }
    }

    pendente?.let { texto ->
        Confirmar(
            "Restaurar backup?",
            "Os dados atuais serão substituídos.",
            "Restaurar",
            onConfirmar = {
                pendente = null
                Repo.importarJson(texto)
                    .onSuccess { n -> Lembrete.reagendarTodas(ctx); ctx.toast("Backup restaurado: $n cliente(s)") }
                    .onFailure { ctx.toast(it.message ?: "Arquivo inválido") }
            },
            onCancelar = { pendente = null },
        )
    }
}
