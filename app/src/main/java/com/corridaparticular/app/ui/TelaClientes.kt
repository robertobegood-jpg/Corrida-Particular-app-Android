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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.ui.graphics.RectangleShape
import com.corridaparticular.app.ui.theme.Oficina
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

    val c = Oficina.cores
    Tela(
        titulo = "Seus *clientes*.",
        eyebrow = "04/Clientes",
        subtitulo = if (clientes.isEmpty()) null else "${clientes.size} cadastrado(s).",
        acoes = { BotaoTexto("+ Novo", { editando = NOVO }) },
        rolavel = false,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = busca,
                        onValueChange = { busca = it },
                        placeholder = { Text("Buscar por nome ou telefone") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        textStyle = Oficina.tipos.body,
                        shape = RectangleShape,
                        colors = coresCampo(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BotaoSecundario("Importar dos contatos do celular", importarContato, Modifier.fillMaxWidth())
                }
                Linha()
            }
            if (lista.isEmpty()) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (clientes.isEmpty()) "Nenhum cliente cadastrado." else "Nenhum cliente encontrado.",
                            style = Oficina.tipos.body, color = c.ink,
                        )
                        Text(
                            if (clientes.isEmpty()) "Toque em “+ Novo” ou importe dos contatos." else "Confira o nome ou o número digitado.",
                            style = Oficina.tipos.bodySm, color = c.muted,
                        )
                    }
                }
            }
            items(lista, key = { it.id }) { cli ->
                val total = corridas.count { it.clienteId == cli.id }
                Column(Modifier.fillMaxWidth().clickable { editando = cli.id }.padding(start = 16.dp, top = 14.dp, end = 4.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cli.nome, style = Oficina.tipos.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(cli.telefone, if (total > 0) "$total corrida(s)" else "").filter { it.isNotBlank() }.joinToString(" · ")
                                    .ifBlank { "Sem telefone" },
                                style = Oficina.tipos.bodySm, color = c.muted,
                            )
                        }
                        if (cli.telefone.isNotBlank()) {
                            IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cli.telefone}"))) }) {
                                Icon(Icons.Outlined.Phone, "Ligar", tint = c.ink)
                            }
                            IconButton(onClick = { Compartilhar.whatsappTexto(ctx, cli.telefone, "Olá, ${cli.nome.substringBefore(' ')}! ") }) {
                                Icon(Icons.Outlined.Sms, "WhatsApp", tint = c.ink)
                            }
                        }
                    }
                    cli.origens.firstOrNull()?.let {
                        Row(Modifier.padding(top = 6.dp, end = 12.dp), verticalAlignment = Alignment.Top) {
                            Hud("Embarque", cor = c.faint, modifier = Modifier.width(84.dp).padding(top = 3.dp))
                            Text(it.rotulo, style = Oficina.tipos.bodySm, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    BotaoTexto("Nova corrida  →", { state.novoOrcamento(cli) }, Modifier.offset(x = (-12).dp))
                }
                Linha()
            }
        }
    }
}

@Composable
internal fun EditarCliente(cliente: Cliente?, rascunho: Cliente?, onFechar: () -> Unit) {
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

    val c = Oficina.cores
    Tela(
        titulo = if (cliente == null) "Novo *cliente*." else "Editar *cliente*.",
        subtitulo = if (cliente != null) nome.ifBlank { null } else null,
        eyebrow = if (cliente == null) "Clientes · cadastro" else "Clientes · editar",
        onVoltar = onFechar,
        acoes = {
            if (cliente != null) BotaoTexto("Excluir", { excluir = true }, cor = c.danger)
            BotaoTexto("Salvar", { salvar() })
        },
    ) {
        Secao("Dados") {
            if (cliente == null) {
                val preencher = rememberImportarContato { d ->
                    if (d.nome.isNotBlank()) nome = d.nome
                    if (d.telefone.isNotBlank()) fone = d.telefone
                    d.enderecos.filter { e -> origens.none { it.texto == e.texto } }.forEach { origens.add(it) }
                }
                BotaoSecundario("Preencher com um contato", preencher, Modifier.fillMaxWidth())
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

        BotaoPrimario("Salvar cliente", { salvar() }, Modifier.fillMaxWidth().height(56.dp))
    }

    if (excluir) {
        Confirmar(
            "Excluir cliente?",
            "O histórico de corridas dele continua salvo.",
            "Excluir",
            perigo = true,
            onConfirmar = { excluir = false; Repo.excluirCliente(base.id); onFechar() },
            onCancelar = { excluir = false },
        )
    }
}

@Composable
private fun ListaEnderecos(titulo: String, dica: String, itens: MutableList<Endereco>) {
    Secao(titulo) {
        Text(dica, style = Oficina.tipos.bodySm, color = Oficina.cores.muted)
        itens.forEachIndexed { i, e ->
            key(i) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (i > 0) Linha(Modifier.padding(vertical = 6.dp))
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
        BotaoTexto("+ Adicionar endereço", { itens.add(Endereco()) }, Modifier.offset(x = (-12).dp))
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
