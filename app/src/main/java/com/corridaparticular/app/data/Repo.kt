package com.corridaparticular.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Armazena tudo em um único arquivo JSON dentro do app.
 * O mesmo formato é usado no backup (exportar/importar).
 */
object Repo {
    private var arquivo: File? = null

    val clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val corridas = MutableStateFlow<List<Corrida>>(emptyList())
    val tarifas = MutableStateFlow(Tarifas())
    val config = MutableStateFlow(Config())

    fun init(ctx: Context) {
        if (arquivo != null) return
        val f = File(ctx.filesDir, "dados.json")
        arquivo = f
        if (f.exists()) runCatching { carregar(JSONObject(f.readText())) }
    }

    // ---------- Clientes ----------
    fun cliente(id: String?): Cliente? = id?.let { i -> clientes.value.find { it.id == i } }

    fun salvarCliente(c: Cliente) {
        val lista = clientes.value.toMutableList()
        val i = lista.indexOfFirst { it.id == c.id }
        if (i >= 0) lista[i] = c else lista.add(c)
        clientes.value = lista.sortedBy { it.nome.lowercase() }
        salvar()
    }

    fun excluirCliente(id: String) {
        clientes.value = clientes.value.filterNot { it.id == id }
        salvar()
    }

    // ---------- Corridas ----------
    fun corrida(id: String?): Corrida? = id?.let { i -> corridas.value.find { it.id == i } }

    fun salvarCorrida(c: Corrida) {
        val lista = corridas.value.toMutableList()
        val i = lista.indexOfFirst { it.id == c.id }
        if (i >= 0) lista[i] = c else lista.add(c)
        corridas.value = lista.sortedBy { it.dataHora }
        salvar()
    }

    fun excluirCorrida(id: String) {
        corridas.value = corridas.value.filterNot { it.id == id }
        salvar()
    }

    // ---------- Ajustes ----------
    fun salvarTarifas(t: Tarifas) { tarifas.value = t; salvar() }
    fun salvarConfig(c: Config) { config.value = c; salvar() }

    // ---------- Backup ----------
    fun exportarJson(): String = JSONObject().apply {
        put("app", "CorridaParticular")
        put("versao", 1)
        put("exportadoEm", System.currentTimeMillis())
        put("config", config.value.toJson())
        put("tarifas", tarifas.value.toJson())
        put("clientes", JSONArray().also { a -> clientes.value.forEach { a.put(it.toJson()) } })
        put("corridas", JSONArray().also { a -> corridas.value.forEach { a.put(it.toJson()) } })
    }.toString(2)

    /** Substitui todos os dados pelos do backup. Retorna o nº de clientes importados. */
    fun importarJson(texto: String): Result<Int> = runCatching {
        val o = JSONObject(texto)
        require(o.has("clientes") || o.has("corridas")) { "Este arquivo não é um backup do app." }
        carregar(o)
        salvar()
        clientes.value.size
    }

    private fun carregar(o: JSONObject) {
        config.value = o.optJSONObject("config")?.let { Config.fromJson(it) } ?: Config()
        tarifas.value = o.optJSONObject("tarifas")?.let { Tarifas.fromJson(it) } ?: Tarifas()
        clientes.value = o.optJSONArray("clientes").lista { Cliente.fromJson(it) }.sortedBy { it.nome.lowercase() }
        corridas.value = o.optJSONArray("corridas").lista { Corrida.fromJson(it) }.sortedBy { it.dataHora }
    }

    @Synchronized
    private fun salvar() {
        val f = arquivo ?: return
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(exportarJson())
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
