package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate

/**
 * Estado do diário em memória, com as operações de salvar/apagar/desfazer/conferir.
 * É a referência de comportamento para o repositório Room: cada operação aqui
 * vira UMA transação no banco.
 */
data class Diario(
    val lancamentos: List<Lancamento> = emptyList(),
    val ocorrencias: Map<String, Ocorrencia> = emptyMap(),
    /** Revisão de cada dia: aumenta a cada alteração naquela data. */
    val revisoes: Map<LocalDate, Int> = emptyMap(),
    /** Revisão do dia no momento em que foi conferido. */
    val conferidos: Map<LocalDate, Int> = emptyMap(),
) {
    private fun tocar(d: LocalDate, r: Map<LocalDate, Int>) = r + (d to ((r[d] ?: 0) + 1))

    /** Lançamento removido + estado anterior da ocorrência, para o "Desfazer". */
    data class Removido(val lancamento: Lancamento, val ocorrenciaAntes: Ocorrencia?)

    /**
     * Cria ou edita. Mover um frete de data só afeta ele (R01): nenhuma despesa
     * ou abastecimento é movido junto. Edição sem prazo, mesmo em dia conferido.
     */
    fun salvar(l: Lancamento): Diario {
        val antigo = lancamentos.firstOrNull { it.id == l.id }
        var rev = revisoes
        if (antigo != null) rev = tocar(antigo.data, rev)
        val lista = if (antigo != null) lancamentos.map { if (it.id == l.id) l else it } else lancamentos + l
        if (antigo == null || antigo.data != l.data) rev = tocar(l.data, rev)
        var occ = ocorrencias
        val chave = l.ocorrencia
        if (chave != null) {
            occ[chave]?.let { o ->
                occ = occ + (chave to o.copy(status = StatusOcorrencia.GERADA, alteracao = AlteracaoMes(l.data, l.valor, l.descricao, l.categoria ?: "Outros")))
            }
        }
        return copy(lancamentos = lista, revisoes = rev, ocorrencias = occ)
    }

    /** Apaga só este lançamento. Se veio de conta do mês, aquele mês fica "pulado" (não recria). */
    fun apagar(id: String): Pair<Diario, Removido?> {
        val l = lancamentos.firstOrNull { it.id == id } ?: return this to null
        val antes = l.ocorrencia?.let { ocorrencias[it] }
        var occ = ocorrencias
        if (l.ocorrencia != null && antes != null) occ = occ + (l.ocorrencia to antes.copy(status = StatusOcorrencia.PULADA, lancamentoId = null))
        return copy(lancamentos = lancamentos.filter { it.id != id }, ocorrencias = occ, revisoes = tocar(l.data, revisoes)) to Removido(l, antes)
    }

    /** Desfazer: devolve o lançamento uma única vez (tocar duas vezes não duplica). */
    fun restaurar(r: Removido): Diario {
        if (lancamentos.any { it.id == r.lancamento.id }) return this
        var occ = ocorrencias
        val chave = r.lancamento.ocorrencia
        if (chave != null && r.ocorrenciaAntes != null) occ = occ + (chave to r.ocorrenciaAntes)
        return copy(lancamentos = lancamentos + r.lancamento, ocorrencias = occ, revisoes = tocar(r.lancamento.data, revisoes))
    }

    /**
     * Retira o lançamento sem marcar o mês como pulado. Usado pelas contas do mês quando
     * a ocorrência volta a ser só prevista (ex.: alteração para uma data futura).
     */
    fun removerSemPular(id: String): Diario {
        val l = lancamentos.firstOrNull { it.id == id } ?: return this
        return copy(lancamentos = lancamentos.filter { it.id != id }, revisoes = tocar(l.data, revisoes))
    }

    fun comOcorrencia(o: Ocorrencia): Diario = copy(ocorrencias = ocorrencias + (o.chave to o))

    fun conferir(d: LocalDate): Diario = copy(conferidos = conferidos + (d to (revisoes[d] ?: 0)))

    /** Conferido e sem alterações depois da conferência. Conferir nunca bloqueia edição. */
    fun conferidoAtual(d: LocalDate): Boolean = conferidos[d]?.let { it == (revisoes[d] ?: 0) } ?: false

    fun aplicarContasDoMes(contas: List<ContaMensal>, hoje: LocalDate, novoId: () -> String, agora: String = ""): Diario {
        val g = Recorrencias.gerar(contas, ocorrencias, hoje, novoId, agora)
        if (g.novos.isEmpty()) return this
        var rev = revisoes
        for (l in g.novos) rev = tocar(l.data, rev)
        return copy(lancamentos = lancamentos + g.novos, ocorrencias = g.ocorrencias, revisoes = rev)
    }
}
