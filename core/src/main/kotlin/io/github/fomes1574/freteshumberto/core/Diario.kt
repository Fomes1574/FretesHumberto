package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate

/**
 * Estado do diário em memória, com as operações de salvar/apagar/desfazer.
 * É a referência de comportamento para o repositório Room: cada operação aqui
 * vira UMA transação no banco.
 */
data class Diario(
    val lancamentos: List<Lancamento> = emptyList(),
    val ocorrencias: Map<String, Ocorrencia> = emptyMap(),
) {
    /** Lançamento removido + estado anterior da ocorrência, para o "Desfazer". */
    data class Removido(val lancamento: Lancamento, val ocorrenciaAntes: Ocorrencia?)

    /**
     * Cria ou edita. Mover um frete de data só afeta ele (R01): nenhuma despesa
     * ou abastecimento é movido junto. Edição sem prazo.
     */
    fun salvar(l: Lancamento): Diario {
        val antigo = lancamentos.firstOrNull { it.id == l.id }
        val lista = if (antigo != null) lancamentos.map { if (it.id == l.id) l else it } else lancamentos + l
        var occ = ocorrencias
        val chave = l.ocorrencia
        if (chave != null) {
            occ[chave]?.let { o ->
                occ = occ + (chave to o.copy(status = StatusOcorrencia.GERADA, alteracao = AlteracaoMes(l.data, l.valor, l.descricao, l.categoria ?: "Outros")))
            }
        }
        return copy(lancamentos = lista, ocorrencias = occ)
    }

    /** Apaga só este lançamento. Se veio de conta do mês, aquele mês fica "pulado" (não recria). */
    fun apagar(id: String): Pair<Diario, Removido?> {
        val l = lancamentos.firstOrNull { it.id == id } ?: return this to null
        val antes = l.ocorrencia?.let { ocorrencias[it] }
        var occ = ocorrencias
        if (l.ocorrencia != null && antes != null) occ = occ + (l.ocorrencia to antes.copy(status = StatusOcorrencia.PULADA, lancamentoId = null))
        return copy(lancamentos = lancamentos.filter { it.id != id }, ocorrencias = occ) to Removido(l, antes)
    }

    /** Desfazer: devolve o lançamento uma única vez (tocar duas vezes não duplica). */
    fun restaurar(r: Removido): Diario {
        if (lancamentos.any { it.id == r.lancamento.id }) return this
        var occ = ocorrencias
        val chave = r.lancamento.ocorrencia
        if (chave != null && r.ocorrenciaAntes != null) occ = occ + (chave to r.ocorrenciaAntes)
        return copy(lancamentos = lancamentos + r.lancamento, ocorrencias = occ)
    }

    /**
     * Retira o lançamento sem marcar o mês como pulado. Usado pelas contas do mês quando
     * a ocorrência volta a ser só prevista (ex.: alteração para uma data futura).
     */
    fun removerSemPular(id: String): Diario = copy(lancamentos = lancamentos.filter { it.id != id })

    fun comOcorrencia(o: Ocorrencia): Diario = copy(ocorrencias = ocorrencias + (o.chave to o))

    fun aplicarContasDoMes(contas: List<ContaMensal>, hoje: LocalDate, novoId: () -> String, agora: String = ""): Diario {
        val g = Recorrencias.gerar(contas, ocorrencias, hoje, novoId, agora)
        if (g.novos.isEmpty()) return this
        return copy(lancamentos = lancamentos + g.novos, ocorrencias = g.ocorrencias)
    }
}
