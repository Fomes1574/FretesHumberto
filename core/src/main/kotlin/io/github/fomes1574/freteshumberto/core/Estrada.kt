package io.github.fomes1574.freteshumberto.core

/**
 * Regras da cena "estrada da meta" (Fiorino), independentes de Compose para poderem
 * ser testadas. A tela desenha em um Canvas de 360×150 unidades e escala.
 *
 * - A van é posicionada pela FRENTE (x).
 * - Abaixo da meta: x vai de [INICIO] a [ENCOSTA_NA_LINHA], proporcional ao lucro.
 * - Meta atingida: x = [ESTACIONADA], depois da linha de chegada.
 * - Acima da meta: a posição não muda; o número já aparece com o valor final.
 */
object Estrada {
    const val LARGURA = 360f
    const val ALTURA = 150f
    const val INICIO = 100f
    const val ENCOSTA_NA_LINHA = 249f
    const val LINHA = 250f
    const val ESTACIONADA = 357f
    const val COMPRIMENTO_VAN = 96f

    /** Posição da frente da van para um lucro e uma meta. Meta zero ou negativa conta como atingida. */
    fun alvo(lucro: Long, meta: Long): Float {
        if (meta <= 0 || lucro >= meta) return ESTACIONADA
        if (lucro <= 0) return INICIO
        return INICIO + (ENCOSTA_NA_LINHA - INICIO) * (lucro.toFloat() / meta.toFloat())
    }

    /** Animar só se a posição muda de verdade (acima da meta: só o número muda). */
    fun deveMover(de: Float, para: Float): Boolean = kotlin.math.abs(para - de) > 0.3f

    /** Confete uma vez, quando a van cruza a linha indo para frente. Dando ré, nunca. */
    fun soltaConfete(de: Float, para: Float): Boolean = de < LINHA + 10f && para >= LINHA + 10f

    /** Duração em ms: 1,1 s + 7 ms por unidade percorrida. */
    fun duracaoMs(de: Float, para: Float): Int = (1100 + 7 * kotlin.math.abs(para - de)).toInt()

    /** Curva ease-in-out cúbica (arranca, acelera, freia). */
    fun curva(t: Float): Float = if (t < 0.5f) 4f * t * t * t else 1f - Math.pow((-2.0 * t + 2.0), 3.0).toFloat() / 2f

    /** Rodas: acima de ~6° por quadro os raios viram borrão (evita roda "girando para trás"). */
    fun opacidadeBorrao(grausPorQuadro: Float): Float = ((grausPorQuadro - 6f) / 14f).coerceIn(0f, 1f)
}
