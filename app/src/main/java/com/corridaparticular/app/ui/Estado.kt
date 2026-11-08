package com.corridaparticular.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.corridaparticular.app.data.Cliente
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.data.novoId

enum class Aba(val titulo: String, val icone: ImageVector) {
    TAXIMETRO("Taxímetro", Icons.Filled.LocalTaxi),
    ORCAMENTO("Orçamento", Icons.Filled.Calculate),
    AGENDA("Agenda", Icons.Filled.CalendarMonth),
    CLIENTES("Clientes", Icons.Filled.People),
    MAIS("Mais", Icons.Filled.Menu),
}

/** Dados que vão para o taxímetro quando ele é aberto a partir de outra tela. */
data class TaxiPreparo(
    val clienteId: String? = null,
    val nome: String = "",
    val telefone: String = "",
    val origem: Endereco = Endereco(),
    val paradas: List<Endereco> = emptyList(),
    val destino: Endereco = Endereco(),
    val agendadaId: String? = null,
)

data class Resultado(
    val km: Double,
    val minutos: Double,
    val minutosSemTransito: Double,
    val minutosAtraso: Double,
    val valor: Double,
    val bandeira2: Boolean,
    val nParadas: Int,
)

/** Formulário de orçamento: fica guardado mesmo ao trocar de aba. */
class OrcamentoForm {
    var editandoId by mutableStateOf<String?>(null)
    var criadoEm by mutableStateOf<Long?>(null)
    var clienteId by mutableStateOf<String?>(null)
    var nome by mutableStateOf("")
    var telefone by mutableStateOf("")
    var origem by mutableStateOf(Endereco())
    val paradas = mutableStateListOf<Endereco>()
    var destino by mutableStateOf(Endereco())
    var dataHora by mutableStateOf<Long?>(null)   // null = agora
    var manual by mutableStateOf(false)
    var kmManual by mutableStateOf("")
    var minManual by mutableStateOf("")
    var obs by mutableStateOf("")
    var resultado by mutableStateOf<Resultado?>(null)

    fun limpar() {
        editandoId = null; criadoEm = null; clienteId = null; nome = ""; telefone = ""
        origem = Endereco(); paradas.clear(); destino = Endereco(); dataHora = null
        manual = false; kmManual = ""; minManual = ""; obs = ""; resultado = null
    }

    fun aplicarCliente(c: Cliente?) {
        clienteId = c?.id
        if (c != null) {
            nome = c.nome
            telefone = c.telefone
            if (c.origens.isNotEmpty()) origem = c.origens.first()
        }
        resultado = null
    }

    fun carregar(c: Corrida) {
        limpar()
        editandoId = c.id
        criadoEm = c.criadoEm
        clienteId = c.clienteId
        nome = c.clienteNome
        telefone = c.telefone
        origem = c.origem
        paradas.addAll(c.paradas)
        destino = c.destino
        dataHora = c.dataHora
        obs = c.obs
        if (c.distanciaKm > 0 || c.valor > 0) {
            resultado = Resultado(
                c.distanciaKm, c.minutos, c.minutos - c.minutosAtraso, c.minutosAtraso,
                c.valor, c.bandeira2, c.paradas.size
            )
        }
    }

    fun paraCorrida(status: String): Corrida? {
        val r = resultado ?: return null
        val cli = Repo.cliente(clienteId)
        return Corrida(
            id = editandoId ?: novoId(),
            clienteId = cli?.id,
            clienteNome = cli?.nome ?: nome.ifBlank { "Cliente avulso" },
            telefone = cli?.telefone ?: telefone,
            origem = origem,
            paradas = paradas.filter { it.texto.isNotBlank() },
            destino = destino,
            dataHora = dataHora ?: System.currentTimeMillis(),
            distanciaKm = r.km,
            minutos = r.minutos,
            minutosAtraso = r.minutosAtraso,
            valor = r.valor,
            bandeira2 = r.bandeira2,
            status = status,
            tipo = Corrida.TIPO_ESTIMATIVA,
            obs = obs,
            criadoEm = criadoEm ?: System.currentTimeMillis(),
        )
    }
}

class AppState {
    var aba by mutableStateOf(Aba.TAXIMETRO)
    val form = OrcamentoForm()
    var taxiPreparo by mutableStateOf(TaxiPreparo())

    fun novoOrcamento(cliente: Cliente? = null) {
        form.limpar()
        form.aplicarCliente(cliente)
        aba = Aba.ORCAMENTO
    }

    fun editarCorrida(c: Corrida) {
        form.carregar(c)
        aba = Aba.ORCAMENTO
    }

    fun abrirTaximetro(p: TaxiPreparo) {
        taxiPreparo = p
        aba = Aba.TAXIMETRO
    }
}

/** Estado único do app (sobrevive à troca de abas). */
object Sessao {
    val app = AppState()
}
