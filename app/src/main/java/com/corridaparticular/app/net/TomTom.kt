package com.corridaparticular.app.net

import com.corridaparticular.app.data.Endereco
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Integração com a API da TomTom (busca de endereços e rotas com trânsito).
 * Chave gratuita em https://developer.tomtom.com
 */
object TomTom {

    class Erro(msg: String) : Exception(msg)

    data class Rota(
        val km: Double,
        val minutos: Double,             // tempo previsto com trânsito
        val minutosSemTransito: Double,
        val minutosAtraso: Double,
    )

    private fun cod(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun get(url: String): JSONObject {
        val con = URL(url).openConnection() as HttpURLConnection
        try {
            con.connectTimeout = 15_000
            con.readTimeout = 20_000
            val codigo = con.responseCode
            val corpo = (if (codigo in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (codigo !in 200..299) {
                throw Erro(
                    when (codigo) {
                        401, 403 -> "Chave TomTom inválida ou sem permissão. Confira em Mais › Configurações."
                        429 -> "Limite diário gratuito da TomTom atingido. Tente mais tarde."
                        400 -> "Não foi possível calcular a rota para esses endereços."
                        else -> "Erro TomTom ($codigo)"
                    }
                )
            }
            return JSONObject(corpo)
        } catch (e: Erro) {
            throw e
        } catch (e: Exception) {
            throw Erro("Sem conexão com a internet ou servidor indisponível.")
        } finally {
            con.disconnect()
        }
    }

    /** Busca endereços e locais (shoppings, hospitais, etc.) no Brasil. */
    suspend fun buscar(chave: String, texto: String, lat: Double? = null, lon: Double? = null): List<Endereco> =
        withContext(Dispatchers.IO) {
            var url = "https://api.tomtom.com/search/2/search/${cod(texto)}.json" +
                "?key=${cod(chave)}&countrySet=BR&language=pt-BR&limit=6"
            if (lat != null && lon != null) url += "&lat=$lat&lon=$lon"
            val res = get(url).optJSONArray("results") ?: return@withContext emptyList()
            (0 until res.length()).mapNotNull { i ->
                val r = res.optJSONObject(i) ?: return@mapNotNull null
                val pos = r.optJSONObject("position") ?: return@mapNotNull null
                val end = r.optJSONObject("address")?.optString("freeformAddress").orEmpty()
                val poi = r.optJSONObject("poi")?.optString("name").orEmpty()
                val nome = when {
                    poi.isNotBlank() && end.isNotBlank() -> "$poi - $end"
                    poi.isNotBlank() -> poi
                    else -> end
                }
                if (nome.isBlank()) null
                else Endereco(texto = nome, lat = pos.optDouble("lat"), lon = pos.optDouble("lon"))
            }
        }

    /** Endereço a partir de coordenadas (para "usar minha localização"). */
    suspend fun enderecoDe(chave: String, lat: Double, lon: Double): Endereco = withContext(Dispatchers.IO) {
        val url = "https://api.tomtom.com/search/2/reverseGeocode/$lat,$lon.json?key=${cod(chave)}&language=pt-BR"
        val a = get(url).optJSONArray("addresses")?.optJSONObject(0)
        val txt = a?.optJSONObject("address")?.optString("freeformAddress").orEmpty()
        Endereco(texto = txt.ifBlank { String.format(Locale.US, "%.5f, %.5f", lat, lon) }, lat = lat, lon = lon)
    }

    /**
     * Rota de carro passando por todos os pontos, na ordem.
     * Se [quando] for no futuro, a TomTom usa a previsão de trânsito daquele horário.
     */
    suspend fun rota(chave: String, pontos: List<Endereco>, quando: Long): Rota = withContext(Dispatchers.IO) {
        require(pontos.size >= 2) { "Informe origem e destino." }
        val locs = pontos.joinToString(":") { "${it.lat},${it.lon}" }
        var url = "https://api.tomtom.com/routing/1/calculateRoute/$locs/json" +
            "?key=${cod(chave)}&traffic=true&travelMode=car&routeType=fastest&computeTravelTimeFor=all"
        if (quando > System.currentTimeMillis() + 2 * 60_000) {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(quando))
            url += "&departAt=${cod(iso)}"
        }
        val rota = get(url).optJSONArray("routes")?.optJSONObject(0)
            ?: throw Erro("Nenhuma rota encontrada.")
        val s = rota.getJSONObject("summary")
        val comTransito = s.optDouble("travelTimeInSeconds", 0.0)
        val semTransito = s.optDouble("noTrafficTravelTimeInSeconds", comTransito)
        Rota(
            km = s.optDouble("lengthInMeters", 0.0) / 1000.0,
            minutos = comTransito / 60.0,
            minutosSemTransito = semTransito / 60.0,
            minutosAtraso = s.optDouble("trafficDelayInSeconds", 0.0).coerceAtLeast(comTransito - semTransito) / 60.0,
        )
    }
}
