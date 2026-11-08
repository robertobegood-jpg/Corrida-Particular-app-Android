package com.corridaparticular.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.corridaparticular.app.data.Endereco
import java.util.Locale

/** Abre a rota em apps de GPS (Google Maps, Waze ou outro instalado). */
object Navegar {

    private const val PACOTE_MAPS = "com.google.android.apps.maps"
    private const val PACOTE_WAZE = "com.waze"

    private fun local(e: Endereco): String =
        if (e.temCoord) String.format(Locale.US, "%.6f,%.6f", e.lat, e.lon) else e.texto

    private fun abrir(ctx: Context, url: String, pacote: String?): Boolean {
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (pacote != null) {
            try {
                ctx.startActivity(Intent(base).setPackage(pacote))
                return true
            } catch (e: ActivityNotFoundException) { /* app não instalado: abre pelo navegador */ }
        }
        return try {
            ctx.startActivity(base)
            true
        } catch (e: ActivityNotFoundException) {
            ctx.toast("Nenhum app disponível para abrir a rota")
            false
        }
    }

    /**
     * Google Maps com a rota completa saindo da posição atual:
     * pontos intermediários (embarque e paradas) e destino final.
     */
    fun mapsRotaCompleta(ctx: Context, pontos: List<Endereco>) {
        val validos = pontos.filter { it.texto.isNotBlank() || it.temCoord }
        if (validos.isEmpty()) { ctx.toast("Informe pelo menos um endereço"); return }
        val destino = validos.last()
        val meio = validos.dropLast(1)
        var url = "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=${Uri.encode(local(destino))}"
        if (meio.isNotEmpty()) url += "&waypoints=" + Uri.encode(meio.joinToString("|") { local(it) })
        abrir(ctx, url, PACOTE_MAPS)
    }

    /** Google Maps até um único ponto. */
    fun mapsAte(ctx: Context, e: Endereco) {
        abrir(ctx, "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=${Uri.encode(local(e))}", PACOTE_MAPS)
    }

    /** Waze até um ponto (o Waze não aceita paradas no link, então vai um ponto por vez). */
    fun wazeAte(ctx: Context, e: Endereco) {
        val url = if (e.temCoord) {
            String.format(Locale.US, "https://waze.com/ul?ll=%.6f,%.6f&navigate=yes", e.lat, e.lon)
        } else {
            "https://waze.com/ul?q=${Uri.encode(e.texto)}&navigate=yes"
        }
        abrir(ctx, url, PACOTE_WAZE)
    }

    /** Qualquer app de mapas instalado (o Android pergunta qual usar). */
    fun outroApp(ctx: Context, e: Endereco) {
        val q = if (e.temCoord) {
            String.format(Locale.US, "%.6f,%.6f", e.lat, e.lon) + "(" + Uri.encode(e.apelido.ifBlank { e.texto }) + ")"
        } else Uri.encode(e.texto)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
        try {
            ctx.startActivity(Intent.createChooser(intent, "Abrir com").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (ex: ActivityNotFoundException) {
            ctx.toast("Nenhum app de mapas instalado")
        }
    }
}
