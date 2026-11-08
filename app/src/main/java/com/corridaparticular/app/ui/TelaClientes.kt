@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.data.Cliente
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.share.Compartilhar
import com.corridaparticular.app.util.Contatos
import com.corridaparticular.app.util.toast

private const val NOVO = "__novo__"

@Composable
fun TelaClientes(state: AppState) {
    val ctx = LocalContext.current
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var rascunho by remember { mutableStateOf<Cliente?>(null) }

    val importarContato = rememberImportarContato { dados ->
        val existente = Repo.clientes.value.find { Contatos.mesmoNumero(it.telefone, dados.telefone) }
        if (existente != null) {
            ctx.toast("${existente.nome} já está cadastrado com esse número")
            editando = existente.id
        } else {
            rascunho = Cliente(nome = dados.nome, telefone = dados.telefone, origens = dados.enderecos)
            editando = NOVO
        }
    }

    if (editando != null) {
        val fechar = { editando = null; rascunho = null }
        BackHandler { fechar() }
        EditarCliente(
            cliente = if (editando == NOVO) null else Repo.cliente(editando),
            rascunho = rascunho,
            onFechar = fechar,
        )
        return
    }

    val clientes by Repo.clientes.collectAsState()
    val corridas by Repo.corridas.collectAsState()
    var busca by rememberSaveable { mutableStateOf("") }
    val lista = clientes.filter {
        busca.isBlank() || it.nome.contains(busca, true) || it.telefone.filter { ch -> ch.isDigit() }.contains(busca.filter { ch -> ch.isDigit() }.ifEmpty { "\u0000" })
    }

    Tela(
        titulo = "Clientes",
        rolavel = false,
        fab = {
            ExtendedFloatingActionButton(
                text = { Text("Novo cliente") },
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                onClick = { editando = NOVO },
            )
        },
    ) {
        OutlinedTextField(
            value = busca,
            onValueChange = { busca = it },
            placeholder = { Text("Buscar cliente") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        TextButton(onClick = importarContato, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Filled.ContactPhone, null); Spacer(Modifier.width(8.dp)); Text("Importar dos contatos do celular")
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (lista.isEmpty()) {
                item {
                    Text(
                        if (clientes.isEmpty()) "Nenhum cliente cadastrado.\nToque em “Novo cliente”." else "Nenhum cliente encontrado.",
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(lista, key = { it.id }) { c ->
                val total = corridas.count { it.clienteId == c.id }
                OutlinedCard(Modifier.fillMaxWidth().clickable { editando = c.id }) {
                    Column(Modifier.padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.nome, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(c.telefone, if (total > 0) "$total corrida(s)" else "").filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (c.telefone.isNotBlank()) {
                                IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.telefone}"))) }) {
                                    Icon(Icons.Filled.Phone, "Ligar")
                                }
                                IconButton(onClick = { Compartilhar.whatsappTexto(ctx, c.telefone, "Olá, ${c.nome.substringBefore(' ')}! ") }) {
                                    Icon(Icons.Filled.Sms, "WhatsApp")
                                }
                            }
                        }
                        c.origens.firstOrNull()?.let {
                            Text(
                                "Origem: ${it.rotulo}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        TextButton(onClick = { state.novoOrcamento(c) }) {
                            Icon(Icons.Filled.Calculate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nova corrida / orçamento")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditarCliente(cliente: Cliente?, rascunho: Cliente?, onFechar: () -> Unit) {
    val ctx = LocalContext.current
    val base = remember { cliente ?: rascunho ?: Cliente() }
    var nome by remember { mutableStateOf(base.nome) }
    var fone by remember { mutableStateOf(base.telefone) }
    var obs by remember { mutableStateOf(base.obs) }
    val origens = remember { base.origens.toMutableStateList() }
    val destinos = remember { base.destinos.toMutableStateList() }
    var excluir by remember { mutableStateOf(false) }

    fun salvar() {
        if (nome.isBlank()) { ctx.toast("Informe o nome do cliente"); return }
        Repo.salvarCliente(
            base.copy(
                nome = nome.trim(), telefone = fone.trim(), obs = obs.trim(),
                origens = origens.filter { it.texto.isNotBlank() },
                destinos = destinos.filter { it.texto.isNotBlank() },
            )
        )
        ctx.toast("Cliente salvo")
        onFechar()
    }

    Tela(
        titulo = if (cliente == null) "Novo cliente" else "Editar cliente",
        onVoltar = onFechar,
        acoes = {
            if (cliente != null) IconButton(onClick = { excluir = true }) { Icon(Icons.Filled.Delete, "Excluir") }
            IconButton(onClick = { salvar() }) { Icon(Icons.Filled.Check, "Salvar") }
        },
    ) {
        Secao("Dados") {
            if (cliente == null) {
                val preencher = rememberImportarContato { d ->
                    if (d.nome.isNotBlank()) nome = d.nome
                    if (d.telefone.isNotBlank()) fone = d.telefone
                    d.enderecos.filter { e -> origens.none { it.texto == e.texto } }.forEach { origens.add(it) }
                }
                OutlinedButton(onClick = preencher, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.ContactPhone, null); Spacer(Modifier.width(8.dp)); Text("Preencher com um contato do celular")
                }
            }
            CampoTexto("Nome *", nome, { nome = it })
            CampoTexto("WhatsApp / telefone", fone, { fone = it }, teclado = KeyboardType.Phone, ajuda = "Com DDD. Ex.: 11 98765-4321")
            CampoTexto("Observações", obs, { obs = it }, linhas = 2)
        }

        ListaEnderecos(
            titulo = "Endereços de origem (embarque)",
            dica = "Ex.: Casa, Trabalho. O primeiro é usado automaticamente nos orçamentos.",
            itens = origens,
        )
        ListaEnderecos(
            titulo = "Destinos frequentes",
            dica = "Aparecem como atalho no campo de destino.",
            itens = destinos,
        )

        Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar cliente")
        }
    }

    if (excluir) {
        Confirmar(
            "Excluir cliente?",
            "O histórico de corridas dele continua salvo.",
            "Excluir",
            onConfirmar = { excluir = false; Repo.excluirCliente(base.id); onFechar() },
            onCancelar = { excluir = false },
        )
    }
}

@Composable
private fun ListaEnderecos(titulo: String, dica: String, itens: MutableList<Endereco>) {
    Secao(titulo) {
        Text(dica, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        itens.forEachIndexed { i, e ->
            key(i) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    CampoTexto("Apelido (ex.: Casa)", e.apelido, { itens[i] = e.copy(apelido = it) })
                    CampoEndereco(
                        "Endereço", e,
                        { novo -> itens[i] = novo.copy(apelido = itens[i].apelido) },
                        localAtual = true,
                        onRemover = { itens.removeAt(i) },
                    )
                }
            }
        }
        TextButton(onClick = { itens.add(Endereco()) }) {
            Icon(Icons.Filled.AddLocationAlt, null); Spacer(Modifier.width(6.dp)); Text("Adicionar endereço")
        }
    }
}

/**
 * Abre a agenda do celular para escolher um contato.
 * Pede a permissão de contatos (para trazer também os endereços); se negada, traz só nome e telefone.
 */
@Composable
fun rememberImportarContato(onContato: (Contatos.Dados) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val callback by rememberUpdatedState(onContato)

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.data
        if (res.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        val dados = Contatos.ler(ctx, uri)
        if (dados == null) ctx.toast("Não foi possível ler esse contato") else callback(dados)
    }

    fun abrirAgenda() {
        try {
            escolher.launch(Contatos.intentEscolher())
        } catch (e: ActivityNotFoundException) {
            ctx.toast("Nenhum app de contatos encontrado")
        }
    }

    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { abrirAgenda() }

    return {
        if (Contatos.temPermissao(ctx)) abrirAgenda() else permissao.launch(Manifest.permission.READ_CONTACTS)
    }
}
