package com.corridaparticular.app

import com.corridaparticular.app.data.Cliente
import com.corridaparticular.app.data.Config
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.data.Tarifas
import com.corridaparticular.app.taximetro.TaxiDados
import com.corridaparticular.app.taximetro.TaximetroState
import java.util.Calendar

/** Dados extremos para os prints: nomes e endereços longos, valores altos, muitas paradas. */
object DadosDeTeste {

    private fun horario(diasAPartirDeHoje: Int, hora: Int, minuto: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, diasAPartirDeHoje)
        set(Calendar.HOUR_OF_DAY, hora); set(Calendar.MINUTE, minuto); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val casa = Endereco("Casa", "Rua Doutor Antônio Bento de Albuquerque Maranhão, 1234 - Jardim Paulista, São Paulo - SP", -23.57, -46.66)
    val trabalho = Endereco("Trabalho", "Avenida Brigadeiro Faria Lima, 3477 - Itaim Bibi, São Paulo - SP", -23.58, -46.68)
    val aeroporto = Endereco("", "Aeroporto Internacional de Guarulhos - Terminal 2, Guarulhos - SP", -23.43, -46.47)
    val shopping = Endereco("", "Shopping Ibirapuera - Av. Ibirapuera, 3103 - Indianópolis", -23.61, -46.66)

    val clientes = listOf(
        Cliente(id = "c1", nome = "Maria Aparecida dos Santos Oliveira", telefone = "(11) 98765-4321", origens = listOf(casa, trabalho), destinos = listOf(aeroporto)),
        Cliente(id = "c2", nome = "João Pedro", telefone = "(11) 91234-5678", origens = listOf(trabalho)),
        Cliente(id = "c3", nome = "Dona Cida", telefone = ""),
        Cliente(id = "c4", nome = "Carlos Eduardo Ferreira Lima", telefone = "(21) 99876-5432", origens = listOf(shopping)),
    )

    val corridas = listOf(
        Corrida(id = "a1", clienteId = "c1", clienteNome = clientes[0].nome, telefone = clientes[0].telefone,
            origem = casa, paradas = listOf(shopping), destino = aeroporto, dataHora = horario(0, 23, 50),
            distanciaKm = 42.7, minutos = 68.0, valor = 1234.56, bandeira2 = true),
        Corrida(id = "a2", clienteId = "c2", clienteNome = "João Pedro", origem = trabalho, destino = casa,
            dataHora = horario(1, 7, 30), distanciaKm = 8.2, minutos = 22.0, valor = 47.5),
        Corrida(id = "a3", clienteNome = "Cliente avulso", origem = shopping, destino = trabalho,
            dataHora = horario(-1, 9, 0), distanciaKm = 6.0, minutos = 18.0, valor = 38.2),
        Corrida(id = "h1", clienteId = "c1", clienteNome = clientes[0].nome, origem = casa, destino = trabalho,
            dataHora = horario(0, 8, 15), distanciaKm = 11.4, minutos = 31.0, valor = 62.9,
            status = Corrida.STATUS_CONCLUIDA, tipo = Corrida.TIPO_TAXIMETRO),
        Corrida(id = "h2", clienteNome = "Carlos Eduardo Ferreira Lima", origem = shopping, destino = aeroporto,
            dataHora = horario(-2, 14, 0), distanciaKm = 38.0, minutos = 55.0, valor = 189.0,
            status = Corrida.STATUS_CANCELADA),
        Corrida(id = "h3", clienteNome = "Dona Cida", origem = casa, destino = shopping,
            dataHora = horario(-3, 10, 40), distanciaKm = 5.1, minutos = 15.0, valor = 25.0,
            status = Corrida.STATUS_CONCLUIDA),
    )

    fun carregar(comCorridas: Boolean = true) {
        Repo.config.value = Config(nome = "Roberto", chaveTomTom = "teste")
        Repo.tarifas.value = Tarifas()
        Repo.clientes.value = clientes
        Repo.corridas.value = if (comCorridas) corridas else emptyList()
        TaximetroState.limpar()
    }

    fun taxiRodando(valor: Double = 87.4, esperando: Boolean = false, pausado: Boolean = false) {
        TaximetroState.dados.value = TaxiDados(
            ativo = true, pausado = pausado, inicio = horario(0, 10, 0),
            msTotal = 47 * 60_000L + 12_000, msParado = 9 * 60_000L, metros = 23_450.0, velocidadeKmh = 42.0,
            bandeira2 = false, valor = valor, gpsOk = true, precisao = 6f,
            clienteId = "c1", clienteNome = clientes[0].nome, telefone = clientes[0].telefone,
            origem = casa, paradas = listOf(shopping), destino = aeroporto,
            esperando = esperando, msEsperaSessao = if (esperando) 3 * 60_000L + 25_000 else 0, msEsperaTotal = if (esperando) 205_000 else 0,
        )
    }

    fun taxiFinalizado() {
        TaximetroState.dados.value = TaxiDados(
            ativo = false, finalizado = true, inicio = horario(0, 10, 0), fim = horario(0, 10, 50),
            msTotal = 50 * 60_000L, msParado = 11 * 60_000L, metros = 24_100.0, bandeira2 = true, valor = 112.35,
            clienteId = "c1", clienteNome = clientes[0].nome, origem = casa, destino = aeroporto,
            msEsperaTotal = 8 * 60_000L, msEsperaCobradaFechada = 3 * 60_000L,
        )
    }
}
