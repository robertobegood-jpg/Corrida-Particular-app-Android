package com.corridaparticular.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.data.Tema
import com.corridaparticular.app.data.TemaStore
import com.corridaparticular.app.taximetro.TaximetroState
import com.corridaparticular.app.ui.*
import com.corridaparticular.app.ui.theme.CorridaTheme
import com.corridaparticular.app.ui.theme.FontesOficina
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.ui.theme.Visor
import com.corridaparticular.app.util.minutosTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.reais

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ABA = "aba"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        TemaStore.init(applicationContext)
        abrirAbaDoIntent(intent)
        val fontes = FontesOficina(applicationContext)
        setContent {
            val tema by TemaStore.tema.collectAsState()
            val escuro = when (tema) {
                Tema.ESCURO -> true
                Tema.CLARO -> false
                Tema.SISTEMA -> isSystemInDarkTheme()
            }
            // Ícones da barra de status acompanham o tema.
            DisposableEffect(escuro) {
                val estilo = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }
            CorridaTheme(escuro = escuro, fontes = fontes) { AppRoot(Sessao.app) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abrirAbaDoIntent(intent)
    }

    private fun abrirAbaDoIntent(intent: Intent?) {
        val nome = intent?.getStringExtra(EXTRA_ABA) ?: return
        Aba.entries.find { it.name == nome }?.let { Sessao.app.aba = it }
    }
}

@Composable
fun AppRoot(state: AppState) {
    BackHandler(enabled = state.aba != Aba.TAXIMETRO) { state.aba = Aba.TAXIMETRO }
    EstruturaApp(state) {
        when (state.aba) {
            Aba.TAXIMETRO -> TelaTaximetro(state)
            Aba.ORCAMENTO -> TelaOrcamento(state)
            Aba.AGENDA -> TelaAgenda(state)
            Aba.CLIENTES -> TelaClientes(state)
            Aba.MAIS -> TelaMais(state)
        }
    }
    VerificadorAtualizacao()
    SeletorDataHoraHost()
}

/** Barra do topo, conteúdo da aba, faixa do taxímetro e menu inferior. */
@Composable
fun EstruturaApp(state: AppState, conteudo: @Composable () -> Unit) {
    val taxi by TaximetroState.dados.collectAsState()
    val c = Oficina.cores
    val (statusTexto, statusCor) = when {
        taxi.ativo && taxi.pausado -> "Pausado" to c.ambar
        taxi.ativo && taxi.esperando -> "Aguardando" to Visor.amarelo
        taxi.ativo -> "Em corrida" to Visor.amarelo
        else -> "Livre" to c.verde
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        BarraSuperior(statusTexto, statusCor)
        Box(Modifier.weight(1f).fillMaxWidth()) { conteudo() }
        if (taxi.ativo && state.aba != Aba.TAXIMETRO) {
            FaixaTaximetro(
                texto = if (taxi.pausado) "Pausado" else if (taxi.esperando) "Aguardando" else "Taxímetro",
                detalhe = "${taxi.km.kmTexto()} · ${minutosTexto(taxi.msTotal / 60_000.0)}",
                valor = taxi.valor.reais(),
                onClick = { state.aba = Aba.TAXIMETRO },
            )
        }
        BarraInferior(
            abaAtual = state.aba,
            onAba = { state.aba = it },
            marcas = if (taxi.ativo) mapOf(Aba.TAXIMETRO to Visor.amarelo) else emptyMap(),
        )
    }
}
