package com.corridaparticular.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun novoId(): String = UUID.randomUUID().toString()

fun <T> JSONArray?.lista(f: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(f) }
}

fun List<Endereco>.paraJson(): JSONArray = JSONArray().also { a -> forEach { a.put(it.toJson()) } }

data class Endereco(
    val apelido: String = "",
    val texto: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
) {
    val temCoord: Boolean get() = lat != null && lon != null
    val rotulo: String get() = if (apelido.isNotBlank()) "$apelido – $texto" else texto

    fun toJson(): JSONObject = JSONObject().apply {
        put("apelido", apelido)
        put("texto", texto)
        lat?.let { put("lat", it) }
        lon?.let { put("lon", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = Endereco(
            apelido = o.optString("apelido"),
            texto = o.optString("texto"),
            lat = if (o.has("lat")) o.optDouble("lat") else null,
            lon = if (o.has("lon")) o.optDouble("lon") else null,
        )
    }
}

data class Cliente(
    val id: String = novoId(),
    val nome: String = "",
    val telefone: String = "",
    val obs: String = "",
    val origens: List<Endereco> = emptyList(),
    val destinos: List<Endereco> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nome", nome)
        put("telefone", telefone)
        put("obs", obs)
        put("origens", origens.paraJson())
        put("destinos", destinos.paraJson())
    }

    companion object {
        fun fromJson(o: JSONObject) = Cliente(
            id = o.optString("id").ifEmpty { novoId() },
            nome = o.optString("nome"),
            telefone = o.optString("telefone"),
            obs = o.optString("obs"),
            origens = o.optJSONArray("origens").lista { Endereco.fromJson(it) },
            destinos = o.optJSONArray("destinos").lista { Endereco.fromJson(it) },
        )
    }
}

data class Corrida(
    val id: String = novoId(),
    val clienteId: String? = null,
    val clienteNome: String = "",
    val telefone: String = "",
    val origem: Endereco = Endereco(),
    val paradas: List<Endereco> = emptyList(),
    val destino: Endereco = Endereco(),
    val dataHora: Long = System.currentTimeMillis(),
    val distanciaKm: Double = 0.0,
    val minutos: Double = 0.0,
    val minutosAtraso: Double = 0.0,
    val minutosParado: Double = 0.0,
    val minutosEspera: Double = 0.0,
    val valorEspera: Double = 0.0,
    val valor: Double = 0.0,
    val bandeira2: Boolean = false,
    val status: String = STATUS_AGENDADA,
    val tipo: String = TIPO_ESTIMATIVA,
    val obs: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("clienteId", clienteId ?: "")
        put("clienteNome", clienteNome)
        put("telefone", telefone)
        put("origem", origem.toJson())
        put("paradas", paradas.paraJson())
        put("destino", destino.toJson())
        put("dataHora", dataHora)
        put("distanciaKm", distanciaKm)
        put("minutos", minutos)
        put("minutosAtraso", minutosAtraso)
        put("minutosParado", minutosParado)
        put("minutosEspera", minutosEspera)
        put("valorEspera", valorEspera)
        put("valor", valor)
        put("bandeira2", bandeira2)
        put("status", status)
        put("tipo", tipo)
        put("obs", obs)
        put("criadoEm", criadoEm)
    }

    companion object {
        const val STATUS_AGENDADA = "AGENDADA"
        const val STATUS_CONCLUIDA = "CONCLUIDA"
        const val STATUS_CANCELADA = "CANCELADA"
        const val TIPO_ESTIMATIVA = "ESTIMATIVA"
        const val TIPO_TAXIMETRO = "TAXIMETRO"

        fun fromJson(o: JSONObject) = Corrida(
            id = o.optString("id").ifEmpty { novoId() },
            clienteId = o.optString("clienteId").ifEmpty { null },
            clienteNome = o.optString("clienteNome"),
            telefone = o.optString("telefone"),
            origem = o.optJSONObject("origem")?.let { Endereco.fromJson(it) } ?: Endereco(),
            paradas = o.optJSONArray("paradas").lista { Endereco.fromJson(it) },
            destino = o.optJSONObject("destino")?.let { Endereco.fromJson(it) } ?: Endereco(),
            dataHora = o.optLong("dataHora", System.currentTimeMillis()),
            distanciaKm = o.optDouble("distanciaKm", 0.0),
            minutos = o.optDouble("minutos", 0.0),
            minutosAtraso = o.optDouble("minutosAtraso", 0.0),
            minutosParado = o.optDouble("minutosParado", 0.0),
            minutosEspera = o.optDouble("minutosEspera", 0.0),
            valorEspera = o.optDouble("valorEspera", 0.0),
            valor = o.optDouble("valor", 0.0),
            bandeira2 = o.optBoolean("bandeira2", false),
            status = o.optString("status", STATUS_AGENDADA),
            tipo = o.optString("tipo", TIPO_ESTIMATIVA),
            obs = o.optString("obs"),
            criadoEm = o.optLong("criadoEm", System.currentTimeMillis()),
        )
    }
}

data class Tarifas(
    val bandeirada: Double = 5.50,
    val porKm: Double = 3.20,
    val porMinuto: Double = 0.40,
    val porHoraParada: Double = 40.0,
    val esperaGratisMin: Int = 5,
    val esperaPorMinuto: Double = 0.50,
    val valorMinimo: Double = 15.0,
    val bandeira2Pct: Double = 20.0,
    val b2HoraInicio: Int = 20,
    val b2HoraFim: Int = 6,
    val b2Domingo: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("bandeirada", bandeirada)
        put("porKm", porKm)
        put("porMinuto", porMinuto)
        put("porHoraParada", porHoraParada)
        put("esperaGratisMin", esperaGratisMin)
        put("esperaPorMinuto", esperaPorMinuto)
        put("valorMinimo", valorMinimo)
        put("bandeira2Pct", bandeira2Pct)
        put("b2HoraInicio", b2HoraInicio)
        put("b2HoraFim", b2HoraFim)
        put("b2Domingo", b2Domingo)
    }

    companion object {
        fun fromJson(o: JSONObject): Tarifas {
            val p = Tarifas()
            return Tarifas(
                bandeirada = o.optDouble("bandeirada", p.bandeirada),
                porKm = o.optDouble("porKm", p.porKm),
                porMinuto = o.optDouble("porMinuto", p.porMinuto),
                porHoraParada = o.optDouble("porHoraParada", p.porHoraParada),
                esperaGratisMin = o.optInt("esperaGratisMin", p.esperaGratisMin),
                esperaPorMinuto = o.optDouble("esperaPorMinuto", p.esperaPorMinuto),
                valorMinimo = o.optDouble("valorMinimo", p.valorMinimo),
                bandeira2Pct = o.optDouble("bandeira2Pct", p.bandeira2Pct),
                b2HoraInicio = o.optInt("b2HoraInicio", p.b2HoraInicio),
                b2HoraFim = o.optInt("b2HoraFim", p.b2HoraFim),
                b2Domingo = o.optBoolean("b2Domingo", p.b2Domingo),
            )
        }
    }
}

data class Config(
    val nome: String = "",
    val telefone: String = "",
    val veiculo: String = "",
    val placa: String = "",
    val pix: String = "",
    val chaveTomTom: String = "",
    val lembreteMin: Int = 30,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("nome", nome)
        put("telefone", telefone)
        put("veiculo", veiculo)
        put("placa", placa)
        put("pix", pix)
        put("chaveTomTom", chaveTomTom)
        put("lembreteMin", lembreteMin)
    }

    companion object {
        fun fromJson(o: JSONObject) = Config(
            nome = o.optString("nome"),
            telefone = o.optString("telefone"),
            veiculo = o.optString("veiculo"),
            placa = o.optString("placa"),
            pix = o.optString("pix"),
            chaveTomTom = o.optString("chaveTomTom"),
            lembreteMin = o.optInt("lembreteMin", 30),
        )
    }
}
