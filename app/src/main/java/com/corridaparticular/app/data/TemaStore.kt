package com.corridaparticular.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tema(val rotulo: String) {
    ESCURO("Escuro"),
    CLARO("Claro"),
    SISTEMA("Sistema")
}

/** Tema escolhido em "Mais". O escuro é o padrão do sistema Oficina. */
object TemaStore {
    private const val CHAVE = "tema"
    private val _tema = MutableStateFlow(Tema.ESCURO)
    val tema: StateFlow<Tema> = _tema.asStateFlow()
    private var ctx: Context? = null

    fun init(context: Context) {
        if (ctx != null) return
        ctx = context.applicationContext
        val salvo = prefs()?.getString(CHAVE, null)
        _tema.value = Tema.entries.firstOrNull { it.name == salvo } ?: Tema.ESCURO
    }

    fun definir(tema: Tema) {
        prefs()?.edit()?.putString(CHAVE, tema.name)?.apply()
        _tema.value = tema
    }

    private fun prefs() = ctx?.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
}
