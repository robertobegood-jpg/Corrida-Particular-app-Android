package com.corridaparticular.app.data

import java.util.Calendar
import kotlin.math.max
import kotlin.math.round

object Calculo {

    /** Bandeira 2: horário noturno configurado e/ou domingo. */
    fun ehBandeira2(t: Tarifas, quando: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = quando }
        if (t.b2Domingo && c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true
        val h = c.get(Calendar.HOUR_OF_DAY)
        val ini = t.b2HoraInicio
        val fim = t.b2HoraFim
        return when {
            ini == fim -> false
            ini > fim -> h >= ini || h < fim      // ex.: 20h às 6h
            else -> h in ini until fim            // ex.: 0h às 5h
        }
    }

    data class Detalhe(
        val bandeirada: Double,
        val km: Double,
        val tempo: Double,
        val adicionalB2: Double,
        val total: Double,
        val aplicouMinimo: Boolean,
    )

    /** Estimativa: bandeirada + km + minutos de viagem (com trânsito). A espera é cobrada à parte, se houver. */
    fun estimativa(t: Tarifas, km: Double, minutos: Double, b2: Boolean): Detalhe {
        val vKm = km * t.porKm
        val vTempo = minutos * t.porMinuto
        val base = t.bandeirada + vKm + vTempo
        val adicional = if (b2) base * t.bandeira2Pct / 100.0 else 0.0
        val bruto = base + adicional
        val total = arred(max(bruto, t.valorMinimo))
        return Detalhe(t.bandeirada, vKm, vTempo, adicional, total, bruto < t.valorMinimo)
    }

    /** Parte cobrável de uma espera: o que passar da tolerância grátis. */
    fun msCobravel(t: Tarifas, msEspera: Long): Long =
        (msEspera - t.esperaGratisMin * 60_000L).coerceAtLeast(0L)

    /** Valor da espera: cada minuto (proporcional) após a tolerância. */
    fun valorEspera(t: Tarifas, msCobrado: Long): Double =
        arred(msCobrado / 60_000.0 * t.esperaPorMinuto)

    /**
     * Taxímetro: bandeirada + km rodado + hora parada (trânsito, abaixo de ~10 km/h),
     * com bandeira 2 sobre esse total, mais a espera do passageiro após a tolerância.
     */
    fun taximetro(t: Tarifas, km: Double, msParado: Long, msEsperaCobrada: Long, b2: Boolean): Double {
        val base = t.bandeirada + km * t.porKm + (msParado / 3_600_000.0) * t.porHoraParada
        val v = if (b2) base * (1 + t.bandeira2Pct / 100.0) else base
        return arred(v + valorEspera(t, msEsperaCobrada))
    }

    fun arred(v: Double): Double = round(v * 100.0) / 100.0
}
