package io.github.fomes1574.freteshumberto.ui.relatorios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Dinheiro
import io.github.fomes1574.freteshumberto.ui.theme.Atkinson
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private val VERDE_CLARO_BARRA = Color(0xFF7FB394)

private fun DrawScope.texto(m: TextMeasurer, t: String, x: Float, y: Float, cor: Color, tamanho: Int = 12) {
    val r = m.measure(t, TextStyle(fontFamily = Atkinson, fontSize = tamanho.sp, color = cor))
    drawText(r, topLeft = Offset(x - r.size.width / 2f, y - r.size.height))
}

/**
 * Lucro por dia (ARQUITETURA §3.5): verde-escuro com meta atingida, verde-claro abaixo,
 * vermelho no negativo; linha tracejada da meta quando ela é a mesma em todos os dias.
 * Muitos dias: rola para o lado. Os mesmos valores aparecem em texto na lista de dias.
 */
@Composable
fun GraficoLucro(
    dias: List<LocalDate>,
    lucros: Map<LocalDate, Long>,
    metas: List<Long>,
    hoje: LocalDate,
    selecionado: LocalDate?,
    aoTocar: (LocalDate) -> Unit,
) {
    val medidor = rememberTextMeasurer()
    val n = dias.size.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val largura: Dp = max(maxWidth.value, n * 42f).dp
        Canvas(
            Modifier.horizontalScroll(rememberScrollState()).width(largura).height(210.dp)
                .semantics { contentDescription = "Gráfico de lucro por dia. Os mesmos valores estão em Ver valores por dia." }
                .pointerInput(dias) {
                    detectTapGestures { p ->
                        val i = (p.x / (size.width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                        dias.getOrNull(i)?.let(aoTocar)
                    }
                },
        ) {
            val topo = 28.dp.toPx(); val base = 34.dp.toPx()
            val vals = dias.map { lucros[it] ?: 0L }
            val mx = max(max((metas.maxOrNull() ?: 0L) * 1.15, (vals.maxOrNull() ?: 0L).toDouble()), 1.0)
            val mn = min(0.0, (vals.minOrNull() ?: 0L).toDouble())
            val esc = (size.height - topo - base) / (mx - mn)
            val y0 = (topo + mx * esc).toFloat()
            val gw = size.width / n
            val bw = min(34.dp.toPx(), gw * 0.68f)
            if (metas.isNotEmpty() && metas.all { it == metas[0] }) {
                val ym = (y0 - metas[0] * esc).toFloat()
                drawLine(Cores.Verde, Offset(0f, ym), Offset(size.width, ym), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            }
            drawLine(Color(0xFFB8B1A2), Offset(0f, y0), Offset(size.width, y0), 1.dp.toPx())
            val passo = if (n <= 7) 1 else ceil(n / (size.width / 46.dp.toPx())).toInt().coerceAtLeast(1)
            dias.forEachIndexed { i, d ->
                val v = vals[i]
                val x = i * gw + (gw - bw) / 2f
                val h = (abs(v) * esc).toFloat()
                val y = if (v >= 0) y0 - h else y0
                if (!d.isAfter(hoje) && lucros.containsKey(d)) {
                    val cor = when {
                        v < 0 -> Cores.Negativo
                        v >= metas.getOrElse(i) { 0L } -> Cores.Verde
                        else -> VERDE_CLARO_BARRA
                    }
                    drawRoundRect(cor, Offset(x, y), Size(bw, max(h, 2f)), CornerRadius(3.dp.toPx()))
                    if (d == selecionado) drawRoundRect(Cores.Tinta, Offset(x, y), Size(bw, max(h, 2f)), CornerRadius(3.dp.toPx()), style = Stroke(2.5.dp.toPx()))
                    if (n <= 7) texto(medidor, Dinheiro.roundDiv(v, 100).toString(), x + bw / 2f, (if (v >= 0) y else y + h + 16.dp.toPx()) - 3.dp.toPx(), Cores.Tinta)
                }
                if (i % passo == 0) {
                    val rotulo = if (n <= 7) DataTexto.diaSemana(d) else DataTexto.diaMes(d)
                    texto(medidor, rotulo, i * gw + gw / 2f, size.height - 4.dp.toPx(), if (d == selecionado) Cores.Verde else Cores.Apagado, 13)
                }
            }
        }
    }
}

/** Combustível nos últimos 6 meses (só no relatório do mês). Valores também em texto. */
@Composable
fun GraficoCombustivel(meses: List<YearMonth>, totais: List<Long>) {
    val medidor = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = "Gasto mensal com combustível. Valores na lista abaixo." }) {
        val mx = max(totais.maxOrNull() ?: 0L, 1L).toFloat()
        val base = 28.dp.toPx()
        val alto = size.height - base - 8.dp.toPx()
        val gw = size.width / meses.size
        val bw = min(34.dp.toPx(), gw * 0.55f)
        meses.forEachIndexed { i, m ->
            val h = totais[i] / mx * alto
            val x = i * gw + (gw - bw) / 2f
            drawRoundRect(Color(0xFFA96A2D), Offset(x, size.height - base - h), Size(bw, max(h, 1f)), CornerRadius(3.dp.toPx()))
            texto(medidor, DataTexto.mesCurto(m), i * gw + gw / 2f, size.height - 4.dp.toPx(), Cores.Apagado, 13)
        }
    }
}
