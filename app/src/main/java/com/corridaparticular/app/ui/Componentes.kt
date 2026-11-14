@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.corridaparticular.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corridaparticular.app.data.Cliente
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.net.Localizacao
import com.corridaparticular.app.net.TomTom
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.util.Navegar
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/**
 * Estrutura de cada tela no padrão Oficina.
 * - Abas principais: eyebrow numerado ("01/Taxímetro") + título display.
 * - Telas internas ([onVoltar] preenchido): "← Voltar", eyebrow e título.
 * O cabeçalho rola junto com o conteúdo; com [rolavel] = false (listas), ele fica fixo.
 */
@Composable
fun Tela(
    titulo: String,
    eyebrow: String,
    onVoltar: (() -> Unit)? = null,
    subtitulo: String? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    rolavel: Boolean = true,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val c = Oficina.cores
    val cabecalho: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            if (onVoltar != null) {
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onVoltar).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Hud("←  Voltar", cor = c.ink) }
                    Spacer(Modifier.weight(1f))
                    acoes()
                    Spacer(Modifier.width(4.dp))
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = if (onVoltar != null) 4.dp else 16.dp, bottom = 16.dp)) {
                if (onVoltar != null) {
                    Hud(eyebrow)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(eyebrow, Modifier.weight(1f))
                        acoes()
                    }
                }
                Spacer(Modifier.height(if (onVoltar != null) 8.dp else 16.dp))
                TituloDisplay(titulo)
                if (subtitulo != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(subtitulo, style = Oficina.tipos.bodyLg, color = c.muted)
                }
            }
            Linha()
        }
    }

    if (rolavel) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            cabecalho()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = conteudo,
            )
        }
    } else {
        Column(Modifier.fillMaxSize().imePadding()) {
            cabecalho()
            conteudo()
        }
    }
}

/** Seção: rótulo HUD + conteúdo, fechada por uma linha fina de ponta a ponta. */
@Composable
fun Secao(titulo: String, modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hud(titulo)
        conteudo()
        Spacer(Modifier.height(6.dp))
        LinhaSangrada()
    }
}

@Composable
fun LinhaInfo(rotulo: String, valor: String, destaque: Boolean = false) {
    val c = Oficina.cores
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = Oficina.tipos.bodySm, color = c.muted, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = if (destaque) Oficina.tipos.heading.copy(fontSize = 18.sp, lineHeight = 24.sp) else Oficina.tipos.bodySm,
            color = if (destaque) c.inkStrong else c.ink,
        )
    }
}

@Composable
fun CampoTexto(
    rotulo: String,
    valor: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text,
    prefixo: String? = null,
    sufixo: String? = null,
    ajuda: String? = null,
    linhas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        modifier = modifier.fillMaxWidth(),
        textStyle = Oficina.tipos.body,
        singleLine = linhas == 1,
        minLines = linhas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        prefix = if (prefixo != null) { { Text(prefixo) } } else null,
        suffix = if (sufixo != null) { { Text(sufixo) } } else null,
        supportingText = if (ajuda != null) { { Text(ajuda) } } else null,
        shape = RectangleShape,
        colors = coresCampo(),
    )
}

/**
 * Campo de endereço com busca na TomTom, atalhos de endereços salvos
 * e opção de usar a localização atual.
 */
@Composable
fun CampoEndereco(
    rotulo: String,
    valor: Endereco,
    onChange: (Endereco) -> Unit,
    modifier: Modifier = Modifier,
    sugestoes: List<Endereco> = emptyList(),
    localAtual: Boolean = false,
    icone: ImageVector = Icons.Filled.Place,
    onRemover: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    val scope = rememberCoroutineScope()
    var resultados by remember { mutableStateOf<List<Endereco>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    fun chave(): String? {
        val k = Repo.config.value.chaveTomTom.trim()
        if (k.isBlank()) erro = "Cadastre a chave TomTom em Mais › Configurações"
        return k.ifBlank { null }
    }

    fun buscar() {
        val k = chave() ?: return
        if (valor.texto.isBlank()) return
        buscando = true
        erro = null
        scope.launch {
            try {
                val loc = Localizacao.ultima(ctx)
                resultados = TomTom.buscar(k, valor.texto, loc?.latitude, loc?.longitude)
                if (resultados.isEmpty()) erro = "Nenhum endereço encontrado"
            } catch (e: Exception) {
                erro = e.message ?: "Falha na busca"
            } finally {
                buscando = false
            }
        }
    }

    fun usarLocal() {
        val k = chave() ?: return
        val loc = Localizacao.ultima(ctx)
        if (loc == null) {
            erro = "Localização indisponível. Ative o GPS e permita o acesso."
            return
        }
        buscando = true
        erro = null
        scope.launch {
            try {
                onChange(TomTom.enderecoDe(k, loc.latitude, loc.longitude))
            } catch (e: Exception) {
                erro = e.message
            } finally {
                buscando = false
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (sugestoes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sugestoes.forEach { s ->
                    Etiqueta(
                        "★ " + s.apelido.ifBlank { s.texto },
                        selecionada = s.texto == valor.texto,
                        onClick = { onChange(s); resultados = emptyList(); erro = null },
                    )
                }
            }
        }
        Box {
            OutlinedTextField(
                value = valor.texto,
                onValueChange = { onChange(Endereco(texto = it)); erro = null },
                label = { Text(rotulo) },
                textStyle = Oficina.tipos.body,
                leadingIcon = { Icon(icone, null, tint = if (valor.temCoord) c.accentInk else c.muted) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (buscando) {
                            CircularProgressIndicator(Modifier.size(22.dp).padding(2.dp), strokeWidth = 2.dp, color = c.ink)
                        } else {
                            IconButton(onClick = { buscar() }) { Icon(Icons.Filled.Search, "Buscar endereço") }
                        }
                        if (localAtual) IconButton(onClick = { usarLocal() }) { Icon(Icons.Filled.MyLocation, "Usar minha localização") }
                        if (onRemover != null) IconButton(onClick = onRemover) { Icon(Icons.Filled.Close, "Remover") }
                    }
                },
                supportingText = {
                    when {
                        erro != null -> Text(erro!!, color = c.danger)
                        valor.temCoord -> Text("✓ Endereço localizado", color = c.verdeInk)
                        valor.texto.isNotBlank() -> Text("Toque na lupa para localizar")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
                shape = RectangleShape,
                colors = coresCampo(),
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = resultados.isNotEmpty(),
                onDismissRequest = { resultados = emptyList() },
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RectangleShape,
                containerColor = c.bgRaised,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, c.line),
            ) {
                resultados.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.texto, style = Oficina.tipos.bodySm, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Filled.Place, null, tint = c.muted) },
                        onClick = { onChange(r); resultados = emptyList() },
                    )
                }
            }
        }
    }
}

/** Seletor de cliente; null = corrida avulsa. */
@Composable
fun SeletorCliente(clienteId: String?, onSelecionar: (Cliente?) -> Unit) {
    val c = Oficina.cores
    val clientes by Repo.clientes.collectAsState()
    var aberto by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("") }
    val atual = clientes.find { it.id == clienteId }

    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bgSunken)
            .border(1.dp, c.line)
            .clickable(role = Role.DropdownList) { aberto = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(atual?.nome ?: "Corrida avulsa", style = Oficina.tipos.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    atual == null -> "Toque para escolher um cliente cadastrado"
                    atual.telefone.isNotBlank() -> atual.telefone
                    else -> "Sem telefone"
                },
                style = Oficina.tipos.bodySm, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("▾", style = Oficina.tipos.body, color = c.muted)
    }

    if (aberto) {
        val lista = clientes.filter { filtro.isBlank() || it.nome.contains(filtro, true) || it.telefone.contains(filtro) }
        DialogoOficina(
            titulo = "Escolher cliente",
            eyebrow = "${clientes.size} cadastrado(s)",
            onDismiss = { aberto = false },
            acoes = { BotaoTexto("Fechar", { aberto = false }, cor = c.muted) },
        ) {
            OutlinedTextField(
                value = filtro, onValueChange = { filtro = it },
                placeholder = { Text("Buscar por nome ou telefone") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = Oficina.tipos.body, shape = RectangleShape, colors = coresCampo(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    LinhaEscolha("Corrida avulsa", "Sem cadastro", atual == null) { onSelecionar(null); aberto = false }
                    Linha()
                }
                items(lista, key = { it.id }) { cli ->
                    LinhaEscolha(cli.nome, cli.telefone, cli.id == clienteId) { onSelecionar(cli); aberto = false }
                    Linha()
                }
            }
        }
    }
}

@Composable
private fun LinhaEscolha(titulo: String, subtitulo: String, marcado: Boolean, onClick: () -> Unit) {
    val c = Oficina.cores
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = Oficina.tipos.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo.isNotBlank()) Text(subtitulo, style = Oficina.tipos.bodySm, color = c.muted)
        }
        if (marcado) Text("✓", style = Oficina.tipos.body, color = c.ink)
    }
}

@Composable
fun Confirmar(
    titulo: String,
    texto: String,
    confirmar: String = "Confirmar",
    perigo: Boolean = false,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val c = Oficina.cores
    DialogoOficina(
        titulo = titulo,
        onDismiss = onCancelar,
        acoes = {
            BotaoTexto("Voltar", onCancelar, cor = c.muted)
            BotaoTexto(confirmar, onConfirmar, cor = if (perigo) c.danger else c.accentInk)
        },
    ) {
        Text(texto, style = Oficina.tipos.body, color = c.muted)
    }
}

/** Botão do app: principal = bloco invertido; secundário = contorno. O ícone é ignorado no padrão Oficina. */
@Composable
fun BotaoAcao(
    texto: String,
    @Suppress("UNUSED_PARAMETER") icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    principal: Boolean = false,
    habilitado: Boolean = true,
) {
    if (principal) BotaoPrimario(texto, onClick, modifier, habilitado)
    else BotaoSecundario(texto, onClick, modifier, habilitado)
}

/* ───────────── Data e hora (calendário e relógio do Material, no tema do app) ───────────── */

/** Pedido pendente de data/hora. A janela é desenhada por [SeletorDataHoraHost], que fica na raiz do app. */
object PedidoDataHora {
    var pedido by mutableStateOf<Pair<Long?, (Long) -> Unit>?>(null)
}

/** Abre o calendário e depois o relógio. */
@Suppress("UNUSED_PARAMETER")
fun escolherDataHora(ctx: Context, inicial: Long?, onOk: (Long) -> Unit) {
    PedidoDataHora.pedido = inicial to onOk
}

@Composable
fun SeletorDataHoraHost() {
    val pedido = PedidoDataHora.pedido ?: return
    val c = Oficina.cores
    val inicial = remember(pedido) {
        Calendar.getInstance().apply { timeInMillis = pedido.first ?: (System.currentTimeMillis() + 3_600_000L) }
    }
    // O calendário trabalha com meia-noite em UTC.
    val utcInicial = remember(pedido) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(inicial.get(Calendar.YEAR), inicial.get(Calendar.MONTH), inicial.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    var dataEscolhida by remember(pedido) { mutableStateOf<Long?>(null) }
    val fechar = { PedidoDataHora.pedido = null }

    if (dataEscolhida == null) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = utcInicial)
        val cores = DatePickerDefaults.colors(
            containerColor = c.bgRaised,
            selectedDayContainerColor = c.invertBg,
            selectedDayContentColor = c.invertFg,
            todayDateBorderColor = c.accentInk,
            todayContentColor = c.accentInk,
        )
        DatePickerDialog(
            onDismissRequest = fechar,
            confirmButton = { BotaoTexto("Próximo", { dataEscolhida = estado.selectedDateMillis ?: utcInicial }) },
            dismissButton = { BotaoTexto("Cancelar", fechar, cor = c.muted) },
            shape = RectangleShape,
            colors = cores,
        ) {
            DatePicker(state = estado, colors = cores, showModeToggle = false)
        }
    } else {
        val estado = rememberTimePickerState(
            initialHour = inicial.get(Calendar.HOUR_OF_DAY),
            initialMinute = inicial.get(Calendar.MINUTE),
            is24Hour = true,
        )
        DialogoOficina(
            titulo = "Horário",
            onDismiss = fechar,
            acoes = {
                BotaoTexto("Cancelar", fechar, cor = c.muted)
                BotaoTexto("Confirmar", {
                    val dia = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dataEscolhida!! }
                    val r = Calendar.getInstance().apply {
                        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH), estado.hour, estado.minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    fechar()
                    pedido.second(r.timeInMillis)
                })
            },
        ) {
            TimePicker(
                state = estado,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.bgSunken,
                    selectorColor = c.invertBg,
                    containerColor = c.bgRaised,
                    timeSelectorSelectedContainerColor = c.invertBg,
                    timeSelectorSelectedContentColor = c.invertFg,
                    timeSelectorUnselectedContainerColor = c.bgSunken,
                    timeSelectorUnselectedContentColor = c.ink,
                    clockDialSelectedContentColor = c.invertFg,
                    clockDialUnselectedContentColor = c.ink,
                ),
            )
        }
    }
}

/* ───────────── Navegação ───────────── */

/** Pontos de uma corrida, na ordem, com rótulo para o menu de navegação. */
fun pontosDaCorrida(origem: Endereco, paradas: List<Endereco>, destino: Endereco): List<Pair<String, Endereco>> =
    buildList {
        add("Embarque" to origem)
        paradas.forEachIndexed { i, p -> add("Parada ${i + 1}" to p) }
        add("Destino" to destino)
    }.filter { it.second.texto.isNotBlank() || it.second.temCoord }

/** Botão que abre a rota no Waze, Google Maps ou outro app de GPS. */
@Composable
fun BotaoNavegar(
    pontos: List<Pair<String, Endereco>>,
    modifier: Modifier = Modifier,
    texto: String = "Navegar",
    principal: Boolean = false,
) {
    val ctx = LocalContext.current
    val c = Oficina.cores
    var aberto by remember { mutableStateOf(false) }

    BotaoAcao(
        texto, Icons.Filled.Navigation,
        { if (pontos.isEmpty()) ctx.toast("Informe os endereços primeiro") else aberto = true },
        modifier, principal = principal,
    )

    if (aberto) {
        DialogoOficina(
            titulo = "Abrir navegação",
            eyebrow = "GPS",
            onDismiss = { aberto = false },
            acoes = { BotaoTexto("Fechar", { aberto = false }, cor = c.muted) },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pontos.size > 1) {
                    BotaoPrimario(
                        "Google Maps · rota completa",
                        { aberto = false; Navegar.mapsRotaCompleta(ctx, pontos.map { it.second }) },
                        Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Sai da sua posição e passa por embarque, paradas e destino, nessa ordem.",
                        style = Oficina.tipos.bodySm, color = c.muted,
                    )
                    Linha()
                }
                Hud("Ir até um ponto")
                pontos.forEach { (rotulo, e) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Hud(rotulo, cor = c.ink)
                        Text(e.texto, style = Oficina.tipos.bodySm, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BotaoSecundario("Waze", { aberto = false; Navegar.wazeAte(ctx, e) }, Modifier.weight(1f), compacto = true)
                            BotaoSecundario("Maps", { aberto = false; Navegar.mapsAte(ctx, e) }, Modifier.weight(1f), compacto = true)
                            BotaoSecundario("Outro", { aberto = false; Navegar.outroApp(ctx, e) }, Modifier.weight(1f), compacto = true)
                        }
                    }
                }
            }
        }
    }
}
