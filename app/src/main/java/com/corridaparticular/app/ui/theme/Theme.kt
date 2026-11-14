package com.corridaparticular.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Tokens de cor do sistema Oficina. */
@Immutable
data class CoresOficina(
    val bg: Color,
    val bgRaised: Color,
    val bgSunken: Color,
    val ink: Color,
    val inkStrong: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val lineStrong: Color,
    val accent: Color,
    val accentInk: Color,
    val invertBg: Color,
    val invertFg: Color,
    val danger: Color,
    val verde: Color,
    val verdeInk: Color,
    val ambar: Color,
    val ambarInk: Color,
    val azul: Color,
    val azulInk: Color,
    val vermelhao: Color,
    val vermelhaoInk: Color,
    /** Matizes categóricas para gráficos (preenchimento; iguais nos dois temas). */
    val hues: List<Color>,
    val escuro: Boolean
)

/** "#rrggbb" ou "#rrggbbaa" → Color. */
private fun hex(valor: String): Color {
    val s = valor.removePrefix("#")
    val rgb = s.substring(0, 6).toLong(16)
    val alfa = if (s.length == 8) s.substring(6, 8).toLong(16) else 0xFFL
    return Color((alfa shl 24) or rgb)
}

/** Ordem fixa: azul, índigo, âmbar, vermelhão, violeta, petróleo, magenta, lima, menta, ferrugem, ciano, carmim. */
private val HUES = listOf(
    "#2f7bf5", "#4c6ef5", "#f2a516", "#e8573f", "#8b5cf6", "#178a9c",
    "#e0368f", "#7bb51c", "#02da96", "#c2672b", "#14a9c4", "#e8204f"
).map { hex(it) }

val CoresEscuras = CoresOficina(
    bg = hex("#121316"),
    bgRaised = hex("#101014"),
    bgSunken = hex("#060607"),
    ink = hex("#f2f1ee"),
    inkStrong = hex("#ffffff"),
    muted = hex("#f2f1ee99"),
    faint = hex("#f2f1ee61"),
    line = hex("#f2f1ee26"),
    lineStrong = hex("#f2f1ee66"),
    accent = hex("#2d81ff"),
    accentInk = hex("#74aaff"),
    invertBg = hex("#ffffff"),
    invertFg = hex("#0b0b0c"),
    danger = hex("#ff6259"),
    verde = hex("#1f9d55"),
    verdeInk = hex("#5ee089"),
    ambar = hex("#f2a516"),
    ambarInk = hex("#ffc14d"),
    azul = hex("#2f7bf5"),
    azulInk = hex("#7fb0ff"),
    vermelhao = hex("#e8573f"),
    vermelhaoInk = hex("#ff8a70"),
    hues = HUES,
    escuro = true
)

val CoresClaras = CoresOficina(
    bg = hex("#f2f1ee"),
    bgRaised = hex("#faf9f7"),
    bgSunken = hex("#e9e8e4"),
    ink = hex("#101014"),
    inkStrong = hex("#000000"),
    muted = hex("#1010149e"),
    faint = hex("#1010146b"),
    line = hex("#10101429"),
    lineStrong = hex("#10101466"),
    accent = hex("#2d81ff"),
    accentInk = hex("#1659c4"),
    invertBg = hex("#101014"),
    invertFg = hex("#f2f1ee"),
    danger = hex("#b3261e"),
    verde = hex("#1f9d55"),
    verdeInk = hex("#136938"),
    ambar = hex("#f2a516"),
    ambarInk = hex("#8a5800"),
    azul = hex("#2f7bf5"),
    azulInk = hex("#1a5bc4"),
    vermelhao = hex("#e8573f"),
    vermelhaoInk = hex("#b83a24"),
    hues = HUES,
    escuro = false
)

private val PESO_DISPLAY = FontWeight(620)

/** Estilos de texto do sistema Oficina. */
@Immutable
class TiposOficina(fontes: FontesOficina) {
    val serif: FontFamily = fontes.serif

    val displayLg = TextStyle(
        fontFamily = fontes.display, fontWeight = PESO_DISPLAY,
        fontSize = 48.sp, lineHeight = 49.sp, letterSpacing = (-0.035).em
    )
    val displayMd = TextStyle(
        fontFamily = fontes.display, fontWeight = PESO_DISPLAY,
        fontSize = 36.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em
    )
    val heading = TextStyle(
        fontFamily = fontes.display, fontWeight = PESO_DISPLAY,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em
    )
    val bodyLg = TextStyle(
        fontFamily = fontes.body, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 29.sp
    )
    val body = TextStyle(
        fontFamily = fontes.body, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 26.sp
    )
    val bodySm = TextStyle(
        fontFamily = fontes.body, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    )
    val hud = TextStyle(
        fontFamily = fontes.mono, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.15.em
    )
    val buttonLabel = TextStyle(
        fontFamily = fontes.monoNegrito, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.12.em
    )
}

val LocalCores = staticCompositionLocalOf { CoresEscuras }
val LocalTipos = staticCompositionLocalOf<TiposOficina> { error("CorridaTheme não aplicado") }

/** Acesso rápido: Oficina.cores.bg, Oficina.tipos.hud */
object Oficina {
    val cores: CoresOficina
        @Composable @ReadOnlyComposable get() = LocalCores.current
    val tipos: TiposOficina
        @Composable @ReadOnlyComposable get() = LocalTipos.current
}

private fun esquemaMaterial(c: CoresOficina) =
    (if (c.escuro) darkColorScheme() else lightColorScheme()).copy(
        primary = c.invertBg,
        onPrimary = c.invertFg,
        primaryContainer = c.bgSunken,
        onPrimaryContainer = c.ink,
        secondary = c.accent,
        onSecondary = Color.White,
        secondaryContainer = c.bgSunken,
        onSecondaryContainer = c.ink,
        tertiary = c.accentInk,
        background = c.bg,
        onBackground = c.ink,
        surface = c.bg,
        onSurface = c.ink,
        surfaceVariant = c.bgSunken,
        onSurfaceVariant = c.muted,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = c.bgSunken,
        surfaceContainerLow = c.bgRaised,
        surfaceContainer = c.bgRaised,
        surfaceContainerHigh = c.bgRaised,
        surfaceContainerHighest = c.bgRaised,
        outline = c.lineStrong,
        outlineVariant = c.line,
        error = c.danger,
        onError = c.invertFg,
        inverseSurface = c.invertBg,
        inverseOnSurface = c.invertFg,
        inversePrimary = c.accentInk,
        scrim = Color.Black.copy(alpha = 0.6f)
    )

private fun tipografiaMaterial(t: TiposOficina) = Typography(
    displayLarge = t.displayLg,
    displayMedium = t.displayLg,
    displaySmall = t.displayMd,
    headlineLarge = t.displayMd,
    headlineMedium = t.displayMd,
    headlineSmall = t.heading,
    titleLarge = t.heading,
    titleMedium = t.body.copy(fontWeight = FontWeight.Medium),
    titleSmall = t.bodySm.copy(fontWeight = FontWeight.Medium),
    bodyLarge = t.body,
    bodyMedium = t.bodySm,
    bodySmall = t.bodySm.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = t.buttonLabel,
    labelMedium = t.hud,
    labelSmall = t.hud
)

@Composable
fun CorridaTheme(
    escuro: Boolean,
    fontes: FontesOficina,
    content: @Composable () -> Unit
) {
    val cores = if (escuro) CoresEscuras else CoresClaras
    val tipos = remember(fontes) { TiposOficina(fontes) }
    // Cantos retos em tudo.
    val reto = RoundedCornerShape(0.dp)

    CompositionLocalProvider(LocalCores provides cores, LocalTipos provides tipos) {
        MaterialTheme(
            colorScheme = esquemaMaterial(cores),
            typography = tipografiaMaterial(tipos),
            shapes = Shapes(reto, reto, reto, reto, reto),
            content = content
        )
    }
}

/**
 * Visor do taxímetro: painel escuro fixo, igual nos dois temas, com o valor em amarelo táxi.
 * É a única exceção de cor do sistema Oficina neste app (o amarelo também está no ícone).
 */
object Visor {
    val fundo = hex("#0b0b0c")
    val tinta = hex("#f2f1ee")
    val apagado = hex("#f2f1ee99")
    val linha = hex("#f2f1ee33")
    val amarelo = hex("#f2b705")
    val verde = hex("#5ee089")
    val vermelho = hex("#ff8a70")
}
