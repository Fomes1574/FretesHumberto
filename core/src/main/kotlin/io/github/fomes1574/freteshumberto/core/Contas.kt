package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate
import java.time.YearMonth

/** Como fica uma conta num mês, para a tela Contas do mês. */
data class SituacaoConta(
    val conta: ContaMensal,
    val mes: YearMonth,
    val versao: VersaoConta,
    val data: LocalDate,
    val descricao: String,
    val valor: Long,
    val texto: String,
    val pulada: Boolean,
    val ativa: Boolean,
    val lancamentoId: String?,
)

/**
 * Ações da tela Contas do mês (R08). Porta de submitRec, recSkip, recToggle e reconcileRec
 * do protótipo. Todas terminam gerando as despesas devidas, como no protótipo; o repositório
 * grava o resultado numa única transação.
 */
object ContasDoMes {
    fun situacoes(e: Estado, mes: YearMonth): List<SituacaoConta> = e.contas.mapNotNull { c ->
        val v = c.versaoEm(mes) ?: return@mapNotNull null
        if (mes.isBefore(YearMonth.from(c.inicio))) return@mapNotNull null
        val o = e.diario.ocorrencias[Ocorrencia.chave(c.id, mes)]
        val l = o?.lancamentoId?.let { id -> e.lancamentos.firstOrNull { it.id == id } }
        val data = l?.data ?: o?.alteracao?.data ?: Recorrencias.vencimento(mes, v.dia)
        val texto = when {
            o?.status == StatusOcorrencia.PULADA -> "Pulada neste mês"
            l != null -> "Lançada"
            !v.ativa -> "Repetição pausada"
            v.fim != null && data.isAfter(v.fim) -> "Repetição encerrada"
            else -> "Prevista"
        }
        SituacaoConta(
            conta = c, mes = mes, versao = v, data = data,
            descricao = l?.descricao ?: o?.alteracao?.descricao ?: v.nome,
            valor = l?.valor ?: o?.alteracao?.valor ?: v.valor,
            texto = texto, pulada = o?.status == StatusOcorrencia.PULADA, ativa = v.ativa, lancamentoId = l?.id,
        )
    }

    /** Valida os campos do formulário. Devolve a mensagem para a pessoa, ou null. */
    fun validar(nome: String, valor: Long?, dia: Int?, inicio: LocalDate?, fim: LocalDate?): String? = when {
        nome.isBlank() || nome.length > 160 || valor == null || valor !in 1..Dinheiro.MAX ||
            dia == null || dia !in 1..31 || inicio == null || (fim != null && fim.isBefore(inicio)) ->
            "Confira nome, valor, dia e datas."
        else -> null
    }

    fun cadastrar(
        e: Estado, id: String, nome: String, valor: Long, dia: Int, inicio: LocalDate, fim: LocalDate?,
        hoje: LocalDate, novoId: () -> String, agora: String = "",
    ): Estado {
        val v = VersaoConta(YearMonth.from(inicio), nome.trim(), valor, dia, true, CATEGORIA_CONTA, fim)
        return e.copy(contas = e.contas + ContaMensal(id, inicio, listOf(v))).gerarContas(hoje, novoId, agora)
    }

    /** "Só este mês": exceção naquela ocorrência; as demais seguem a regra. */
    fun editarSoEsteMes(
        e: Estado, contaId: String, mes: YearMonth, nome: String, valor: Long, dia: Int,
        hoje: LocalDate, novoId: () -> String, agora: String = "",
    ): Estado {
        val chave = Ocorrencia.chave(contaId, mes)
        val alt = AlteracaoMes(Recorrencias.vencimento(mes, dia), valor, nome.trim(), CATEGORIA_CONTA)
        var di = e.diario
        val o = di.ocorrencias[chave]
        val l = o?.lancamentoId?.let { id -> di.lancamentos.firstOrNull { it.id == id } }
        if (l != null && !alt.data.isAfter(hoje)) {
            di = di.salvar(l.copy(data = alt.data, valor = alt.valor, descricao = alt.descricao, categoria = alt.categoria, editadoEm = agora.ifEmpty { null }))
        } else {
            if (l != null) di = di.removerSemPular(l.id)
            di = di.comOcorrencia(Ocorrencia(contaId, mes, StatusOcorrencia.PREVISTA, null, alt))
        }
        return e.copy(diario = di).gerarContas(hoje, novoId, agora)
    }

    /**
     * "Deste mês em diante": nova versão desde [mes]; versões anteriores ficam guardadas.
     * Despesas já lançadas a partir desse mês, sem exceção individual, passam a seguir a nova regra.
     */
    fun editarDesteMesEmDiante(
        e: Estado, contaId: String, mes: YearMonth, nome: String, valor: Long, dia: Int, fim: LocalDate?,
        hoje: LocalDate, novoId: () -> String, agora: String = "",
    ): Estado {
        val c = e.contas.first { it.id == contaId }
        val nova = c.copy(versoes = c.versoes.filter { it.desde.isBefore(mes) } + VersaoConta(mes, nome.trim(), valor, dia, true, CATEGORIA_CONTA, fim))
        var di = e.diario
        for (o in di.ocorrencias.values.filter { it.contaId == contaId && !it.mes.isBefore(mes) }) {
            if (o.status == StatusOcorrencia.PULADA || o.alteracao != null) continue
            val l = o.lancamentoId?.let { id -> di.lancamentos.firstOrNull { it.id == id } } ?: continue
            val v = nova.versaoEm(o.mes) ?: continue
            if (!v.ativa) continue
            val d = Recorrencias.vencimento(o.mes, v.dia)
            if (d.isAfter(hoje) || (v.fim != null && d.isAfter(v.fim))) {
                di = di.removerSemPular(l.id).comOcorrencia(Ocorrencia(contaId, o.mes, StatusOcorrencia.PREVISTA))
            } else {
                di = di.salvar(l.copy(data = d, valor = v.valor, descricao = v.nome, categoria = v.categoria, editadoEm = agora.ifEmpty { null }))
                // salvar() registra a mudança como exceção do mês; aqui ela segue a regra nova.
                di = di.comOcorrencia(di.ocorrencias.getValue(o.chave).copy(alteracao = null))
            }
        }
        return e.copy(contas = e.contas.map { if (it.id == contaId) nova else it }, diario = di).gerarContas(hoje, novoId, agora)
    }

    /**
     * "Pular este mês" retira só a despesa daquele mês e guarda que foi pulado.
     * "Retomar este mês" vale só para aquele mês, mesmo com a repetição pausada.
     */
    fun alternarPulo(e: Estado, contaId: String, mes: YearMonth, hoje: LocalDate, novoId: () -> String, agora: String = ""): Estado {
        val chave = Ocorrencia.chave(contaId, mes)
        var di = e.diario
        val o = di.ocorrencias[chave]
        if (o?.status == StatusOcorrencia.PULADA) {
            val v = e.contas.first { it.id == contaId }.versaoEm(mes) ?: return e
            val alt = o.alteracao ?: AlteracaoMes(Recorrencias.vencimento(mes, v.dia), v.valor, v.nome, v.categoria)
            di = di.comOcorrencia(Ocorrencia(contaId, mes, StatusOcorrencia.PREVISTA, null, alt))
            return e.copy(diario = di).gerarContas(hoje, novoId, agora)
        }
        o?.lancamentoId?.let { di = di.removerSemPular(it) }
        di = di.comOcorrencia(Ocorrencia(contaId, mes, StatusOcorrencia.PULADA))
        return e.copy(diario = di)
    }

    /** Pausar ou retomar a repetição a partir de [mes]. Despesas já lançadas ficam. */
    fun alternarPausa(e: Estado, contaId: String, mes: YearMonth, hoje: LocalDate, novoId: () -> String, agora: String = ""): Estado {
        val c = e.contas.first { it.id == contaId }
        val v = c.versaoEm(mes) ?: return e
        val nova = c.copy(versoes = c.versoes.filter { it.desde.isBefore(mes) } + v.copy(desde = mes, ativa = !v.ativa))
        return e.copy(contas = e.contas.map { if (it.id == contaId) nova else it }).gerarContas(hoje, novoId, agora)
    }
}
