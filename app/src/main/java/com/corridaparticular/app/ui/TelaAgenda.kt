package com.corridaparticular.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corridaparticular.app.agenda.Lembrete
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.share.TipoDoc
import com.corridaparticular.app.taximetro.TaximetroState
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.util.diaTitulo
import com.corridaparticular.app.util.horaTexto
import com.corridaparticular.app.util.inicioDoDia
import com.corridaparticular.app.util.inicioDoMes
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast

@Composable
fun TelaAgenda(state: AppState, historicoInicial: Boolean = false) {
    val c = Oficina.cores
    val corridas by Repo.corridas.collectAsState()
    var historico by remember { mutableStateOf(historicoInicial) }
    val agora = System.currentTimeMillis()

    val proximas = corridas.filter { it.status == Corrida.STATUS_AGENDADA }.sortedBy { it.dataHora }
    val anteriores = corridas.filter { it.status != Corrida.STATUS_AGENDADA }.sortedByDescending { it.dataHora }
    val lista = if (historico) anteriores else proximas

    Tela(
        titulo = if (historico) "Corridas *feitas*." else "Agenda de *corridas*.",
        eyebrow = "03/Agenda",
        acoes = { BotaoTexto("+ Nova", { state.novoOrcamento() }) },
        rolavel = false,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Segmentado(
                    opcoes = listOf(false, true),
                    selecionada = historico,
                    rotulo = { if (it) "Histórico" else "Próximas · ${proximas.size}" },
                    onSelecionar = { historico = it },
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (historico) {
                item { ResumoGanhos(anteriores, agora) }
            }
            if (lista.isEmpty()) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (!historico) "Nenhuma corrida agendada." else "Nenhuma corrida no histórico ainda.",
                            style = Oficina.tipos.body, color = c.ink,
                        )
                        Text(
                            if (!historico) "Faça um orçamento e toque em “Agendar”." else "As corridas concluídas e canceladas aparecem aqui.",
                            style = Oficina.tipos.bodySm, color = c.muted,
                        )
                        if (!historico) {
                            Spacer(Modifier.height(16.dp))
                            BotaoSecundario("Fazer um orçamento", { state.novoOrcamento() }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            var diaAnterior = -1L
            lista.forEach { corrida ->
                val dia = inicioDoDia(corrida.dataHora)
                if (dia != diaAnterior) {
                    diaAnterior = dia
                    item(key = "dia_$dia") {
                        Eyebrow(corrida.dataHora.diaTitulo(), Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp))
                    }
                }
                item(key = corrida.id) {
                    LinhaCorrida(corrida, state, atrasada = !historico && corrida.dataHora < agora)
                    Linha()
                }
            }
        }
    }
}

@Composable
private fun ResumoGanhos(historico: List<Corrida>, agora: Long) {
    val c = Oficina.cores
    val concluidas = historico.filter { it.status == Corrida.STATUS_CONCLUIDA }
    val hoje = concluidas.filter { it.dataHora >= inicioDoDia(agora) }
    val mes = concluidas.filter { it.dataHora >= inicioDoMes(agora) }
    Moldura(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
        Row {
            Column(Modifier.weight(1f)) {
                Hud("Hoje")
                Text(hoje.sumOf { it.valor }.reais(), style = Oficina.tipos.heading, color = c.inkStrong, maxLines = 1)
                Text("${hoje.size} corrida(s)", style = Oficina.tipos.bodySm, color = c.muted)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Hud("Este mês")
                Text(mes.sumOf { it.valor }.reais(), style = Oficina.tipos.heading, color = c.inkStrong, maxLines = 1)
                Text("${mes.size} corrida(s) · ${mes.sumOf { it.distanciaKm }.kmTexto().replace(' ', '\u00A0')}", style = Oficina.tipos.bodySm, color = c.muted)
            }
        }
    }
}

@Composable
private fun LinhaCorrida(corrida: Corrida, state: AppState, atrasada: Boolean) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    var menu by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf(false) }
    var cancelar by remember { mutableStateOf(false) }
    val taxiAtivo = TaximetroState.dados.collectAsState().value.ativo

    val (status, corStatus, corPonto) = when (corrida.status) {
        Corrida.STATUS_CONCLUIDA -> Triple(if (corrida.tipo == Corrida.TIPO_TAXIMETRO) "Concluída · taxímetro" else "Concluída", c.verdeInk, c.verde)
        Corrida.STATUS_CANCELADA -> Triple("Cancelada", c.danger, c.danger)
        else -> if (atrasada) Triple("Horário passou", c.ambarInk, c.ambar) else Triple("Agendada", c.accentInk, c.accent)
    }

    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            PontoStatus(corPonto, Modifier.padding(top = 9.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(corrida.dataHora.horaTexto(), style = Oficina.tipos.heading.copy(fontSize = 20.sp, lineHeight = 26.sp), color = c.inkStrong)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        corrida.clienteNome.ifBlank { "Cliente avulso" },
                        style = Oficina.tipos.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Hud(status, cor = corStatus, modifier = Modifier.weight(1f))
                    Text(corrida.valor.reais(), style = Oficina.tipos.body, color = c.ink)
                }
            }
            Box {
                Box(
                    Modifier.size(44.dp).clickable(role = Role.Button) { menu = true },
                    contentAlignment = Alignment.Center,
                ) { Text("⋯", style = Oficina.tipos.heading, color = c.ink) }
                MenuCorrida(menu, { menu = false }) {
                    if (corrida.status == Corrida.STATUS_AGENDADA) {
                        if (!taxiAtivo) {
                            ItemMenuCorrida("Iniciar taxímetro") {
                                state.abrirTaximetro(
                                    TaxiPreparo(corrida.clienteId, corrida.clienteNome, corrida.telefone, corrida.origem, corrida.paradas, corrida.destino, corrida.id)
                                )
                            }
                        }
                        ItemMenuCorrida("Marcar como concluída") { Lembrete.salvar(ctx, corrida.copy(status = Corrida.STATUS_CONCLUIDA)) }
                        ItemMenuCorrida("Editar") { state.editarCorrida(corrida) }
                        ItemMenuCorrida("Confirmação por texto") {
                            Compartilhar.whatsappTexto(ctx, corrida.telefone, Compartilhar.texto(corrida, TipoDoc.CONFIRMACAO))
                        }
                        ItemMenuCorrida("Confirmação em PDF") { Compartilhar.whatsappPdf(ctx, corrida, TipoDoc.CONFIRMACAO) }
                        ItemMenuCorrida("Cancelar corrida", c.danger) { cancelar = true }
                    } else {
                        ItemMenuCorrida("Recibo por texto") {
                            Compartilhar.whatsappTexto(ctx, corrida.telefone, Compartilhar.texto(corrida, TipoDoc.RECIBO))
                        }
                        ItemMenuCorrida("Recibo em PDF") { Compartilhar.whatsappPdf(ctx, corrida, TipoDoc.RECIBO) }
                        ItemMenuCorrida("Repetir esta corrida") {
                            state.editarCorrida(corrida)
                            state.form.editandoId = null
                            state.form.criadoEm = null
                            state.form.dataHora = null
                            state.form.resultado = null
                        }
                    }
                    Linha()
                    ItemMenuCorrida("Excluir", c.danger) { excluir = true }
                }
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Trecho("De", corrida.origem.texto)
            corrida.paradas.forEachIndexed { i, p -> Trecho("Parada${if (corrida.paradas.size > 1) " ${i + 1}" else ""}", p.texto) }
            Trecho("Para", corrida.destino.texto)
            if (corrida.distanciaKm > 0) {
                Hud(corrida.distanciaKm.kmTexto() + if (corrida.bandeira2) " · bandeira 2" else "", cor = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
            if (corrida.status == Corrida.STATUS_AGENDADA) {
                val pontos = pontosDaCorrida(corrida.origem, corrida.paradas, corrida.destino)
                if (pontos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    BotaoNavegar(pontos, Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (excluir) {
        Confirmar("Excluir corrida?", "Esta ação não pode ser desfeita.", "Excluir", perigo = true,
            onConfirmar = { excluir = false; Lembrete.excluir(ctx, corrida.id) },
            onCancelar = { excluir = false })
    }
    if (cancelar) {
        Confirmar("Cancelar corrida?", "A corrida vai para o histórico como cancelada.", "Cancelar corrida", perigo = true,
            onConfirmar = {
                cancelar = false
                Lembrete.salvar(ctx, corrida.copy(status = Corrida.STATUS_CANCELADA))
                ctx.toast("Corrida cancelada")
            },
            onCancelar = { cancelar = false })
    }
}

/** Menu suspenso no padrão Oficina: cantos retos, contorno fino. */
@Composable
fun MenuCorrida(aberto: Boolean, onFechar: () -> Unit, itens: @Composable MenuEscopo.() -> Unit) {
    val c = Oficina.cores
    DropdownMenu(
        expanded = aberto,
        onDismissRequest = onFechar,
        shape = RectangleShape,
        containerColor = c.bgRaised,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, c.line),
    ) {
        MenuEscopo(onFechar).itens()
    }
}

class MenuEscopo(val fechar: () -> Unit)

@Composable
fun MenuEscopo.ItemMenuCorrida(texto: String, cor: Color = Oficina.cores.ink, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(texto, style = Oficina.tipos.body, color = cor) },
        onClick = { fechar(); onClick() },
    )
}

@Composable
private fun Trecho(rotulo: String, texto: String) {
    if (texto.isBlank()) return
    Row(verticalAlignment = Alignment.Top) {
        Hud(rotulo, cor = Oficina.cores.faint, modifier = Modifier.width(76.dp).padding(top = 3.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = Oficina.tipos.bodySm, color = Oficina.cores.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
