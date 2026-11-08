@file:OptIn(ExperimentalLayoutApi::class)

package com.corridaparticular.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.taximetro.TaximetroState
import com.corridaparticular.app.ui.*
import com.corridaparticular.app.util.duracaoTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.reais

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ABA = "aba"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        enableEdgeToEdge()
        abrirAbaDoIntent(intent)
        setContent {
            Tema { AppRoot(Sessao.app) }
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
    val taxi by TaximetroState.dados.collectAsState()

    BackHandler(enabled = state.aba != Aba.TAXIMETRO) { state.aba = Aba.TAXIMETRO }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                // Aviso do taxímetro ligado quando o motorista está em outra aba
                if (taxi.ativo && state.aba != Aba.TAXIMETRO) {
                    Surface(
                        color = PainelEscuro,
                        modifier = Modifier.fillMaxWidth().clickable { state.aba = Aba.TAXIMETRO },
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalTaxi, null, tint = AmareloTaxi)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (taxi.pausado) "Taxímetro pausado" else "Taxímetro rodando",
                                color = Color.White, modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${taxi.km.kmTexto()} • ${duracaoTexto(taxi.msTotal)}  ",
                                color = Color(0xFFB0B8C4), style = MaterialTheme.typography.bodySmall,
                            )
                            Text(taxi.valor.reais(), color = AmareloTaxi, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = state.aba == aba,
                            onClick = { state.aba = aba },
                            icon = {
                                if (aba == Aba.TAXIMETRO && taxi.ativo) {
                                    BadgedBox(badge = { Badge(containerColor = AmareloTaxi) }) { Icon(aba.icone, aba.titulo) }
                                } else {
                                    Icon(aba.icone, aba.titulo)
                                }
                            },
                            label = { Text(aba.titulo, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .consumeWindowInsets(pad)
        ) {
            when (state.aba) {
                Aba.TAXIMETRO -> TelaTaximetro(state)
                Aba.ORCAMENTO -> TelaOrcamento(state)
                Aba.AGENDA -> TelaAgenda(state)
                Aba.CLIENTES -> TelaClientes(state)
                Aba.MAIS -> TelaMais(state)
            }
        }
    }
}
