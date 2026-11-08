package com.corridaparticular.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmareloTaxi = Color(0xFFF2B705)
val PainelEscuro = Color(0xFF14181F)
val VerdeOk = Color(0xFF2E7D32)

private val Claro = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3F4),
    onPrimaryContainer = Color(0xFF0B1F38),
    secondary = Color(0xFF8A6400),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A6),
    onSecondaryContainer = Color(0xFF2B1F00),
    tertiary = VerdeOk,
    background = Color(0xFFF6F7FA),
    surface = Color(0xFFF6F7FA),
)

private val Escuro = darkColorScheme(
    primary = AmareloTaxi,
    onPrimary = Color(0xFF1E1600),
    primaryContainer = Color(0xFF3A3220),
    onPrimaryContainer = Color(0xFFFFE7A6),
    secondary = Color(0xFFA9C7F0),
    onSecondary = Color(0xFF0B1F38),
    secondaryContainer = Color(0xFF26364D),
    onSecondaryContainer = Color(0xFFD7E3F4),
    tertiary = Color(0xFF81C784),
)

@Composable
fun Tema(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro, content = content)
}
