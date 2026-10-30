package io.github.fomes1574.freteshumberto.core

import java.math.BigInteger

/**
 * Dinheiro sempre em centavos (Long). Porta fiel de `Core.parseScaled` e `Core.roundDiv`
 * do protótipo (src/core.js). Nunca usar Double para dinheiro.
 */
object Dinheiro {
    /** Maior valor aceito: R$ 999.999.999,99 (mesmo limite do protótipo). */
    const val MAX: Long = 99_999_999_999L

    private val milhares = Regex("""^\d{1,3}(\.\d{3})+$""")
    private val soDigitos = Regex("""^\d+$""")
    private val digitosOuVazio = Regex("""^\d*$""")
    private val permitido = Regex("""^[\d.,]+$""")

    /**
     * Converte texto digitado ou colado em centésimos (centavos, litros×100, km×100).
     *
     * - `1.234,56`, `R$ 1.234,56` e `1234,56` → 123456
     * - `85.50` → 8550 (ponto como decimal quando não há milhares)
     * - `54.0599` e `54,0599` → 5405 (trunca na 2ª casa)
     * - `1.234` é ambíguo → null (não vira R$ 1,23 em silêncio)
     */
    fun parse(entrada: String?): Long? {
        if (entrada == null) return null
        val s = entrada.trim().replaceFirst(Regex("""^R\$\s*"""), "").replace(Regex("""[\s ]"""), "")
        if (s.isEmpty() || !permitido.matches(s)) return null
        var inteiro: String
        var fracao = ""
        if (s.contains(',')) {
            if (s.count { it == ',' } != 1) return null
            val partes = s.split(',')
            inteiro = partes[0].ifEmpty { "0" }
            fracao = partes[1]
            if (inteiro.contains('.')) {
                if (!milhares.matches(inteiro)) return null
                inteiro = inteiro.replace(".", "")
            }
        } else if (s.contains('.')) {
            val partes = s.split('.')
            if (partes.size > 2) {
                if (!milhares.matches(s)) return null
                inteiro = s.replace(".", "")
            } else {
                inteiro = partes[0].ifEmpty { "0" }
                fracao = partes[1]
                if (fracao.length == 3) return null
            }
        } else {
            inteiro = s
        }
        if (!soDigitos.matches(inteiro) || !digitosOuVazio.matches(fracao)) return null
        val n = BigInteger(inteiro) * BigInteger.valueOf(100) + BigInteger(fracao.take(2).padEnd(2, '0'))
        return if (n <= BigInteger.valueOf(MAX)) n.toLong() else null
    }

    /** Divisão arredondando metade para longe de zero, igual ao protótipo. */
    fun roundDiv(a: Long, b: Long): Long = roundDiv(BigInteger.valueOf(a), BigInteger.valueOf(b))

    fun roundDiv(a: BigInteger, b: BigInteger): Long {
        require(b.signum() > 0) { "Divisor inválido" }
        val sinal = if (a.signum() < 0) BigInteger.ONE.negate() else BigInteger.ONE
        val v = a * sinal
        return (sinal * ((v + b / BigInteger.TWO) / b)).toLong()
    }

    fun valido(c: Long): Boolean = c in 0..MAX

    /**
     * Filtro aplicado enquanto a pessoa digita num campo decimal:
     * ponto vira vírgula, só uma vírgula, no máximo 2 casas, letras somem.
     * `54.0599` → `54,05`; `,5` → `0,5`.
     */
    fun filtrarDigitacao(texto: String): String {
        var v = texto.replace('.', ',').filter { it.isDigit() || it == ',' }
        val i = v.indexOf(',')
        if (i >= 0) v = v.substring(0, i + 1) + v.substring(i + 1).replace(",", "").take(2)
        if (v.startsWith(',')) v = "0$v"
        return v
    }
}

/** Formatação em português do Brasil, sem depender de Locale do aparelho. */
object Formato {
    /** 123456 → "R$ 1.234,56"; -6000 → "-R$ 60,00". */
    fun reais(centavos: Long): String {
        val neg = centavos < 0
        val abs = if (neg) -centavos else centavos
        val texto = "R$ " + milhar(abs / 100) + "," + (abs % 100).toString().padStart(2, '0')
        return if (neg) "-$texto" else texto
    }

    /** Sem centavos, para rótulos de gráfico: 550000 → "R$ 5.500". Arredonda. */
    fun reaisInteiros(centavos: Long): String {
        val r = Dinheiro.roundDiv(centavos, 100)
        return (if (r < 0) "-R$ " else "R$ ") + milhar(kotlin.math.abs(r))
    }

    /** Centésimos com 2 casas: 5405 → "54,05". */
    fun centesimos(valor: Long): String {
        val neg = valor < 0
        val abs = if (neg) -valor else valor
        return (if (neg) "-" else "") + milhar(abs / 100) + "," + (abs % 100).toString().padStart(2, '0')
    }

    /** Valor para pré-preencher campo editável, sem separador de milhar: 25000 → "250,00". */
    fun paraCampo(centesimos: Long): String = (centesimos / 100).toString() + "," + (centesimos % 100).toString().padStart(2, '0')

    private fun milhar(n: Long): String {
        val s = n.toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return sb.toString()
    }
}
