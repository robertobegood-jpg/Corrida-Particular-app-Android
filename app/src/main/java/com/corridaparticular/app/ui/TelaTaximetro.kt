@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corridaparticular.app.agenda.Lembrete
import com.corridaparticular.app.data.Calculo
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.net.Localizacao
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.share.TipoDoc
import com.corridaparticular.app.taximetro.TaxiDados
import com.corridaparticular.app.taximetro.TaximetroService
import com.corridaparticular.app.taximetro.TaximetroState
import com.corridaparticular.app.util.duracaoTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.minutosTexto
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.flow.update

@Composable
fun TelaTaximetro(state: AppState) {
    val d by TaximetroState.dados.collectAsState()
    Tela("Taxímetro") {
        when {
            d.ativo -> Rodando(d)
            d.finalizado -> Resumo(d, state)
            else -> Preparar(state)
        }
    }
}

@Composable
private fun Preparar(state: AppState) {
    val ctx = LocalContext.current
    val tarifas by Repo.tarifas.collectAsState()
    val prep = state.taxiPreparo
    var clienteId by remember(prep) { mutableStateOf(prep.clienteId) }
    var nome by remember(prep) { mutableStateOf(prep.nome) }
    var fone by remember(prep) { mutableStateOf(prep.telefone) }
    var bandeira2 by remember(prep) { mutableStateOf(Calculo.ehBandeira2(tarifas, System.currentTimeMillis())) }
    var comecarEsperando by remember(prep) { mutableStateOf(false) }

    fun iniciar() {
        if (!Localizacao.temPermissao(ctx)) {
            ctx.toast("Permita o acesso à localização para usar o taxímetro.")
            return
        }
        if (!Localizacao.gpsLigado(ctx)) {
            ctx.toast("Ligue o GPS (localização) do celular.")
            ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        val cli = Repo.cliente(clienteId)
        TaximetroState.iniciar(
            ctx,
            TaxiDados(
                clienteId = cli?.id,
                clienteNome = cli?.nome ?: nome.trim(),
                telefone = cli?.telefone ?: fone.trim(),
                origem = prep.origem,
                paradas = prep.paradas,
                destino = prep.destino,
                agendadaId = prep.agendadaId,
                bandeira2 = bandeira2,
                esperando = comecarEsperando,
            )
        )
        state.taxiPreparo = TaxiPreparo()
    }

    val permissoes = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { iniciar() }

    if (prep.agendadaId != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "Corrida agendada${if (prep.origem.texto.isNotBlank()) " – saída: ${prep.origem.texto}" else ""}",
                Modifier.padding(12.dp),
            )
        }
    }

    Secao("Cliente") {
        SeletorCliente(clienteId) { c ->
            clienteId = c?.id
            if (c == null) { nome = ""; fone = "" }
        }
        if (clienteId == null) {
            CampoTexto("Nome do passageiro (opcional)", nome, { nome = it })
            CampoTexto("WhatsApp (opcional)", fone, { fone = it }, teclado = KeyboardType.Phone)
        }
    }

    Secao("Espera do passageiro") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Já estou no local, aguardando", fontWeight = FontWeight.SemiBold)
                Text(
                    "${tarifas.esperaGratisMin} min grátis, depois ${tarifas.esperaPorMinuto.reais()}/min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = comecarEsperando, onCheckedChange = { comecarEsperando = it })
        }
    }

    Secao("Tarifa") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (bandeira2) "Bandeira 2 (+${tarifas.bandeira2Pct.toInt()}%)" else "Bandeira 1", fontWeight = FontWeight.SemiBold)
                Text(
                    "Bandeirada ${tarifas.bandeirada.reais()} • ${tarifas.porKm.reais()}/km • ${tarifas.porHoraParada.reais()}/h parada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = bandeira2, onCheckedChange = { bandeira2 = it })
        }
    }

    Button(
        onClick = {
            val falta = permissoes.any {
                androidx.core.content.ContextCompat.checkSelfPermission(ctx, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (falta) pedir.launch(permissoes) else iniciar()
        },
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AmareloTaxi, contentColor = Color.Black),
    ) {
        Icon(Icons.Filled.PlayArrow, null)
        Spacer(Modifier.width(8.dp))
        Text("INICIAR CORRIDA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }

    Text(
        "O taxímetro continua contando enquanto você usa as outras abas, com a tela bloqueada ou em outro app. " +
            "O valor aparece também na barra de notificações.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Rodando(d: TaxiDados) {
    val ctx = LocalContext.current
    var confirmarFim by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PainelEscuro),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (d.gpsOk) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, null,
                    tint = if (d.gpsOk) Color(0xFF81C784) else Color(0xFFE57373), modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        d.pausado -> "PAUSADO"
                        d.gpsOk -> "GPS ok" + if (d.precisao > 0) " (±${d.precisao.toInt()} m)" else ""
                        else -> "Procurando sinal de GPS…"
                    },
                    color = Color(0xFFB0B8C4), style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(if (d.bandeira2) "BANDEIRA 2" else "BANDEIRA 1", color = AmareloTaxi, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                d.valor.reais(),
                color = if (d.pausado) Color(0xFF9AA0A6) else AmareloTaxi,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
            )
            if (d.clienteNome.isNotBlank()) Text(d.clienteNome, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Indicador("Distância", d.km.kmTexto(), Modifier.weight(1f))
                Indicador("Tempo", duracaoTexto(d.msTotal), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Indicador("Parado", duracaoTexto(d.msParado), Modifier.weight(1f))
                Indicador("Velocidade", "${d.velocidadeKmh.toInt()} km/h", Modifier.weight(1f))
            }
        }
    }

    PainelEspera(d)

    PainelNavegacao(d)

    Secao("Bandeira") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Usar bandeira 2", Modifier.weight(1f))
            Switch(checked = d.bandeira2, onCheckedChange = { TaximetroState.definirBandeira2(ctx, it) })
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (d.pausado) {
            BotaoAcao("Retomar", Icons.Filled.PlayArrow, { TaximetroState.comando(ctx, TaximetroService.ACAO_RETOMAR) }, Modifier.weight(1f).height(56.dp))
        } else {
            BotaoAcao("Pausar", Icons.Filled.Pause, { TaximetroState.comando(ctx, TaximetroService.ACAO_PAUSAR) }, Modifier.weight(1f).height(56.dp))
        }
        Button(
            onClick = { confirmarFim = true },
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(8.dp)); Text("Finalizar")
        }
    }

    if (confirmarFim) {
        Confirmar(
            titulo = "Finalizar corrida?",
            texto = "Valor atual: ${d.valor.reais()}",
            confirmar = "Finalizar",
            onConfirmar = { confirmarFim = false; TaximetroState.comando(ctx, TaximetroService.ACAO_FINALIZAR) },
            onCancelar = { confirmarFim = false },
        )
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(rotulo.uppercase(), color = Color(0xFF8C96A3), style = MaterialTheme.typography.labelSmall)
        Text(valor, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Resumo(d: TaxiDados, state: AppState) {
    val ctx = LocalContext.current
    val agendada = remember(d.inicio) { Repo.corrida(d.agendadaId) }
    var origem by remember(d.inicio) { mutableStateOf(agendada?.origem ?: d.origem) }
    var destino by remember(d.inicio) { mutableStateOf(d.destino.takeIf { it.texto.isNotBlank() } ?: agendada?.destino ?: Endereco()) }
    var obs by remember(d.inicio) { mutableStateOf("") }
    val tarifasAtual by Repo.tarifas.collectAsState()
    var salvo by remember(d.inicio) {
        mutableStateOf(Repo.corrida(d.agendadaId ?: "taxi_${d.inicio}")?.tipo == Corrida.TIPO_TAXIMETRO)
    }

    fun montar(): Corrida {
        val base = agendada ?: Corrida(
            id = "taxi_${d.inicio}",   // id fixo: salvar de novo atualiza, não duplica
            clienteId = d.clienteId,
            clienteNome = d.clienteNome.ifBlank { "Cliente avulso" },
            telefone = d.telefone,
        )
        return base.copy(
            origem = origem,
            paradas = d.paradas.ifEmpty { base.paradas },
            destino = destino,
            dataHora = d.inicio,
            distanciaKm = d.km,
            minutos = d.msTotal / 60_000.0,
            minutosAtraso = 0.0,
            minutosParado = d.msParado / 60_000.0,
            minutosEspera = d.msEsperaTotal / 60_000.0,
            valorEspera = Calculo.valorEspera(tarifasAtual, d.msEsperaCobradaFechada),
            valor = d.valor,
            bandeira2 = d.bandeira2,
            status = Corrida.STATUS_CONCLUIDA,
            tipo = Corrida.TIPO_TAXIMETRO,
            obs = obs.ifBlank { base.obs },
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = PainelEscuro), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CORRIDA FINALIZADA", color = Color(0xFFB0B8C4), style = MaterialTheme.typography.labelMedium)
            Text(d.valor.reais(), color = AmareloTaxi, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 48.sp)
            if (d.clienteNome.isNotBlank()) Text(d.clienteNome, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Indicador("Distância", d.km.kmTexto(), Modifier.weight(1f))
                Indicador("Duração", minutosTexto(d.msTotal / 60_000.0), Modifier.weight(1f))
                Indicador("Parado", minutosTexto(d.msParado / 60_000.0), Modifier.weight(1f))
            }
            if (d.msEsperaTotal > 0) {
                Spacer(Modifier.height(10.dp))
                val cobrada = Calculo.valorEspera(tarifasAtual, d.msEsperaCobradaFechada)
                Text(
                    "Espera: ${minutosTexto(d.msEsperaTotal / 60_000.0)}" +
                        if (cobrada > 0) " • cobrado ${cobrada.reais()}" else " • dentro do tempo grátis",
                    color = Color(0xFFB0B8C4),
                )
            }
        }
    }

    Secao("Trajeto (para o recibo)") {
        CampoEndereco("Origem", origem, { origem = it }, icone = Icons.Filled.TripOrigin)
        CampoEndereco("Destino", destino, { destino = it }, localAtual = true, icone = Icons.Filled.Flag)
        CampoTexto("Observação (opcional)", obs, { obs = it })
    }

    BotaoAcao(
        if (salvo) "Salvo no histórico" else "Salvar no histórico",
        if (salvo) Icons.Filled.CheckCircle else Icons.Filled.Save,
        {
            Lembrete.salvar(ctx, montar())
            salvo = true
            ctx.toast("Corrida salva no histórico")
        },
        Modifier.fillMaxWidth(), principal = true, habilitado = !salvo,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotaoAcao("Recibo texto", Icons.Filled.Sms, {
            val c = montar()
            Compartilhar.whatsappTexto(ctx, c.telefone, Compartilhar.texto(c, TipoDoc.RECIBO))
        }, Modifier.weight(1f))
        BotaoAcao("Recibo PDF", Icons.Filled.PictureAsPdf, {
            Compartilhar.whatsappPdf(ctx, montar(), TipoDoc.RECIBO)
        }, Modifier.weight(1f))
    }
    TextButton(
        onClick = {
            if (!salvo) Lembrete.salvar(ctx, montar())
            TaximetroState.limpar()
            state.taxiPreparo = TaxiPreparo()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp))
        Text(if (salvo) "Nova corrida" else "Salvar e iniciar nova corrida")
    }
}

@Composable
private fun PainelEspera(d: TaxiDados) {
    val ctx = LocalContext.current
    val t by Repo.tarifas.collectAsState()
    val cobradoTotal = Calculo.valorEspera(t, d.msEsperaCobrada(t))

    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (d.esperando) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.HourglassTop, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Espera do passageiro", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (cobradoTotal > 0) Text("+ ${cobradoTotal.reais()}", fontWeight = FontWeight.Bold)
            }
            if (d.esperando) {
                val restante = d.msGratisRestante(t)
                Text(duracaoTexto(d.msEsperaSessao), fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (restante > 0) "Grátis por mais ${duracaoTexto(restante).removePrefix("00:")}. Depois, ${t.esperaPorMinuto.reais()}/min."
                    else "Cobrando ${t.esperaPorMinuto.reais()} por minuto",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = { TaximetroState.definirEspera(ctx, false) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.DirectionsCar, null); Spacer(Modifier.width(8.dp)); Text("Passageiro embarcou – seguir viagem")
                }
            } else {
                Text(
                    "Use ao chegar no embarque ou numa parada. ${t.esperaGratisMin} min grátis; depois ${t.esperaPorMinuto.reais()}/min, somado automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { TaximetroState.definirEspera(ctx, true) },
                    enabled = !d.pausado,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.HourglassTop, null); Spacer(Modifier.width(8.dp)); Text("Aguardando passageiro")
                }
            }
        }
    }
}

@Composable
private fun PainelNavegacao(d: TaxiDados) {
    var destino by remember(d.inicio) { mutableStateOf(d.destino) }

    // Embarque, paradas programadas e destino (da agenda ou do orçamento)
    val pontos = pontosDaCorrida(d.origem, d.paradas, destino)

    Secao("Navegação") {
        if (d.origem.texto.isNotBlank()) Text("Embarque: ${d.origem.texto}", style = MaterialTheme.typography.bodySmall)
        d.paradas.forEachIndexed { i, p ->
            Text("Parada ${i + 1}: ${p.texto}", style = MaterialTheme.typography.bodySmall)
        }
        CampoEndereco(
            "Destino", destino,
            { novo ->
                destino = novo
                TaximetroState.dados.update { it.copy(destino = novo) }
            },
            icone = Icons.Filled.Flag,
        )
        BotaoNavegar(pontos, Modifier.fillMaxWidth(), texto = "Abrir no GPS (Waze, Maps…)", principal = true)
    }
}
