package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate

/**
 * Ajustes que entram nas contas e na cópia de segurança. Tamanho da letra e data da
 * última cópia são preferências do aparelho e ficam fora daqui.
 */
data class Ajustes(
    /** Consumo da calculadora, em centésimos de km/l: 850 = 8,5 km/l. */
    val kmPorLitroCentesimos: Long = 850,
    /** Preço do litro definido à mão nos Ajustes. Abastecimento mais novo tem preferência (R04). */
    val precoManual: Combustivel.ReferenciaManual = Combustivel.ReferenciaManual(LocalDate.of(1900, 1, 1), 379, ""),
) {
    init {
        require(kmPorLitroCentesimos in 1..100_000) { "Consumo inválido" }
        require(precoManual.preco in 1..Dinheiro.MAX) { "Preço inválido" }
    }
}

/** Tudo o que o app guarda no banco. Cada operação recebe um Estado e devolve outro. */
data class Estado(
    val diario: Diario = Diario(),
    val contas: List<ContaMensal> = emptyList(),
    val metas: Metas = Metas(),
    val ajustes: Ajustes = Ajustes(),
) {
    val lancamentos: List<Lancamento> get() = diario.lancamentos

    /** Preço de referência atual (calculadora e formulário Abasteci). */
    fun precoReferencia(): Long = Combustivel.precoReferencia(lancamentos, ajustes.precoManual, ajustes.precoManual.preco)

    fun gerarContas(hoje: LocalDate, novoId: () -> String, agora: String = ""): Estado =
        copy(diario = diario.aplicarContasDoMes(contas, hoje, novoId, agora))
}

/** Revisão de um dia e a revisão que estava valendo quando ele foi conferido. */
data class RevisaoDia(val revisao: Int, val conferida: Int?)

/**
 * O que mudou entre dois estados. O repositório grava só isto, numa única transação,
 * em vez de reescrever o banco inteiro a cada lançamento.
 */
data class Mudancas(
    val lancamentosGravar: List<Lancamento>,
    val lancamentosApagar: List<String>,
    val ocorrenciasGravar: List<Ocorrencia>,
    val ocorrenciasApagar: List<String>,
    val diasGravar: Map<LocalDate, RevisaoDia>,
    val diasApagar: List<LocalDate>,
    /** Listas pequenas: quando mudam, são regravadas inteiras. */
    val contas: List<ContaMensal>?,
    val metas: Metas?,
    val ajustes: Ajustes?,
) {
    val vazia: Boolean
        get() = lancamentosGravar.isEmpty() && lancamentosApagar.isEmpty() && ocorrenciasGravar.isEmpty() &&
            ocorrenciasApagar.isEmpty() && diasGravar.isEmpty() && diasApagar.isEmpty() &&
            contas == null && metas == null && ajustes == null

    companion object {
        fun entre(antes: Estado, depois: Estado): Mudancas {
            val lAntes = antes.lancamentos.associateBy { it.id }
            val lDepois = depois.lancamentos.associateBy { it.id }
            val oAntes = antes.diario.ocorrencias
            val oDepois = depois.diario.ocorrencias
            val dAntes = dias(antes.diario)
            val dDepois = dias(depois.diario)
            return Mudancas(
                lancamentosGravar = depois.lancamentos.filter { lAntes[it.id] != it },
                lancamentosApagar = lAntes.keys.filter { it !in lDepois },
                ocorrenciasGravar = oDepois.values.filter { oAntes[it.chave] != it },
                ocorrenciasApagar = oAntes.keys.filter { it !in oDepois },
                diasGravar = dDepois.filter { (d, r) -> dAntes[d] != r },
                diasApagar = dAntes.keys.filter { it !in dDepois },
                contas = depois.contas.takeIf { it != antes.contas },
                metas = depois.metas.takeIf { it != antes.metas },
                ajustes = depois.ajustes.takeIf { it != antes.ajustes },
            )
        }

        fun dias(d: Diario): Map<LocalDate, RevisaoDia> =
            (d.revisoes.keys + d.conferidos.keys).associateWith { RevisaoDia(d.revisoes[it] ?: 0, d.conferidos[it]) }
    }
}
