package com.corridaparticular.app.util

import android.content.Context
import android.widget.Toast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(PT_BR)

fun Double.reais(): String = moeda.format(this)
fun Double.kmTexto(): String = String.format(PT_BR, "%.1f km", this)
fun Double.decimal(casas: Int = 2): String = String.format(PT_BR, "%.${casas}f", this)

fun minutosTexto(min: Double): String {
    val m = min.roundToInt().coerceAtLeast(0)
    return if (m < 60) "$m min" else String.format(PT_BR, "%dh %02dmin", m / 60, m % 60)
}

fun duracaoTexto(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(PT_BR, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun Long.dataHoraTexto(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", PT_BR).format(Date(this))
fun Long.horaTexto(): String = SimpleDateFormat("HH:mm", PT_BR).format(Date(this))
fun Long.dataTexto(): String = SimpleDateFormat("dd/MM/yyyy", PT_BR).format(Date(this))

fun inicioDoDia(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = t
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun inicioDoMes(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = inicioDoDia(t)
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

fun Long.diaTitulo(): String {
    val diff = ((inicioDoDia(this) - inicioDoDia(System.currentTimeMillis())).toDouble() / 86_400_000.0).roundToInt()
    return when (diff) {
        0 -> "Hoje"
        1 -> "Amanhã"
        -1 -> "Ontem"
        else -> SimpleDateFormat("EEEE, dd/MM/yyyy", PT_BR).format(Date(this))
            .replaceFirstChar { it.uppercase() }
    }
}

/** Aceita "3,20", "3.20" e "1.250,50". */
fun String.paraDouble(): Double? {
    val t = trim()
    if (t.isEmpty()) return null
    val normal = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
    return normal.toDoubleOrNull()
}

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
