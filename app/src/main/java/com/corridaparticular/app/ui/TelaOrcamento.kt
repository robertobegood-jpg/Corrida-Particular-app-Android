@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.corridaparticular.app.net.TomTom
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.share.TipoDoc
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.util.PT_BR
import com.corridaparticular.app.util.dataHoraTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.minutosTexto
import com.corridaparticular.app.util.paraDouble
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.launch

@Composable
fun TelaOrcamento(state: AppState) {
    val ctx = LocalContext.current
    val form = state.form
    val scope = rememberCoroutineScope()
    val clientes by Repo.clientes.collectAsState()
    val tarifas by Repo.tarifas.collectAsState()
    val cliente = clientes.find { it.id == form.clienteId }
    var calculando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var limpar by remember { mutableStateOf(false) }

    fun mudou() { form.resultado = null; erro = null }

    fun calcular() {
        erro = null
        calculando = true
        scope.launch {
            try {
                val quando = form.dataHora ?: System.currentTimeMillis()
                val b2 = Calculo.ehBandeira2(tarifas, quando)
                if (form.manual) {
                    val km = form.kmManual.paraDouble() ?: error("Informe a distância em km.")
                    val min = form.minManual.paraDouble() ?: error("Informe o tempo em minutos.")
                    val n = form.paradas.count { it.texto.isNotBlank() }
                    val det = Calculo.estimativa(tarifas, km, min, b2)
                    form.resultado = Resultado(km, min, min, 0.0, det.total, b2, n)
                } else {
                    val chave = Repo.config.value.chaveTomTom.trim()
                    if (chave.isBlank()) error("Cadastre a chave TomTom em Mais › Configurações ou use o cálculo manual.")
                    if (form.origem.texto.isBlank()) error("Informe a origem.")
                    if (form.destino.texto.isBlank()) error("Informe o destino.")
                    val loc = Localizacao.ultima(ctx)

                    suspend fun localizar(e: Endereco): Endereco {
                        if (e.temCoord) return e
                        val achado = TomTom.buscar(chave, e.texto, loc?.latitude, loc?.longitude).firstOrNull()
                            ?: error("Endereço não encontrado: ${e.texto}")
                        return achado.copy(apelido = e.apelido)
                    }

                    val o = localizar(form.origem)
                    val ps = form.paradas.filter { it.texto.isNotBlank() }.map { localizar(it) }
                    val de = localizar(form.destino)
                    form.origem = o
                    form.paradas.clear(); form.paradas.addAll(ps)
                    form.destino = de

                    val r = TomTom.rota(chave, listOf(o) + ps + de, quando)
                    val det = Calculo.estimativa(tarifas, r.km, r.minutos, b2)
                    form.resultado = Resultado(r.km, r.minutos, r.minutosSemTransito, r.minutosAtraso, det.total, b2, ps.size)
                }
            } catch (e: Exception) {
                erro = e.message ?: "Não foi possível calcular."
            } finally {
                calculando = false
            }
        }
    }

    fun agendar() {
        val salvarAgora = {
            val c = form.paraCorrida(Corrida.STATUS_AGENDADA)
            if (c != null) {
                Lembrete.salvar(ctx, c)
                ctx.toast(if (form.editandoId != null) "Corrida atualizada" else "Corrida agendada para ${c.dataHora.dataHoraTexto()}")
                form.limpar()
                state.aba = Aba.AGENDA
            }
        }
        if (form.dataHora == null) {
            escolherDataHora(ctx, null) { t ->
                form.dataHora = t
                // recalcula o valor para o novo horário (trânsito e bandeira podem mudar)
                val r = form.resultado
                if (r != null) {
                    val b2 = Calculo.ehBandeira2(tarifas, t)
                    val det = Calculo.estimativa(tarifas, r.km, r.minutos, b2)
                    form.resultado = r.copy(bandeira2 = b2, valor = det.total)
                }
                salvarAgora()
            }
        } else salvarAgora()
    }

    val c = Oficina.cores
    Tela(
        titulo = if (form.editandoId != null) "Editar *corrida*." else "Quanto *custa*?",
        eyebrow = "02/Orçamento",
        acoes = { BotaoTexto("Limpar", { limpar = true }, cor = c.muted) },
    ) {
        Secao("Cliente") {
            SeletorCliente(form.clienteId) { cli -> form.aplicarCliente(cli); if (cli == null) { form.nome = ""; form.telefone = "" }; mudou() }
            if (cliente == null) {
                CampoTexto("Nome do cliente", form.nome, { form.nome = it })
                CampoTexto("WhatsApp", form.telefone, { form.telefone = it }, teclado = KeyboardType.Phone, ajuda = "Com DDD. Ex.: 11 98765-4321")
            }
        }

        Secao("Trajeto") {
            CampoEndereco(
                "Origem", form.origem, { form.origem = it; mudou() },
                sugestoes = cliente?.origens.orEmpty(), localAtual = true, icone = Icons.Filled.TripOrigin,
            )
            form.paradas.forEachIndexed { i, p ->
                key(i) {
                    CampoEndereco(
                        "Parada ${i + 1}", p, { form.paradas[i] = it; mudou() },
                        icone = Icons.Filled.AddLocation,
                        onRemover = { form.paradas.removeAt(i); mudou() },
                    )
                }
            }
            CampoEndereco(
                "Destino", form.destino, { form.destino = it; mudou() },
                sugestoes = cliente?.destinos.orEmpty(), icone = Icons.Filled.Flag,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotaoTexto("+ Parada", { form.paradas.add(Endereco()); mudou() })
                Spacer(Modifier.weight(1f))
                BotaoTexto("⇅ Inverter", {
                    val o = form.origem; form.origem = form.destino; form.destino = o
                    form.paradas.reverse(); mudou()
                }, cor = c.ink)
            }
            val pontosNav = pontosDaCorrida(form.origem, form.paradas.toList(), form.destino)
            if (pontosNav.isNotEmpty()) {
                BotaoNavegar(pontosNav, Modifier.fillMaxWidth(), texto = "Abrir no GPS · Waze, Maps")
            }
            if (cliente != null && form.destino.texto.isNotBlank() && cliente.destinos.none { it.texto == form.destino.texto }) {
                BotaoTexto("★ Salvar destino no cadastro do cliente", {
                    Repo.salvarCliente(cliente.copy(destinos = cliente.destinos + form.destino))
                    ctx.toast("Destino salvo no cadastro de ${cliente.nome}")
                })
            }
        }

        Secao("Quando") {
            Segmentado(
                opcoes = listOf(false, true),
                selecionada = form.dataHora != null,
                rotulo = { agendado -> if (!agendado) "Agora" else form.dataHora?.dataHoraTexto() ?: "Escolher horário" },
                onSelecionar = { agendado ->
                    if (agendado) escolherDataHora(ctx, form.dataHora) { form.dataHora = it; mudou() }
                    else { form.dataHora = null; mudou() }
                },
            )
            Text(
                "O tempo de viagem usa a previsão de trânsito da TomTom para o horário escolhido.",
                style = Oficina.tipos.bodySm, color = c.muted,
            )
        }

        Secao("Cálculo manual") {
            LinhaMarcavel(
                texto = "Informar km e tempo manualmente",
                marcado = form.manual,
                onAlternar = { form.manual = it; mudou() },
                subtitulo = "Para quando estiver sem internet ou sem a chave TomTom.",
            )
            if (form.manual) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto("Distância", form.kmManual, { form.kmManual = it; mudou() }, Modifier.weight(1f), KeyboardType.Decimal, sufixo = "km")
                    CampoTexto("Tempo", form.minManual, { form.minManual = it; mudou() }, Modifier.weight(1f), KeyboardType.Number, sufixo = "min")
                }
            }
        }

        CampoTexto("Observação para o cliente (opcional)", form.obs, { form.obs = it }, linhas = 2)

        BotaoPrimario(
            if (calculando) "Calculando…" else "Calcular corrida",
            { calcular() },
            Modifier.fillMaxWidth().height(56.dp),
            habilitado = !calculando,
            seta = !calculando,
        )

        erro?.let {
            Row(verticalAlignment = Alignment.Top) {
                PontoStatus(c.danger, Modifier.padding(top = 7.dp), tamanho = 6.dp)
                Spacer(Modifier.width(8.dp))
                Text(it, style = Oficina.tipos.bodySm, color = c.danger)
            }
        }

        form.resultado?.let { r -> CardResultado(state, r, onAgendar = { agendar() }) }
    }

    if (limpar) {
        Confirmar(
            "Limpar orçamento?", "Todos os campos serão apagados.", "Limpar", perigo = true,
            onConfirmar = { form.limpar(); erro = null; limpar = false },
            onCancelar = { limpar = false },
        )
    }
}

@Composable
private fun CardResultado(state: AppState, r: Resultado, onAgendar: () -> Unit) {
    val ctx = LocalContext.current
    val form = state.form
    val tarifas by Repo.tarifas.collectAsState()
    val det = Calculo.estimativa(tarifas, r.km, r.minutos, r.bandeira2)

    val c = Oficina.cores
    Moldura {
        Hud("Valor estimado")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("R$", style = Oficina.tipos.heading.copy(fontSize = 20.sp, lineHeight = 24.sp), color = c.muted)
            Spacer(Modifier.width(8.dp))
            Text(String.format(PT_BR, "%,.2f", r.valor), style = Oficina.tipos.displayLg, color = c.inkStrong, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Hud(
            "${r.km.kmTexto()} · ${minutosTexto(r.minutos)} · " + if (r.bandeira2) "Bandeira 2" else "Bandeira 1",
            maxLinhas = 2,
        )
        Spacer(Modifier.height(16.dp))
        Linha()
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinhaInfo("Distância", r.km.kmTexto())
            LinhaInfo("Tempo com trânsito", minutosTexto(r.minutos))
            if (r.minutosAtraso >= 1) LinhaInfo("Atraso por trânsito", "+" + minutosTexto(r.minutosAtraso))
            if (r.minutosSemTransito > 0 && r.minutosAtraso >= 1) LinhaInfo("Sem trânsito seria", minutosTexto(r.minutosSemTransito))
        }
        Spacer(Modifier.height(12.dp))
        Linha()
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinhaInfo("Bandeirada", det.bandeirada.reais())
            LinhaInfo("Km · ${tarifas.porKm.reais()}/km", det.km.reais())
            LinhaInfo("Tempo · ${tarifas.porMinuto.reais()}/min", det.tempo.reais())
            if (det.adicionalB2 > 0) LinhaInfo("Adicional bandeira 2", det.adicionalB2.reais())
            if (det.aplicouMinimo) LinhaInfo("Valor mínimo aplicado", tarifas.valorMinimo.reais())
        }
        Spacer(Modifier.height(12.dp))
        Linha()
        Spacer(Modifier.height(12.dp))
        Text(
            "Espera: ${tarifas.esperaGratisMin} min grátis no embarque e em cada parada; " +
                "depois ${tarifas.esperaPorMinuto.reais()}/min, somado ao valor final.",
            style = Oficina.tipos.bodySm, color = c.muted,
        )
    }

    val previa = form.paraCorrida(Corrida.STATUS_AGENDADA)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotaoAcao("WhatsApp", Icons.Filled.Sms, {
            previa?.let { Compartilhar.whatsappTexto(ctx, it.telefone, Compartilhar.texto(it, TipoDoc.ORCAMENTO)) }
        }, Modifier.weight(1f))
        BotaoAcao("Enviar PDF", Icons.Filled.PictureAsPdf, {
            previa?.let { Compartilhar.whatsappPdf(ctx, it, TipoDoc.ORCAMENTO) }
        }, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotaoAcao(
            if (form.editandoId != null) "Salvar" else "Agendar",
            Icons.Filled.EventAvailable, onAgendar, Modifier.weight(1f), principal = true,
        )
        BotaoAcao("Taxímetro", Icons.Filled.LocalTaxi, {
            val cli = Repo.cliente(form.clienteId)
            state.abrirTaximetro(
                TaxiPreparo(
                    clienteId = cli?.id,
                    nome = cli?.nome ?: form.nome,
                    telefone = cli?.telefone ?: form.telefone,
                    origem = form.origem,
                    paradas = form.paradas.filter { it.texto.isNotBlank() || it.temCoord },
                    destino = form.destino,
                    agendadaId = form.editandoId,
                )
            )
            form.limpar()
        }, Modifier.weight(1f))
    }
}
