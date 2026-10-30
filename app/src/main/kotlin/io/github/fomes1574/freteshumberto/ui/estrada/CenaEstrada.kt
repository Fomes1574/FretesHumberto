package io.github.fomes1574.freteshumberto.ui.estrada

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.core.Estrada
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.ui.theme.Atkinson
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/** Estrada anda mais rápido que a van; morros a 18% e árvores a 55% da estrada (ARQUITETURA §5). */
private const val VELOCIDADE_ESTRADA = 2.2f
private const val RAIO_RODA = 8f
private const val CONFETE_MS = 5000

/**
 * Estrada da meta com a Fiorino. Valor final aparece na hora na tela; só a van anima.
 * Sem animação quando nada mudou, e nenhuma com "Remover animações" ligado no Android.
 *
 * [posicoes] guarda onde a van parou em cada cena ([chave]), para voltar à aba sem animar de novo.
 */
@Composable
fun CenaEstrada(lucro: Long, meta: Long, chave: String, posicoes: MutableMap<String, Float>, rotuloMeta: String) {
    val contexto = LocalContext.current
    val semMovimento = remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val alvo = Estrada.alvo(lucro, meta)
    val x = remember(chave) { Animatable(posicoes[chave] ?: Estrada.INICIO) }
    val confete = remember(chave) { Animatable(1f) }
    val medidor = rememberTextMeasurer()

    LaunchedEffect(chave, alvo) {
        val de = x.value
        if (semMovimento || !Estrada.deveMover(de, alvo)) {
            x.snapTo(alvo)
        } else {
            var soltou = false
            x.animateTo(alvo, tween(Estrada.duracaoMs(de, alvo), easing = Easing { Estrada.curva(it) })) {
                if (!soltou && Estrada.soltaConfete(de, value) && value >= Estrada.LINHA + 10f) {
                    soltou = true
                    launch {
                        confete.snapTo(0f)
                        confete.animateTo(1f, tween(CONFETE_MS, easing = LinearEasing))
                    }
                }
            }
        }
        posicoes[chave] = alvo
    }

    val descricao = if (meta <= 0 || lucro >= meta) "Estrada da meta: a van passou da linha de chegada. Meta atingida."
    else "Estrada da meta: a van percorreu ${((lucro.coerceAtLeast(0) * 100) / meta)}% do caminho até a meta."

    Column {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(Estrada.LARGURA / Estrada.ALTURA).clip(RoundedCornerShape(14.dp))
                .semantics { contentDescription = descricao },
        ) {
            val s = size.width / Estrada.LARGURA
            val pos = x.value
            val estrada = pos * VELOCIDADE_ESTRADA
            // Graus por quadro (60 qps) para decidir o borrão das rodas.
            val grausQuadro = abs(x.velocity) * VELOCIDADE_ESTRADA / RAIO_RODA * (180f / PI.toFloat()) / 60f
            withTransform({ scale(s, s, Offset.Zero) }) {
                fundo(estrada)
                pista(estrada, pos)
                chegada()
                van(pos - Estrada.COMPRIMENTO_VAN, estrada / RAIO_RODA * (180f / PI.toFloat()), Estrada.opacidadeBorrao(grausQuadro))
                if (confete.value < 1f) confetes(confete.value)
            }
            // Placa "META" fora da escala, para o texto ficar nítido.
            val estilo = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, color = Color.White, fontSize = (8.5f * s).toSp())
            val medida = medidor.measure("META", estilo)
            drawRoundRect(Cores.Verde, Offset(234f * s, 40f * s), Size(40f * s, 15f * s), CornerRadius(4f * s))
            drawText(medida, topLeft = Offset(254f * s - medida.size.width / 2f, 47.5f * s - medida.size.height / 2f))
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("R$ 0", style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado, modifier = Modifier.weight(1f))
            Text(rotuloMeta, style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado)
        }
    }
}

/** Repete um desenho de largura [periodo] deslocado por [deslocamento], cobrindo a cena. */
private fun DrawScope.repetir(periodo: Float, deslocamento: Float, desenho: DrawScope.() -> Unit) {
    val inicio = -(deslocamento - floor(deslocamento / periodo) * periodo)
    var x = inicio
    while (x < Estrada.LARGURA) {
        translate(left = x) { desenho() }
        x += periodo
    }
}

private fun DrawScope.fundo(estrada: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF7F1DE), Color(0xFFEAF0E0)), 0f, 100f), size = Size(Estrada.LARGURA, 106f))
    drawCircle(Color(0x33F4D58D), 24f, Offset(318f, 30f))
    drawCircle(Color(0xFFF4D58D), 14f, Offset(318f, 30f))
    // Morros e prédios ao fundo: 18% da estrada.
    repetir(360f, estrada * 0.18f) {
        drawOval(Color(0xFFD7E5D2), Offset(-40f, 70f), Size(200f, 70f))
        drawOval(Color(0xFFCFE0CB), Offset(150f, 76f), Size(240f, 64f))
        drawRect(Color(0xFFC9D6D0), Offset(70f, 66f), Size(14f, 36f))
        drawRect(Color(0xFFBFCEC7), Offset(86f, 58f), Size(12f, 44f))
        drawRect(Color(0xFFC9D6D0), Offset(100f, 72f), Size(10f, 30f))
    }
    drawRect(Color(0xFFDDE8C9), Offset(0f, 98f), Size(Estrada.LARGURA, 8f))
    // Árvores e postes: 55% da estrada.
    repetir(180f, estrada * 0.55f) {
        drawRect(Color(0xFF7A5C3E), Offset(28f, 84f), Size(3f, 16f))
        drawCircle(Color(0xFF6E9E73), 9f, Offset(29.5f, 80f))
        drawCircle(Color(0xFF5F8F66), 6f, Offset(35f, 84f))
        val pinheiro = Path().apply { moveTo(110f, 66f); lineTo(100f, 92f); lineTo(120f, 92f); close() }
        drawPath(pinheiro, Color(0xFF55875C))
        drawRect(Color(0xFF7A5C3E), Offset(108.5f, 92f), Size(3f, 8f))
        drawLine(Color(0xFF9AA3A0), Offset(160f, 100f), Offset(160f, 70f), 2f)
        drawLine(Color(0xFF9AA3A0), Offset(160f, 70f), Offset(168f, 70f), 2f)
        drawRect(Color(0xFFF2E4B0), Offset(166f, 70f), Size(4f, 2.5f))
    }
}

private fun DrawScope.pista(estrada: Float, frente: Float) {
    drawRect(Color(0xFF343A37), Offset(0f, 106f), Size(Estrada.LARGURA, 36f))
    repetir(36f, estrada) {
        drawRect(Color(0xFFF2C14E), Offset(0f, 122.5f), Size(18f, 3f))
    }
    drawRect(Color(0xFFE8E2D3), Offset(0f, 142f), Size(Estrada.LARGURA, 8f))
    // Faixa verde: caminho já percorrido.
    drawRect(Cores.Verde, Offset(0f, 142f), Size(frente.coerceIn(0f, Estrada.LARGURA), 5f))
}

private fun DrawScope.chegada() {
    val q = 6f
    for (linha in 0 until 6) for (col in 0 until 2) {
        val preto = (linha + col) % 2 == 0
        drawRect(if (preto) Color(0xFF191D1B) else Color.White, Offset(Estrada.LINHA + col * q, 106f + linha * q), Size(q, q))
    }
    drawLine(Color(0xFF565D59), Offset(Estrada.LINHA + 12f, 106f), Offset(Estrada.LINHA + 12f, 58f), 1.6f)
    for (linha in 0 until 2) for (col in 0 until 3) {
        val preto = (linha + col) % 2 == 0
        drawRect(if (preto) Color(0xFF191D1B) else Color.White, Offset(Estrada.LINHA + 12.8f + col * 4f, 58f + linha * 4f), Size(4f, 4f))
    }
}

/** Fiorino branca de perfil, desenhada a partir da traseira em [x]. */
private fun DrawScope.van(x: Float, anguloRoda: Float, borrao: Float) {
    translate(left = x) {
        val contorno = Color(0xFF8E9894)
        val corpo = Path().apply {
            moveTo(2f, 127f)
            lineTo(2f, 96f)
            quadraticTo(2f, 91f, 7f, 91f)
            lineTo(70f, 91f)
            quadraticTo(73f, 91f, 75f, 93f)
            lineTo(86f, 106f)
            lineTo(93f, 109f)
            quadraticTo(96f, 110f, 96f, 114f)
            lineTo(96f, 125f)
            quadraticTo(96f, 128f, 93f, 128f)
            lineTo(4f, 128f)
            close()
        }
        drawPath(corpo, Color.White)
        drawPath(corpo, contorno, style = Stroke(1f))
        val vidro = Path().apply { moveTo(70f, 95f); lineTo(74f, 95f); lineTo(84f, 107f); lineTo(70f, 107f); close() }
        drawPath(vidro, Color(0xFF9CC3D5))
        drawLine(contorno, Offset(66f, 94f), Offset(66f, 125f), 0.8f)
        drawLine(contorno, Offset(38f, 94f), Offset(38f, 125f), 0.8f)
        drawLine(contorno, Offset(60f, 112f), Offset(63f, 112f), 1.2f, StrokeCap.Round)
        drawRect(Color(0xFFC9CED0), Offset(68f, 104f), Size(3f, 4f))
        drawRect(Color(0xFFF2C14E), Offset(91f, 112f), Size(4f, 3f))
        drawRect(Color(0xFFE59A2F), Offset(85f, 117f), Size(2.5f, 2f))
        drawRect(Color(0xFFC0392B), Offset(2f, 99f), Size(2.5f, 9f))
        drawRoundRect(Color(0xFF6B7470), Offset(0f, 123f), Size(9f, 4.5f), CornerRadius(1.5f))
        drawRoundRect(Color(0xFF6B7470), Offset(88f, 123f), Size(8.5f, 4.5f), CornerRadius(1.5f))
        drawRect(Color(0x22000000), Offset(10f, 137f), Size(80f, 2f))
        roda(Offset(20f, 130f), anguloRoda, borrao)
        roda(Offset(77f, 130f), anguloRoda, borrao)
    }
}

private fun DrawScope.roda(c: Offset, angulo: Float, borrao: Float) {
    drawCircle(Color(0xFF222624), RAIO_RODA, c)
    drawCircle(Color(0xFFC9CED0), 5f, c)
    if (borrao < 1f) {
        rotate(angulo, pivot = c) {
            for (i in 0 until 5) {
                rotate(i * 72f, pivot = c) {
                    drawLine(Color(0xFF6B7470).copy(alpha = 1f - borrao), c, Offset(c.x, c.y - 4.6f), 1.3f, StrokeCap.Round)
                }
            }
        }
    }
    // Acima de ~6° por quadro os raios viram borrão (evita a roda "girando para trás").
    if (borrao > 0f) drawCircle(Color(0xFF9AA3A0).copy(alpha = borrao * 0.8f), 4.6f, c)
    drawCircle(Color(0xFF565D59), 1.4f, c)
}

private val CORES_CONFETE = listOf(Color(0xFF17603F), Color(0xFFF2C14E), Color(0xFFC0392B), Color(0xFF1F4E79), Color(0xFFE59A2F), Color.White)

/** Confete preso à linha de chegada (fixa): sai da bandeira, cai e fica no asfalto. */
private fun DrawScope.confetes(p: Float) {
    val t = p * CONFETE_MS / 1000f
    val alfa = if (p > 0.8f) (1f - p) / 0.2f else 1f
    val r = Random(7)
    repeat(46) { i ->
        val vx = r.nextFloat() * 70f - 35f
        val vy = -r.nextFloat() * 40f - 10f
        val chao = 108f + r.nextFloat() * 32f
        val px = Estrada.LINHA + 16f + vx * minOf(t, 1.2f)
        val py = minOf(62f + vy * t + 60f * t * t, chao)
        rotate(i * 37f + t * 200f * (if (py >= chao) 0f else 1f), pivot = Offset(px, py)) {
            drawRect(CORES_CONFETE[i % CORES_CONFETE.size].copy(alpha = alfa), Offset(px - 1.5f, py - 0.8f), Size(3f, 1.6f))
        }
    }
}

/** Rótulo "Meta R$ 250,00" do lado direito da estrada. */
fun rotuloMeta(meta: Long, prefixo: String = "Meta") = "$prefixo ${Formato.reais(meta)}"
