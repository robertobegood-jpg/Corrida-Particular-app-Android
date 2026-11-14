package com.corridaparticular.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.ui.theme.Visor
import com.corridaparticular.app.util.PT_BR
import com.corridaparticular.app.util.duracaoTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.minutosTexto
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.flow.update

@Composable
fun TelaTaximetro(state: AppState) {
    val d by TaximetroState.dados.collectAsState()
    val tarifas by Repo.tarifas.collectAsState()
    when {
        d.ativo -> Tela(
            titulo = if (d.pausado) "Corrida *pausada*." else if (d.esperando) "Aguardando *passageiro*." else "Em *corrida*.",
            eyebrow = "01/Taxímetro · " + if (d.bandeira2) "Bandeira 2" else "Bandeira 1",
        ) { Rodando(d) }
        d.finalizado -> Tela(
            titulo = "Corrida *finalizada*.",
            eyebrow = "01/Taxímetro · Recibo",
            subtitulo = d.clienteNome.ifBlank { null },
        ) { Resumo(d, state) }
        else -> Tela(
            titulo = "Pronto para *rodar*.",
            eyebrow = "01/Taxímetro",
            subtitulo = "Bandeirada ${tarifas.bandeirada.reais()} · ${tarifas.porKm.reais()}/km · ${tarifas.porHoraParada.reais()}/h parada.",
        ) { Preparar(state) }
    }
}

/* ───────────── Visor ───────────── */

/** Número sem o "R$", para o visor: 1.234,56 */
private fun numero(v: Double): String = String.format(PT_BR, "%,.2f", v)

/** Diminui a fonte do valor conforme o número cresce, para caber em telas estreitas. */
private fun tamanhoValor(texto: String, base: Int): TextUnit = when {
    texto.length <= 6 -> base.sp
    texto.length <= 8 -> (base * 0.84f).sp
    else -> (base * 0.7f).sp
}

/**
 * Visor do taxímetro: painel escuro fixo (igual nos dois temas), com marcas de canto,
 * o valor em amarelo táxi e os indicadores logo abaixo.
 */
@Composable
private fun VisorTaximetro(
    status: String,
    statusCor: androidx.compose.ui.graphics.Color,
    direita: String,
    valor: Double,
    apagado: Boolean,
    indicadores: List<Pair<String, String>>,
    rodape: String? = null,
) {
    val t = Oficina.tipos
    Moldura(fundo = Visor.fundo, linha = Visor.linha, marcas = Oficina.cores.lineStrong) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PontoStatus(statusCor, tamanho = 6.dp)
            Spacer(Modifier.width(8.dp))
            Hud(status, cor = Visor.apagado, modifier = Modifier.weight(1f))
            Hud(direita, cor = Visor.amarelo)
        }
        Spacer(Modifier.height(14.dp))
        val n = numero(valor)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("R$", style = t.heading.copy(fontSize = 22.sp, lineHeight = 26.sp), color = if (apagado) Visor.apagado else Visor.amarelo)
            Spacer(Modifier.width(8.dp))
            Text(
                n,
                style = t.displayLg.copy(fontSize = tamanhoValor(n, 64), lineHeight = tamanhoValor(n, 66)),
                color = if (apagado) Visor.apagado else Visor.amarelo,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Visor.linha))
        Spacer(Modifier.height(14.dp))
        indicadores.chunked(2).forEachIndexed { i, par ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                par.forEach { (rotulo, v) ->
                    Column(Modifier.weight(1f)) {
                        Hud(rotulo, cor = Visor.apagado)
                        Spacer(Modifier.height(2.dp))
                        Text(v, style = t.heading.copy(fontSize = 22.sp, lineHeight = 28.sp), color = Visor.tinta, maxLines = 1)
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (rodape != null) {
            Spacer(Modifier.height(14.dp))
            Text(rodape, style = t.bodySm, color = Visor.apagado)
        }
    }
}

/* ───────────── Preparar ───────────── */

@Composable
private fun Preparar(state: AppState) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    val tarifas by Repo.tarifas.collectAsState()
    val prep = state.taxiPreparo
    var clienteId by remember(prep) { mutableStateOf(prep.clienteId) }
    var nome by remember(prep) { mutableStateOf(prep.nome) }
    var fone by remember(prep) { mutableStateOf(prep.telefone) }
    val sugestaoB2 = remember(prep, tarifas) { Calculo.ehBandeira2(tarifas, System.currentTimeMillis()) }
    var bandeira2 by remember(prep) { mutableStateOf(sugestaoB2) }
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoStatus(c.accent, tamanho = 6.dp)
                Spacer(Modifier.width(8.dp))
                Hud("Corrida agendada", cor = c.accentInk)
            }
            if (prep.origem.texto.isNotBlank()) {
                Text("Saída: ${prep.origem.texto}", style = Oficina.tipos.bodySm, color = c.muted)
            }
            Spacer(Modifier.height(4.dp))
            LinhaSangrada()
        }
    }

    Secao("Cliente") {
        SeletorCliente(clienteId) { cli ->
            clienteId = cli?.id
            if (cli == null) { nome = ""; fone = "" }
        }
        if (clienteId == null) {
            CampoTexto("Nome do passageiro (opcional)", nome, { nome = it })
            CampoTexto("WhatsApp (opcional)", fone, { fone = it }, teclado = KeyboardType.Phone)
        }
    }

    Secao("Espera do passageiro") {
        LinhaMarcavel(
            texto = "Já estou no local, aguardando",
            marcado = comecarEsperando,
            onAlternar = { comecarEsperando = it },
            subtitulo = "${tarifas.esperaGratisMin} min grátis, depois ${tarifas.esperaPorMinuto.reais()}/min.",
        )
    }

    Secao("Bandeira") {
        Segmentado(
            opcoes = listOf(false, true),
            selecionada = bandeira2,
            rotulo = { if (it) "Bandeira 2 · +${tarifas.bandeira2Pct.toInt()}%" else "Bandeira 1" },
            onSelecionar = { bandeira2 = it },
        )
        Text(
            if (sugestaoB2) "Sugerida a bandeira 2 pelo horário ou dia." else "Sugerida a bandeira 1 pelo horário.",
            style = Oficina.tipos.bodySm, color = c.muted,
        )
    }

    BotaoPrimario(
        "Iniciar corrida",
        onClick = {
            val falta = permissoes.any { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
            if (falta) pedir.launch(permissoes) else iniciar()
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        seta = true,
    )

    Text(
        "O taxímetro continua contando enquanto você usa as outras abas, com a tela bloqueada ou em outro app. " +
            "O valor aparece também na barra de notificações.",
        style = Oficina.tipos.bodySm,
        color = c.muted,
    )
}

/* ───────────── Rodando ───────────── */

@Composable
private fun Rodando(d: TaxiDados) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    var confirmarFim by remember { mutableStateOf(false) }

    VisorTaximetro(
        status = when {
            d.pausado -> "Pausado"
            d.gpsOk -> "GPS ok" + if (d.precisao > 0) " · ±${d.precisao.toInt()} m" else ""
            else -> "Procurando GPS…"
        },
        statusCor = when {
            d.pausado -> c.ambar
            d.gpsOk -> Visor.verde
            else -> Visor.vermelho
        },
        direita = if (d.bandeira2) "Bandeira 2" else "Bandeira 1",
        valor = d.valor,
        apagado = d.pausado,
        indicadores = listOf(
            "Distância" to d.km.kmTexto(),
            "Tempo" to duracaoTexto(d.msTotal),
            "Parado" to duracaoTexto(d.msParado),
            "Velocidade" to "${d.velocidadeKmh.toInt()} km/h",
        ),
        rodape = d.clienteNome.ifBlank { null },
    )

    PainelEspera(d)

    PainelNavegacao(d)

    Secao("Bandeira") {
        Segmentado(
            opcoes = listOf(false, true),
            selecionada = d.bandeira2,
            rotulo = { if (it) "Bandeira 2" else "Bandeira 1" },
            onSelecionar = { TaximetroState.definirBandeira2(ctx, it) },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (d.pausado) {
            BotaoPrimario("Retomar", { TaximetroState.comando(ctx, TaximetroService.ACAO_RETOMAR) }, Modifier.weight(1f).height(56.dp))
        } else {
            BotaoSecundario("Pausar", { TaximetroState.comando(ctx, TaximetroService.ACAO_PAUSAR) }, Modifier.weight(1f).height(56.dp))
        }
        BotaoSecundario("Finalizar", { confirmarFim = true }, Modifier.weight(1f).height(56.dp), perigo = true)
    }

    if (confirmarFim) {
        Confirmar(
            titulo = "Finalizar corrida?",
            texto = "Valor atual: ${d.valor.reais()}",
            confirmar = "Finalizar",
            perigo = true,
            onConfirmar = { confirmarFim = false; TaximetroState.comando(ctx, TaximetroService.ACAO_FINALIZAR) },
            onCancelar = { confirmarFim = false },
        )
    }
}

@Composable
private fun PainelEspera(d: TaxiDados) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    val t by Repo.tarifas.collectAsState()
    val cobradoTotal = Calculo.valorEspera(t, d.msEsperaCobrada(t))

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hud("Espera do passageiro", modifier = Modifier.weight(1f))
            if (cobradoTotal > 0) Hud("+ ${cobradoTotal.reais()}", cor = c.ink)
        }
        if (d.esperando) {
            val restante = d.msGratisRestante(t)
            Moldura {
                Hud(if (restante > 0) "Tempo grátis" else "Cobrando", cor = if (restante > 0) c.verdeInk else c.ambarInk)
                Spacer(Modifier.height(4.dp))
                Text(duracaoTexto(d.msEsperaSessao), style = Oficina.tipos.displayMd, color = c.inkStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (restante > 0) "Grátis por mais ${duracaoTexto(restante).removePrefix("00:")}. Depois, ${t.esperaPorMinuto.reais()} por minuto."
                    else "Cobrando ${t.esperaPorMinuto.reais()} por minuto.",
                    style = Oficina.tipos.bodySm, color = c.muted,
                )
            }
            BotaoPrimario(
                "Passageiro embarcou",
                { TaximetroState.definirEspera(ctx, false) },
                Modifier.fillMaxWidth(), seta = true,
            )
        } else {
            Text(
                "Use ao chegar no embarque ou numa parada. ${t.esperaGratisMin} min grátis; depois ${t.esperaPorMinuto.reais()}/min, somado automaticamente.",
                style = Oficina.tipos.bodySm, color = c.muted,
            )
            BotaoSecundario(
                "Aguardando passageiro",
                { TaximetroState.definirEspera(ctx, true) },
                Modifier.fillMaxWidth(), habilitado = !d.pausado,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinhaSangrada()
    }
}

@Composable
private fun PainelNavegacao(d: TaxiDados) {
    val c = Oficina.cores
    var destino by remember(d.inicio) { mutableStateOf(d.destino) }

    // Embarque, paradas programadas e destino (da agenda ou do orçamento)
    val pontos = pontosDaCorrida(d.origem, d.paradas, destino)

    Secao("Navegação") {
        if (d.origem.texto.isNotBlank()) Trajeto("Embarque", d.origem.texto)
        d.paradas.forEachIndexed { i, p -> Trajeto("Parada ${i + 1}", p.texto) }
        CampoEndereco(
            "Destino", destino,
            { novo ->
                destino = novo
                TaximetroState.dados.update { it.copy(destino = novo) }
            },
            icone = Icons.Filled.Flag,
        )
        BotaoNavegar(pontos, Modifier.fillMaxWidth(), texto = "Abrir no GPS · Waze, Maps", principal = false)
    }
}

/** Linha "EMBARQUE  Rua tal, 123". */
@Composable
private fun Trajeto(rotulo: String, texto: String) {
    val c = Oficina.cores
    Row(verticalAlignment = Alignment.Top) {
        Hud(rotulo, modifier = Modifier.width(96.dp).padding(top = 3.dp))
        Text(texto, style = Oficina.tipos.bodySm, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/* ───────────── Resumo ───────────── */

@Composable
private fun Resumo(d: TaxiDados, state: AppState) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    val agendada = remember(d.inicio) { Repo.corrida(d.agendadaId) }
    var origem by remember(d.inicio) { mutableStateOf(agendada?.origem ?: d.origem) }
    var destino by remember(d.inicio) { mutableStateOf(d.destino.takeIf { it.texto.isNotBlank() } ?: agendada?.destino ?: Endereco()) }
    var obs by remember(d.inicio) { mutableStateOf("") }
    val tarifasAtual by Repo.tarifas.collectAsState()
    var salvo by remember(d.inicio) {
        mutableStateOf(Repo.corrida(d.agendadaId ?: "taxi_${d.inicio}")?.tipo == Corrida.TIPO_TAXIMETRO)
    }
    val cobrada = Calculo.valorEspera(tarifasAtual, d.msEsperaCobradaFechada)

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
            valorEspera = cobrada,
            valor = d.valor,
            bandeira2 = d.bandeira2,
            status = Corrida.STATUS_CONCLUIDA,
            tipo = Corrida.TIPO_TAXIMETRO,
            obs = obs.ifBlank { base.obs },
        )
    }

    VisorTaximetro(
        status = "Corrida finalizada",
        statusCor = Visor.verde,
        direita = if (d.bandeira2) "Bandeira 2" else "Bandeira 1",
        valor = d.valor,
        apagado = false,
        indicadores = buildList {
            add("Distância" to d.km.kmTexto())
            add("Duração" to minutosTexto(d.msTotal / 60_000.0))
            add("Parado" to minutosTexto(d.msParado / 60_000.0))
            if (d.msEsperaTotal > 0) add("Espera" to minutosTexto(d.msEsperaTotal / 60_000.0))
        },
        rodape = if (d.msEsperaTotal > 0) {
            if (cobrada > 0) "Espera cobrada: ${cobrada.reais()} (já incluída)." else "Espera dentro do tempo grátis."
        } else null,
    )

    Secao("Trajeto · para o recibo") {
        CampoEndereco("Origem", origem, { origem = it }, icone = Icons.Filled.TripOrigin)
        CampoEndereco("Destino", destino, { destino = it }, localAtual = true, icone = Icons.Filled.Flag)
        CampoTexto("Observação (opcional)", obs, { obs = it })
    }

    if (salvo) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PontoStatus(c.verde, tamanho = 6.dp)
            Spacer(Modifier.width(8.dp))
            Hud("Salvo no histórico", cor = c.verdeInk)
        }
    } else {
        BotaoPrimario(
            "Salvar no histórico",
            {
                Lembrete.salvar(ctx, montar())
                salvo = true
                ctx.toast("Corrida salva no histórico")
            },
            Modifier.fillMaxWidth(),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotaoSecundario("Recibo texto", {
            val corrida = montar()
            Compartilhar.whatsappTexto(ctx, corrida.telefone, Compartilhar.texto(corrida, TipoDoc.RECIBO))
        }, Modifier.weight(1f))
        BotaoSecundario("Recibo PDF", {
            Compartilhar.whatsappPdf(ctx, montar(), TipoDoc.RECIBO)
        }, Modifier.weight(1f))
    }
    BotaoTexto(
        if (salvo) "+ Nova corrida" else "Salvar e iniciar nova corrida",
        {
            if (!salvo) Lembrete.salvar(ctx, montar())
            TaximetroState.limpar()
            state.taxiPreparo = TaxiPreparo()
        },
        Modifier.fillMaxWidth(),
    )
}
