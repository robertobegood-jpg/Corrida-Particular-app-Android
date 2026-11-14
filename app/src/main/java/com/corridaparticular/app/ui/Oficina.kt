@file:OptIn(ExperimentalMaterial3Api::class)

package com.corridaparticular.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.corridaparticular.app.ui.theme.Oficina
import com.corridaparticular.app.ui.theme.Visor
import com.corridaparticular.app.util.PT_BR

/*
 * Componentes do sistema visual Oficina (o mesmo do app Em Dia):
 * cantos retos, linhas finas, rótulos em mono maiúsculo e títulos display com uma palavra em serif itálico.
 */

/* ───────────── Texto ───────────── */

/** Rótulo de instrumento: mono, 11sp, sempre em maiúsculas. */
@Composable
fun Hud(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = Oficina.cores.muted,
    estilo: TextStyle = Oficina.tipos.hud,
    maxLinhas: Int = 1,
) {
    Text(
        text = texto.uppercase(PT_BR),
        modifier = modifier,
        color = cor,
        style = estilo,
        maxLines = maxLinhas,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Eyebrow com régua: "02/ORÇAMENTO ────────". */
@Composable
fun Eyebrow(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hud(texto)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Oficina.cores.line))
    }
}

/** Título display. A palavra entre asteriscos sai em serif itálico: "Pronto para *rodar*." */
@Composable
fun TituloDisplay(
    texto: String,
    modifier: Modifier = Modifier,
    estilo: TextStyle = Oficina.tipos.displayMd,
    cor: Color = Oficina.cores.inkStrong,
) {
    val serif = Oficina.tipos.serif
    val anotado = remember(texto, serif) {
        buildAnnotatedString {
            texto.split('*').forEachIndexed { i, parte ->
                if (i % 2 == 1) {
                    withStyle(SpanStyle(fontFamily = serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) { append(parte) }
                } else {
                    append(parte)
                }
            }
        }
    }
    Text(anotado, modifier = modifier, color = cor, style = estilo)
}

/* ───────────── Botões ───────────── */

/** Ação principal: bloco invertido, rótulo mono. */
@Composable
fun BotaoPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    seta: Boolean = false,
) {
    val cores = Oficina.cores
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (!habilitado) 0.4f else if (pressionado) 0.8f else 1f)
            .background(cores.invertBg)
            .clickable(interactionSource = interacao, indication = null, enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Hud(if (seta) "$texto  →" else texto, cor = cores.invertFg, estilo = Oficina.tipos.buttonLabel)
    }
}

/** Ação secundária: contorno de 1px; ao tocar, inverte. */
@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    perigo: Boolean = false,
    compacto: Boolean = false,
) {
    val cores = Oficina.cores
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val corBase = if (perigo) cores.danger else cores.ink
    val fundo by animateColorAsState(
        if (pressionado) (if (perigo) cores.danger else cores.invertBg) else Color.Transparent,
        animationSpec = tween(120, easing = LinearEasing), label = "fundo",
    )
    val frente by animateColorAsState(
        if (pressionado) cores.invertFg else corBase,
        animationSpec = tween(120, easing = LinearEasing), label = "frente",
    )
    Box(
        modifier = modifier
            .heightIn(min = if (compacto) 40.dp else 48.dp)
            .alpha(if (habilitado) 1f else 0.4f)
            .background(fundo)
            .border(1.dp, if (perigo) cores.danger else cores.lineStrong)
            .clickable(interactionSource = interacao, indication = null, enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compacto) 8.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Hud(texto, cor = frente, estilo = Oficina.tipos.buttonLabel)
    }
}

/** Ação de texto (link HUD). */
@Composable
fun BotaoTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cor: Color = Oficina.cores.accentInk,
    habilitado: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (habilitado) 1f else 0.4f)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Hud(texto, cor = cor, estilo = Oficina.tipos.buttonLabel)
    }
}

/* ───────────── Estrutura ───────────── */

/** Hairline horizontal de 1px. */
@Composable
fun Linha(modifier: Modifier = Modifier, cor: Color = Oficina.cores.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(cor))
}

/** Hairline que atravessa a margem lateral da tela (16dp de cada lado). */
@Composable
fun LinhaSangrada(margem: Dp = 16.dp) {
    Box(
        Modifier
            .layout { medida, restricoes ->
                val extra = (margem * 2).roundToPx()
                val largura = restricoes.maxWidth + extra
                val p = medida.measure(restricoes.copy(minWidth = largura, maxWidth = largura))
                layout(restricoes.maxWidth, p.height) { p.place(-extra / 2, 0) }
            }
            .height(1.dp)
            .background(Oficina.cores.line)
    )
}

/** Marcas de canto (crosshairs) para emoldurar um bloco-chave. */
fun Modifier.marcasDeCanto(cor: Color, tamanho: Dp = 11.dp): Modifier = drawWithContent {
    drawContent()
    val t = tamanho.toPx() / 2
    val espessura = 1.dp.toPx()
    listOf(Offset(0f, 0f), Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { c ->
        drawLine(cor, Offset(c.x - t, c.y), Offset(c.x + t, c.y), espessura)
        drawLine(cor, Offset(c.x, c.y - t), Offset(c.x, c.y + t), espessura)
    }
}

/** Bloco-chave emoldurado: borda fina + marcas de canto (como o hodômetro do Em Dia). */
@Composable
fun Moldura(
    modifier: Modifier = Modifier,
    fundo: Color = Color.Transparent,
    linha: Color = Oficina.cores.line,
    marcas: Color = Oficina.cores.lineStrong,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .marcasDeCanto(marcas)
            .background(fundo)
            .border(1.dp, linha)
            .padding(20.dp),
        content = conteudo,
    )
}

/** Ponto de status, o único elemento redondo do sistema. */
@Composable
fun PontoStatus(cor: Color, modifier: Modifier = Modifier, tamanho: Dp = 8.dp) {
    Box(modifier.size(tamanho).background(cor, CircleShape))
}

/** Seletor segmentado em células: o item ativo vira bloco invertido. */
@Composable
fun <T> Segmentado(
    opcoes: List<T>,
    selecionada: T,
    rotulo: (T) -> String,
    onSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = Oficina.cores
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).border(1.dp, cores.line)) {
        opcoes.forEachIndexed { i, opcao ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(cores.line))
            val ativa = opcao == selecionada
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (ativa) cores.invertBg else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelecionar(opcao) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Hud(rotulo(opcao), cor = if (ativa) cores.invertFg else cores.muted)
            }
        }
    }
}

/** Etiqueta selecionável (atalho de endereço, valor sugerido…). */
@Composable
fun Etiqueta(texto: String, selecionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cores = Oficina.cores
    Box(
        modifier
            .heightIn(min = 36.dp)
            .background(if (selecionada) cores.invertBg else Color.Transparent)
            .border(1.dp, if (selecionada) cores.invertBg else cores.line)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Hud(texto, cor = if (selecionada) cores.invertFg else cores.ink, modifier = Modifier.widthIn(max = 200.dp))
    }
}

/** Caixa de marcação quadrada de 20dp. */
@Composable
fun CaixaMarcacao(marcado: Boolean, modifier: Modifier = Modifier) {
    val cores = Oficina.cores
    Box(
        modifier
            .size(20.dp)
            .background(if (marcado) cores.invertBg else Color.Transparent)
            .border(1.dp, if (marcado) cores.invertBg else cores.lineStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (marcado) Text("✓", color = cores.invertFg, style = Oficina.tipos.buttonLabel.copy(fontSize = 12.sp, letterSpacing = 0.sp))
    }
}

/** Linha com caixa de marcação quadrada (substitui o Switch). */
@Composable
fun LinhaMarcavel(
    texto: String,
    marcado: Boolean,
    onAlternar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    habilitado: Boolean = true,
) {
    val cores = Oficina.cores
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (habilitado) 1f else 0.4f)
            .toggleable(value = marcado, enabled = habilitado, role = Role.Checkbox, onValueChange = onAlternar)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaixaMarcacao(marcado)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, style = Oficina.tipos.body, color = cores.ink)
            if (subtitulo != null) Text(subtitulo, style = Oficina.tipos.bodySm, color = cores.muted)
        }
    }
}

/** Item de lista com seta, como no "Mais" do Em Dia. */
@Composable
fun ItemMenu(titulo: String, subtitulo: String, onClick: () -> Unit, etiqueta: String? = null) {
    val c = Oficina.cores
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = Oficina.tipos.body, color = c.ink)
            Text(subtitulo, style = Oficina.tipos.bodySm, color = c.muted)
        }
        Spacer(Modifier.width(12.dp))
        if (etiqueta != null) Hud(etiqueta, cor = c.faint) else Text("→", style = Oficina.tipos.body, color = c.ink)
    }
}

/* ───────────── Diálogo ───────────── */

@Composable
fun DialogoOficina(
    titulo: String,
    onDismiss: () -> Unit,
    eyebrow: String? = null,
    acoes: @Composable RowScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = Oficina.cores
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(cores.bgRaised)
                .border(1.dp, cores.line)
                .padding(24.dp)
        ) {
            if (eyebrow != null) {
                Hud(eyebrow)
                Spacer(Modifier.height(8.dp))
            }
            Text(titulo, style = Oficina.tipos.heading, color = cores.inkStrong)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f, fill = false), content = conteudo)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = acoes,
            )
        }
    }
}

/* ───────────── Campos ───────────── */

@Composable
fun coresCampo(): TextFieldColors {
    val c = Oficina.cores
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        disabledTextColor = c.muted,
        errorTextColor = c.ink,
        focusedContainerColor = c.bgSunken,
        unfocusedContainerColor = c.bgSunken,
        disabledContainerColor = c.bgSunken,
        errorContainerColor = c.bgSunken,
        cursorColor = c.accentInk,
        errorCursorColor = c.danger,
        focusedBorderColor = c.accentInk,
        unfocusedBorderColor = c.line,
        disabledBorderColor = c.line,
        errorBorderColor = c.danger,
        focusedLabelColor = c.accentInk,
        unfocusedLabelColor = c.muted,
        disabledLabelColor = c.faint,
        errorLabelColor = c.danger,
        focusedPlaceholderColor = c.faint,
        unfocusedPlaceholderColor = c.faint,
        focusedSupportingTextColor = c.muted,
        unfocusedSupportingTextColor = c.muted,
        disabledSupportingTextColor = c.faint,
        errorSupportingTextColor = c.danger,
        focusedLeadingIconColor = c.ink,
        unfocusedLeadingIconColor = c.muted,
        focusedTrailingIconColor = c.ink,
        unfocusedTrailingIconColor = c.muted,
        focusedPrefixColor = c.muted,
        unfocusedPrefixColor = c.muted,
        focusedSuffixColor = c.muted,
        unfocusedSuffixColor = c.muted,
    )
}

/* ───────────── Barras do app ───────────── */

/** Topo do app: marca à esquerda e situação do taxímetro à direita. */
@Composable
fun BarraSuperior(statusTexto: String, statusCor: Color) {
    val c = Oficina.cores
    Column(Modifier.fillMaxWidth().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TituloDisplay(
                "Corrida *Particular*",
                estilo = Oficina.tipos.heading.copy(fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em),
                cor = c.ink,
                modifier = Modifier.weight(1f),
            )
            PontoStatus(statusCor, tamanho = 6.dp)
            Spacer(Modifier.width(6.dp))
            Hud(statusTexto)
        }
        Linha()
    }
}

/** Menu inferior: a aba ativa vira bloco invertido. [marcas] põe um ponto colorido no ícone de uma aba. */
@Composable
fun BarraInferior(abaAtual: Aba, onAba: (Aba) -> Unit, marcas: Map<Aba, Color> = emptyMap()) {
    val c = Oficina.cores
    Column(Modifier.fillMaxWidth().background(c.bg)) {
        Linha()
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            Aba.entries.forEach { aba ->
                val ativa = aba == abaAtual
                val frente = if (ativa) c.invertFg else c.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (ativa) c.invertBg else Color.Transparent)
                        .clickable(role = Role.Tab) { onAba(aba) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Icon(aba.icone, contentDescription = null, modifier = Modifier.size(20.dp), tint = frente)
                        marcas[aba]?.let { cor ->
                            PontoStatus(cor, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp), tamanho = 7.dp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Fonte do rótulo cresce no máximo 10% com a "fonte grande" do Android, para caber na aba.
                    val escala = LocalDensity.current.fontScale
                    val tamanho = (10f * minOf(escala, 1.1f) / escala).sp
                    Hud(aba.titulo, cor = frente, estilo = Oficina.tipos.hud.copy(fontSize = tamanho, letterSpacing = 0.04.em))
                }
            }
        }
    }
}

/** Faixa do taxímetro ligado, mostrada acima do menu quando você está em outra aba. */
@Composable
fun FaixaTaximetro(texto: String, detalhe: String, valor: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Visor.fundo)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PontoStatus(Visor.amarelo, tamanho = 6.dp)
        Spacer(Modifier.width(8.dp))
        Hud(texto, cor = Visor.tinta)
        Spacer(Modifier.width(8.dp))
        Hud(detalhe, cor = Visor.apagado, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(valor, color = Visor.amarelo, style = Oficina.tipos.heading.copy(fontSize = 18.sp, lineHeight = 22.sp))
    }
}
