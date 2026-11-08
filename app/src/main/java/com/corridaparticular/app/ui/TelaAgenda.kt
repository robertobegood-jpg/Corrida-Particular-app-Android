@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.agenda.Lembrete
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.share.TipoDoc
import com.corridaparticular.app.taximetro.TaximetroState
import com.corridaparticular.app.util.diaTitulo
import com.corridaparticular.app.util.horaTexto
import com.corridaparticular.app.util.inicioDoDia
import com.corridaparticular.app.util.inicioDoMes
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.reais
import com.corridaparticular.app.util.toast

@Composable
fun TelaAgenda(state: AppState) {
    val corridas by Repo.corridas.collectAsState()
    var aba by remember { mutableIntStateOf(0) }
    val agora = System.currentTimeMillis()

    val proximas = corridas.filter { it.status == Corrida.STATUS_AGENDADA }.sortedBy { it.dataHora }
    val historico = corridas.filter { it.status != Corrida.STATUS_AGENDADA }.sortedByDescending { it.dataHora }
    val lista = if (aba == 0) proximas else historico

    Tela(
        titulo = "Agenda",
        rolavel = false,
        fab = {
            ExtendedFloatingActionButton(
                text = { Text("Nova corrida") },
                icon = { Icon(Icons.Filled.Add, null) },
                onClick = { state.novoOrcamento() },
            )
        },
    ) {
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Próximas (${proximas.size})") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Histórico") })
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (aba == 1) {
                item { ResumoGanhos(historico, agora) }
            }
            if (lista.isEmpty()) {
                item {
                    Text(
                        if (aba == 0) "Nenhuma corrida agendada.\nFaça um orçamento e toque em “Agendar”."
                        else "Nenhuma corrida no histórico ainda.",
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            var diaAnterior = -1L
            lista.forEach { c ->
                val dia = inicioDoDia(c.dataHora)
                if (dia != diaAnterior) {
                    diaAnterior = dia
                    item(key = "dia_$dia") {
                        Text(
                            c.dataHora.diaTitulo(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item(key = c.id) { CardCorrida(c, state, atrasada = aba == 0 && c.dataHora < agora) }
            }
        }
    }
}

@Composable
private fun ResumoGanhos(historico: List<Corrida>, agora: Long) {
    val concluidas = historico.filter { it.status == Corrida.STATUS_CONCLUIDA }
    val hoje = concluidas.filter { it.dataHora >= inicioDoDia(agora) }
    val mes = concluidas.filter { it.dataHora >= inicioDoMes(agora) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Hoje", style = MaterialTheme.typography.labelMedium)
                Text(hoje.sumOf { it.valor }.reais(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${hoje.size} corrida(s)", style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text("Este mês", style = MaterialTheme.typography.labelMedium)
                Text(mes.sumOf { it.valor }.reais(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${mes.size} corrida(s) • ${mes.sumOf { it.distanciaKm }.kmTexto()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CardCorrida(c: Corrida, state: AppState, atrasada: Boolean) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf(false) }
    var cancelar by remember { mutableStateOf(false) }
    val taxiAtivo = TaximetroState.dados.collectAsState().value.ativo

    val corStatus = when (c.status) {
        Corrida.STATUS_CONCLUIDA -> MaterialTheme.colorScheme.tertiary
        Corrida.STATUS_CANCELADA -> MaterialTheme.colorScheme.error
        else -> if (atrasada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.dataHora.horaTexto(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = corStatus)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.clienteNome.ifBlank { "Cliente avulso" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when (c.status) {
                            Corrida.STATUS_CONCLUIDA -> if (c.tipo == Corrida.TIPO_TAXIMETRO) "Concluída • taxímetro" else "Concluída"
                            Corrida.STATUS_CANCELADA -> "Cancelada"
                            else -> if (atrasada) "Horário já passou" else "Agendada"
                        },
                        style = MaterialTheme.typography.bodySmall, color = corStatus,
                    )
                }
                Text(c.valor.reais(), fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (c.status == Corrida.STATUS_AGENDADA) {
                            DropdownMenuItem(
                                text = { Text("Iniciar taxímetro") },
                                leadingIcon = { Icon(Icons.Filled.LocalTaxi, null) },
                                enabled = !taxiAtivo,
                                onClick = {
                                    menu = false
                                    state.abrirTaximetro(TaxiPreparo(c.clienteId, c.clienteNome, c.telefone, c.origem, c.paradas, c.destino, c.id))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Marcar como concluída") },
                                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                                onClick = { menu = false; Lembrete.salvar(ctx, c.copy(status = Corrida.STATUS_CONCLUIDA)) },
                            )
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { menu = false; state.editarCorrida(c) },
                            )
                            DropdownMenuItem(
                                text = { Text("Enviar confirmação (texto)") },
                                leadingIcon = { Icon(Icons.Filled.Sms, null) },
                                onClick = { menu = false; Compartilhar.whatsappTexto(ctx, c.telefone, Compartilhar.texto(c, TipoDoc.CONFIRMACAO)) },
                            )
                            DropdownMenuItem(
                                text = { Text("Enviar confirmação (PDF)") },
                                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
                                onClick = { menu = false; Compartilhar.whatsappPdf(ctx, c, TipoDoc.CONFIRMACAO) },
                            )
                            DropdownMenuItem(
                                text = { Text("Cancelar corrida") },
                                leadingIcon = { Icon(Icons.Filled.Cancel, null) },
                                onClick = { menu = false; cancelar = true },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Recibo (texto)") },
                                leadingIcon = { Icon(Icons.Filled.Sms, null) },
                                onClick = { menu = false; Compartilhar.whatsappTexto(ctx, c.telefone, Compartilhar.texto(c, TipoDoc.RECIBO)) },
                            )
                            DropdownMenuItem(
                                text = { Text("Recibo (PDF)") },
                                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
                                onClick = { menu = false; Compartilhar.whatsappPdf(ctx, c, TipoDoc.RECIBO) },
                            )
                            DropdownMenuItem(
                                text = { Text("Repetir esta corrida") },
                                leadingIcon = { Icon(Icons.Filled.Replay, null) },
                                onClick = {
                                    menu = false
                                    state.editarCorrida(c)
                                    state.form.editandoId = null
                                    state.form.criadoEm = null
                                    state.form.dataHora = null
                                    state.form.resultado = null
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Excluir") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = { menu = false; excluir = true },
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Trecho(Icons.Filled.TripOrigin, c.origem.texto)
            c.paradas.forEach { Trecho(Icons.Filled.AddLocation, it.texto) }
            Trecho(Icons.Filled.Flag, c.destino.texto)
            if (c.status == Corrida.STATUS_AGENDADA) {
                val pontos = pontosDaCorrida(c.origem, c.paradas, c.destino)
                if (pontos.isNotEmpty()) {
                    BotaoNavegar(pontos, Modifier.fillMaxWidth().padding(top = 8.dp, end = 10.dp))
                }
            }
            if (c.distanciaKm > 0) {
                Text(
                    c.distanciaKm.kmTexto() + (if (c.bandeira2) " • bandeira 2" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (excluir) {
        Confirmar("Excluir corrida?", "Esta ação não pode ser desfeita.", "Excluir",
            onConfirmar = { excluir = false; Lembrete.excluir(ctx, c.id) },
            onCancelar = { excluir = false })
    }
    if (cancelar) {
        Confirmar("Cancelar corrida?", "A corrida vai para o histórico como cancelada.", "Cancelar corrida",
            onConfirmar = {
                cancelar = false
                Lembrete.salvar(ctx, c.copy(status = Corrida.STATUS_CANCELADA))
                ctx.toast("Corrida cancelada")
            },
            onCancelar = { cancelar = false })
    }
}

@Composable
private fun Trecho(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    if (texto.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Icon(icone, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
