@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.corridaparticular.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.data.Cliente
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.net.Localizacao
import com.corridaparticular.app.net.TomTom
import com.corridaparticular.app.util.Navegar
import com.corridaparticular.app.util.toast
import kotlinx.coroutines.launch
import java.util.Calendar

/** Estrutura padrão de cada tela: barra superior + conteúdo (rolável ou não). */
@Composable
fun Tela(
    titulo: String,
    onVoltar: (() -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    rolavel: Boolean = true,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onVoltar != null) IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = acoes,
            )
        },
        floatingActionButton = fab,
    ) { pad ->
        val base = Modifier
            .fillMaxSize()
            .padding(pad)
            .consumeWindowInsets(pad)
            .imePadding()
        if (rolavel) {
            Column(
                base
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = conteudo,
            )
        } else {
            Column(base, content = conteudo)
        }
    }
}

@Composable
fun Secao(titulo: String, modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            conteudo()
        }
    }
}

@Composable
fun LinhaInfo(rotulo: String, valor: String, destaque: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium,
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
        singleLine = linhas == 1,
        minLines = linhas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        prefix = if (prefixo != null) { { Text(prefixo) } } else null,
        suffix = if (sufixo != null) { { Text(sufixo) } } else null,
        supportingText = if (ajuda != null) { { Text(ajuda) } } else null,
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

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (sugestoes.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sugestoes) { s ->
                    FilterChip(
                        selected = s.texto == valor.texto,
                        onClick = { onChange(s); resultados = emptyList(); erro = null },
                        label = { Text(s.apelido.ifBlank { s.texto }, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
                        leadingIcon = { Icon(Icons.Filled.Star, null, Modifier.size(16.dp)) },
                    )
                }
            }
        }
        Box {
            OutlinedTextField(
                value = valor.texto,
                onValueChange = { onChange(Endereco(texto = it)); erro = null },
                label = { Text(rotulo) },
                leadingIcon = {
                    Icon(icone, null, tint = if (valor.temCoord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (buscando) {
                            CircularProgressIndicator(Modifier.size(22.dp).padding(2.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { buscar() }) { Icon(Icons.Filled.Search, "Buscar endereço") }
                        }
                        if (localAtual) IconButton(onClick = { usarLocal() }) { Icon(Icons.Filled.MyLocation, "Usar minha localização") }
                        if (onRemover != null) IconButton(onClick = onRemover) { Icon(Icons.Filled.Close, "Remover") }
                    }
                },
                supportingText = {
                    when {
                        erro != null -> Text(erro!!, color = MaterialTheme.colorScheme.error)
                        valor.temCoord -> Text("✓ Endereço localizado", color = MaterialTheme.colorScheme.tertiary)
                        valor.texto.isNotBlank() -> Text("Toque na lupa para localizar")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = resultados.isNotEmpty(),
                onDismissRequest = { resultados = emptyList() },
                modifier = Modifier.fillMaxWidth(0.92f),
            ) {
                resultados.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.texto, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Filled.Place, null) },
                        onClick = { onChange(r); resultados = emptyList() },
                    )
                }
            }
        }
    }
}

/** Botão que abre a lista de clientes; null = corrida avulsa. */
@Composable
fun SeletorCliente(clienteId: String?, onSelecionar: (Cliente?) -> Unit) {
    val clientes by Repo.clientes.collectAsState()
    var aberto by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("") }
    val atual = clientes.find { it.id == clienteId }

    OutlinedCard(Modifier.fillMaxWidth().clickable { aberto = true }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (atual != null) Icons.Filled.Person else Icons.Filled.PersonOutline, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(atual?.nome ?: "Corrida avulsa", fontWeight = FontWeight.SemiBold)
                Text(
                    atual?.telefone?.ifBlank { null } ?: if (atual == null) "Toque para escolher um cliente cadastrado" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ArrowDropDown, null)
        }
    }

    if (aberto) {
        val lista = clientes.filter {
            filtro.isBlank() || it.nome.contains(filtro, true) || it.telefone.contains(filtro)
        }
        AlertDialog(
            onDismissRequest = { aberto = false },
            confirmButton = { TextButton(onClick = { aberto = false }) { Text("Fechar") } },
            title = { Text("Escolher cliente") },
            text = {
                Column {
                    OutlinedTextField(
                        value = filtro, onValueChange = { filtro = it },
                        placeholder = { Text("Buscar por nome ou telefone") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        item {
                            ListItem(
                                headlineContent = { Text("Corrida avulsa (sem cadastro)") },
                                leadingContent = { Icon(Icons.Filled.PersonOutline, null) },
                                modifier = Modifier.clickable { onSelecionar(null); aberto = false },
                            )
                            HorizontalDivider()
                        }
                        items(lista, key = { it.id }) { c ->
                            ListItem(
                                headlineContent = { Text(c.nome) },
                                supportingContent = { if (c.telefone.isNotBlank()) Text(c.telefone) },
                                leadingContent = { Icon(Icons.Filled.Person, null) },
                                modifier = Modifier.clickable { onSelecionar(c); aberto = false },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun Confirmar(
    titulo: String,
    texto: String,
    confirmar: String = "Confirmar",
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
fun BotaoAcao(texto: String, icone: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, principal: Boolean = false, habilitado: Boolean = true) {
    if (principal) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = habilitado) {
            Icon(icone, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(texto, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = habilitado) {
            Icon(icone, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(texto, maxLines = 1)
        }
    }
}

/** Abre o calendário e depois o relógio. */
fun escolherDataHora(ctx: Context, inicial: Long?, onOk: (Long) -> Unit) {
    val c = Calendar.getInstance().apply { timeInMillis = inicial ?: (System.currentTimeMillis() + 3_600_000L) }
    DatePickerDialog(ctx, { _, ano, mes, dia ->
        TimePickerDialog(ctx, { _, hora, min ->
            val r = Calendar.getInstance().apply {
                set(ano, mes, dia, hora, min, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onOk(r.timeInMillis)
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

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
    var aberto by remember { mutableStateOf(false) }

    BotaoAcao(
        texto, Icons.Filled.Navigation,
        { if (pontos.isEmpty()) ctx.toast("Informe os endereços primeiro") else aberto = true },
        modifier, principal = principal,
    )

    if (aberto) {
        AlertDialog(
            onDismissRequest = { aberto = false },
            confirmButton = { TextButton(onClick = { aberto = false }) { Text("Fechar") } },
            title = { Text("Abrir navegação") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pontos.size > 1) {
                        Button(
                            onClick = { aberto = false; Navegar.mapsRotaCompleta(ctx, pontos.map { it.second }) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Map, null); Spacer(Modifier.width(8.dp)); Text("Google Maps – rota completa")
                        }
                        Text(
                            "Sai da sua posição e passa por embarque, paradas e destino, nessa ordem.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider()
                    }
                    Text("Ir até um ponto", fontWeight = FontWeight.SemiBold)
                    pontos.forEach { (rotulo, e) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(e.texto, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(onClick = { aberto = false; Navegar.wazeAte(ctx, e) }, modifier = Modifier.weight(1f)) { Text("Waze") }
                                FilledTonalButton(onClick = { aberto = false; Navegar.mapsAte(ctx, e) }, modifier = Modifier.weight(1f)) { Text("Maps") }
                                OutlinedButton(onClick = { aberto = false; Navegar.outroApp(ctx, e) }, modifier = Modifier.weight(1f)) { Text("Outro") }
                            }
                        }
                    }
                }
            },
        )
    }
}
