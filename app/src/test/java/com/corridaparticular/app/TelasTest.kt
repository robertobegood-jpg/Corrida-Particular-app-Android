package com.corridaparticular.app

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.corridaparticular.app.DadosDeTeste.aeroporto
import com.corridaparticular.app.DadosDeTeste.casa
import com.corridaparticular.app.DadosDeTeste.shopping
import com.corridaparticular.app.ui.*
import com.corridaparticular.app.ui.theme.CorridaTheme
import com.corridaparticular.app.ui.theme.FontesOficina
import com.corridaparticular.app.ui.theme.Oficina
import org.junit.Rule
import org.junit.Test

/**
 * Prints automáticos para a revisão antes de cada versão.
 * Celular estreito (360 dp) com dados extremos: nomes longos, valores altos e muitas paradas.
 * Cada tela é desenhada inteira e fotografada em fatias de 500 dp.
 * Gerados com: ./gradlew recordPaparazziDebug  (saem em app/src/test/snapshots/images)
 */
class TelasTest {

    private val celular = DeviceConfig(
        screenWidth = 720, screenHeight = 1000, xdpi = 320, ydpi = 320,
        density = Density.XHIGH, softButtons = false,
    )
    private val alturaFatia = 500.dp

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = celular, theme = "android:Theme.Material.NoActionBar")

    private fun fonteGrande() = paparazzi.unsafeUpdateConfig(deviceConfig = celular.copy(fontScale = 1.3f))

    @Composable
    private fun ComTema(escuro: Boolean, conteudo: @Composable () -> Unit) {
        val fontes = FontesOficina(LocalContext.current)
        // Telas com seletores de arquivo/permissão precisam de um "dono" de resultados, como uma Activity.
        val dono = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?,
                ) = Unit
            }
        }
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides dono) {
            CorridaTheme(escuro = escuro, fontes = fontes) {
                Box(Modifier.fillMaxSize().background(Oficina.cores.bg)) { conteudo() }
            }
        }
    }

    /** Desenha [conteudo] com [alturaTotal] e tira uma foto de cada fatia de 500 dp. */
    private fun fatias(nome: String, alturaTotal: Dp, escuro: Boolean = true, conteudo: @Composable () -> Unit) {
        val quantidade = ((alturaTotal.value + alturaFatia.value - 1) / alturaFatia.value).toInt()
        for (k in 0 until quantidade) {
            paparazzi.snapshot(name = "${nome}_${k + 1}") {
                ComTema(escuro) {
                    Box(Modifier.fillMaxSize().clipToBounds()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .requiredHeight(alturaTotal)
                                .offset(y = -alturaFatia * k)
                        ) { conteudo() }
                    }
                }
            }
        }
    }

    /** A aba com a barra de cima, a faixa do taxímetro e o menu de baixo, como no celular. */
    @Composable
    private fun Celular(aba: Aba, conteudo: @Composable (AppState) -> Unit) {
        val state = androidx.compose.runtime.remember { AppState().apply { this.aba = aba } }
        EstruturaApp(state) { conteudo(state) }
    }

    // ───────────── Taxímetro ─────────────

    @Test fun taximetro_preparar() { DadosDeTeste.carregar(); fatias("taximetro_preparar", 1000.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_preparar_claro() { DadosDeTeste.carregar(); fatias("taximetro_preparar_claro", 1000.dp, escuro = false) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }

    @Test fun taximetro_rodando() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(); fatias("taximetro_rodando", 2000.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_rodando_claro() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(); fatias("taximetro_rodando_claro", 1000.dp, escuro = false) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_valor_alto() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(valor = 1234.56); fatias("taximetro_valor_alto", 500.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_esperando() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(esperando = true); fatias("taximetro_esperando", 1500.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_pausado() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(pausado = true); fatias("taximetro_pausado", 500.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_fonte_grande() { fonteGrande(); DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(valor = 1234.56); fatias("taximetro_fonte_grande", 1000.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }
    @Test fun taximetro_resumo() { DadosDeTeste.carregar(); DadosDeTeste.taxiFinalizado(); fatias("taximetro_resumo", 1500.dp) { Celular(Aba.TAXIMETRO) { TelaTaximetro(it) } } }

    /** Faixa do taxímetro ligado aparece quando você está em outra aba. */
    @Test fun faixa_taximetro() { DadosDeTeste.carregar(); DadosDeTeste.taxiRodando(); fatias("faixa_taximetro", 500.dp) { Celular(Aba.CLIENTES) { TelaClientes(it) } } }

    // ───────────── Orçamento ─────────────

    @Test fun orcamento_vazio() { DadosDeTeste.carregar(); fatias("orcamento_vazio", 1500.dp) { Celular(Aba.ORCAMENTO) { TelaOrcamento(it) } } }

    @Composable
    private fun OrcamentoCalculado() = Celular(Aba.ORCAMENTO) { state ->
        state.form.apply {
            clienteId = "c1"; nome = DadosDeTeste.clientes[0].nome; telefone = DadosDeTeste.clientes[0].telefone
            origem = casa; paradas.clear(); paradas.add(shopping); destino = aeroporto
            resultado = Resultado(42.7, 68.0, 50.0, 18.0, 1234.56, true, 1)
        }
        TelaOrcamento(state)
    }

    @Test fun orcamento_resultado() { DadosDeTeste.carregar(); fatias("orcamento_resultado", 2500.dp) { OrcamentoCalculado() } }
    @Test fun orcamento_resultado_claro() { DadosDeTeste.carregar(); fatias("orcamento_resultado_claro", 2500.dp, escuro = false) { OrcamentoCalculado() } }

    // ───────────── Agenda ─────────────

    @Test fun agenda_proximas() { DadosDeTeste.carregar(); fatias("agenda_proximas", 1500.dp) { Celular(Aba.AGENDA) { TelaAgenda(it) } } }
    @Test fun agenda_historico() { DadosDeTeste.carregar(); fatias("agenda_historico", 1500.dp) { Celular(Aba.AGENDA) { TelaAgenda(it, historicoInicial = true) } } }
    @Test fun agenda_vazia() { DadosDeTeste.carregar(comCorridas = false); fatias("agenda_vazia", 500.dp) { Celular(Aba.AGENDA) { TelaAgenda(it) } } }
    @Test fun agenda_claro() { DadosDeTeste.carregar(); fatias("agenda_claro", 1000.dp, escuro = false) { Celular(Aba.AGENDA) { TelaAgenda(it) } } }
    @Test fun agenda_fonte_grande() { fonteGrande(); DadosDeTeste.carregar(); fatias("agenda_fonte_grande", 1500.dp) { Celular(Aba.AGENDA) { TelaAgenda(it) } } }

    // ───────────── Clientes ─────────────

    @Test fun clientes() { DadosDeTeste.carregar(); fatias("clientes", 1000.dp) { Celular(Aba.CLIENTES) { TelaClientes(it) } } }
    @Test fun cliente_editar() { DadosDeTeste.carregar(); fatias("cliente_editar", 2000.dp) { EditarCliente(DadosDeTeste.clientes[0], null) {} } }

    // ───────────── Mais ─────────────

    @Test fun mais() { DadosDeTeste.carregar(); fatias("mais", 1000.dp) { Celular(Aba.MAIS) { TelaMais(it) } } }
    @Test fun mais_claro() { DadosDeTeste.carregar(); fatias("mais_claro", 1000.dp, escuro = false) { Celular(Aba.MAIS) { TelaMais(it) } } }
    @Test fun tarifas() { DadosDeTeste.carregar(); fatias("tarifas", 2000.dp) { TelaTarifas {} } }
    @Test fun configuracoes() { DadosDeTeste.carregar(); fatias("configuracoes", 1500.dp) { TelaConfig {} } }
    @Test fun backup() { DadosDeTeste.carregar(); fatias("backup", 1000.dp) { TelaBackup {} } }
    @Test fun cafezinho() { DadosDeTeste.carregar(); fatias("cafezinho", 1500.dp) { TelaCafezinho {} } }

    /** Confere se as fontes do Oficina carregaram no ambiente dos prints. */
    @Test fun fontes() = fatias("fontes", 500.dp) {
        val fontes = FontesOficina(LocalContext.current)
        Column(Modifier.padding(16.dp)) {
            Hud("Fontes faltando: ${fontes.faltando.ifEmpty { listOf("nenhuma") }.joinToString()}")
            TituloDisplay("Archivo expandido com *serif* itálico.")
        }
    }
}
