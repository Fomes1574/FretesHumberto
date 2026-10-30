package io.github.fomes1574.freteshumberto.core

import java.math.BigInteger
import java.time.LocalDate

private fun big(v: Long) = BigInteger.valueOf(v)

data class ResumoDia(val fretes: Long = 0, val combustivel: Long = 0, val outras: Long = 0) {
    val lucro: Long get() = fretes - combustivel - outras
}

/** Resultado de um período. Porta de `Core.summary`. */
data class Resumo(
    val fretes: Long,
    val qtdFretes: Int,
    val combustivel: Long,
    val qtdAbastecimentos: Int,
    val litrosCentesimos: Long,
    val outras: Long,
    val kmCentesimos: Long,
    val porDia: Map<LocalDate, ResumoDia>,
    val diasTrabalhados: Set<LocalDate>,
    val porCategoria: Map<String, Long>,
    val diasMetaAtingida: Int,
) {
    /** R02: lucro = fretes − abastecimentos − outras despesas. Nunca cortado em zero. */
    val lucro: Long get() = fretes - combustivel - outras

    /** Preço médio = soma paga ÷ soma de litros. Null sem litros (não divide por zero). */
    val precoMedioLitro: Long? get() = if (litrosCentesimos > 0) Dinheiro.roundDiv(big(combustivel) * big(100), big(litrosCentesimos)) else null

    /** % do valor dos fretes que foi para o tanque. Null sem fretes. */
    val percentualTanque: Long? get() = if (fretes > 0) Dinheiro.roundDiv(big(combustivel) * big(100), big(fretes)) else null

    /** Média por dia trabalhado (dia com ao menos um frete). O total inclui gastos de dias sem frete. */
    val lucroMedioPorDiaTrabalhado: Long? get() = if (diasTrabalhados.isNotEmpty()) Dinheiro.roundDiv(lucro, diasTrabalhados.size.toLong()) else null
}

object Relatorio {
    /**
     * Soma os lançamentos do período (ou só das datas [selecionadas], para "comparar dois dias",
     * sem os dias intermediários).
     */
    fun resumir(
        lancamentos: List<Lancamento>,
        periodo: Intervalo,
        metas: Metas,
        selecionadas: Set<LocalDate>? = null,
    ): Resumo {
        var fretes = 0L; var nF = 0; var comb = 0L; var nA = 0; var litros = 0L; var outras = 0L; var km = 0L
        val dias = HashMap<LocalDate, ResumoDia>()
        val trab = HashSet<LocalDate>()
        val cats = LinkedHashMap<String, Long>()
        for (l in lancamentos) {
            val dentro = selecionadas?.contains(l.data) ?: (l.data in periodo)
            if (!dentro) continue
            val d = dias[l.data] ?: ResumoDia()
            when (l.tipo) {
                Tipo.FRETE -> { fretes += l.valor; nF++; trab += l.data; km += l.kmCentesimos ?: 0; dias[l.data] = d.copy(fretes = d.fretes + l.valor) }
                Tipo.ABASTECIMENTO -> { comb += l.valor; nA++; litros += l.litrosCentesimos ?: 0; dias[l.data] = d.copy(combustivel = d.combustivel + l.valor) }
                Tipo.DESPESA -> {
                    outras += l.valor
                    val c = l.categoria ?: "Outros"
                    cats[c] = (cats[c] ?: 0) + l.valor
                    dias[l.data] = d.copy(outras = d.outras + l.valor)
                }
            }
        }
        val metaOk = trab.count { (dias[it]?.lucro ?: 0) >= metas.doDia(it) }
        return Resumo(fretes, nF, comb, nA, litros, outras, km, dias, trab, cats, metaOk)
    }
}

/** Resultado da calculadora isolada (R05). Nunca grava no diário. */
data class Simulacao(
    val valor: Long,
    val combustivel: Long,
    val sobra: Long,
    val litrosCentesimos: Long,
    val porKm: Long,
    /** Null quando o tempo não foi informado. */
    val porHora: Long?,
)

object Calculadora {
    /**
     * combustível = km ÷ km/l × preço; sobra = valor − combustível − outros custos.
     * Retorna null se valor, km, consumo ou preço forem zero/ inválidos (sem divisão por zero).
     */
    fun simular(valor: Long, kmCentesimos: Long, kmPorLitroCentesimos: Long, precoLitro: Long, minutos: Int = 0, outrosCustos: Long = 0): Simulacao? {
        if (listOf(valor, kmCentesimos, kmPorLitroCentesimos, precoLitro).any { it <= 0 || it > Dinheiro.MAX }) return null
        if (outrosCustos < 0 || outrosCustos > Dinheiro.MAX || minutos < 0) return null
        val comb = Dinheiro.roundDiv(big(kmCentesimos) * big(precoLitro), big(kmPorLitroCentesimos))
        val sobra = valor - comb - outrosCustos
        return Simulacao(
            valor = valor,
            combustivel = comb,
            sobra = sobra,
            litrosCentesimos = Dinheiro.roundDiv(big(kmCentesimos) * big(100), big(kmPorLitroCentesimos)),
            porKm = Dinheiro.roundDiv(big(sobra) * big(100), big(kmCentesimos)),
            porHora = if (minutos > 0) Dinheiro.roundDiv(big(sobra) * big(60), big(minutos.toLong())) else null,
        )
    }
}

data class Abastecimento(val litrosCentesimos: Long, val precoLitro: Long)

object Combustivel {
    /**
     * Abasteci: valor pago + preço do litro OU litros. O outro é calculado (R04).
     * R$ 200 e R$ 3,70 → 54,05 L. R$ 200 e 54,05 L → R$ 3,70.
     */
    fun calcular(valorPago: Long, auxiliar: Long, modo: ModoAbastecimento): Abastecimento? {
        if (valorPago !in 1..Dinheiro.MAX || auxiliar !in 1..Dinheiro.MAX) return null
        val r = when (modo) {
            ModoAbastecimento.PRECO -> Abastecimento(Dinheiro.roundDiv(big(valorPago) * big(100), big(auxiliar)), auxiliar)
            ModoAbastecimento.LITROS -> Abastecimento(auxiliar, Dinheiro.roundDiv(big(valorPago) * big(100), big(auxiliar)))
        }
        return if (r.litrosCentesimos > 0 && r.precoLitro > 0) r else null
    }

    data class ReferenciaManual(val data: LocalDate, val preco: Long, val carimbo: String = "")

    /**
     * Preço de referência = abastecimento mais recente pela DATA FINANCEIRA (desempate pelo
     * carimbo técnico), ou a referência manual dos Ajustes se for mais nova. Um lançamento
     * antigo nunca toma o lugar do mais novo. Porta de `Core.price`.
     */
    fun precoReferencia(lancamentos: List<Lancamento>, manual: ReferenciaManual?, padrao: Long): Long {
        data class C(val data: LocalDate, val preco: Long, val carimbo: String)
        val cands = lancamentos.filter { it.tipo == Tipo.ABASTECIMENTO && it.precoLitro != null }
            .map { C(it.data, it.precoLitro!!, it.editadoEm ?: it.criadoEm.ifEmpty { it.id }) }
            .toMutableList()
        if (manual != null) cands += C(manual.data, manual.preco, manual.carimbo)
        return cands.sortedWith(compareBy<C>({ it.data }, { it.carimbo })).lastOrNull()?.preco ?: padrao
    }
}
