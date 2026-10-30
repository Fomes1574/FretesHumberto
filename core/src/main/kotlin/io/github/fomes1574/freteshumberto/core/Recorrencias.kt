package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate
import java.time.YearMonth

/** Versão de uma conta do mês, válida a partir do mês [desde]. Versões antigas ficam guardadas. */
data class VersaoConta(
    val desde: YearMonth,
    val nome: String,
    val valor: Long,
    /** Dia original (1–31). Guardado mesmo quando o mês não tem esse dia. */
    val dia: Int,
    val ativa: Boolean = true,
    val categoria: String = "Outros",
    val fim: LocalDate? = null,
) {
    init {
        require(nome.isNotBlank() && nome.length <= 160)
        require(valor in 1..Dinheiro.MAX)
        require(dia in 1..31)
    }
}

data class ContaMensal(val id: String, val inicio: LocalDate, val versoes: List<VersaoConta>) {
    init { require(versoes.isNotEmpty()) }

    fun versaoEm(m: YearMonth): VersaoConta? = versoes.filter { !it.desde.isAfter(m) }.maxByOrNull { it.desde }
}

enum class StatusOcorrencia { PREVISTA, GERADA, PULADA }

/** Alteração feita só naquele mês ("Só este mês"). */
data class AlteracaoMes(val data: LocalDate, val valor: Long, val descricao: String, val categoria: String)

data class Ocorrencia(
    val contaId: String,
    val mes: YearMonth,
    val status: StatusOcorrencia,
    val lancamentoId: String? = null,
    val alteracao: AlteracaoMes? = null,
) {
    val chave: String get() = chave(contaId, mes)

    companion object {
        /** Única por conta + mês: impede duplicar ao reabrir, restaurar ou rodar o agendador de novo. */
        fun chave(contaId: String, mes: YearMonth) = "$contaId|$mes"
    }
}

object Recorrencias {
    /**
     * Vencimento: dia original limitado ao fim do mês. Fevereiro usa 28 para dias 29–31,
     * inclusive em ano bissexto (decisão do projeto, não corrigir para 29).
     */
    fun vencimento(m: YearMonth, dia: Int): LocalDate {
        val ultimo = if (m.monthValue == 2) 28 else m.lengthOfMonth()
        return m.atDay(minOf(dia, ultimo))
    }

    data class Geracao(val novos: List<Lancamento>, val ocorrencias: Map<String, Ocorrencia>)

    /**
     * Gera as despesas devidas até [hoje]. Idempotente: rodar de novo não duplica;
     * mês pulado continua pulado; pausa e data final são respeitadas.
     * Porta de `Core.generate`. O WorkManager e a abertura do app devem chamar esta mesma função.
     */
    fun gerar(
        contas: List<ContaMensal>,
        ocorrencias: Map<String, Ocorrencia>,
        hoje: LocalDate,
        novoId: () -> String,
        agora: String = "",
    ): Geracao {
        val occ = ocorrencias.toMutableMap()
        val novos = mutableListOf<Lancamento>()
        val ultimo = YearMonth.from(hoje)
        for (c in contas) {
            var m = YearMonth.from(c.inicio)
            var guarda = 0
            while (!m.isAfter(ultimo) && guarda++ < 2400) {
                val chave = Ocorrencia.chave(c.id, m)
                val v = c.versaoEm(m)
                val o = occ[chave]
                if (v != null && (v.ativa || o?.alteracao != null) &&
                    o?.status != StatusOcorrencia.PULADA && o?.status != StatusOcorrencia.GERADA
                ) {
                    val data = o?.alteracao?.data ?: vencimento(m, v.dia)
                    if (!data.isBefore(c.inicio) && !data.isAfter(hoje) && (v.fim == null || !data.isAfter(v.fim))) {
                        val l = Lancamento(
                            id = novoId(), tipo = Tipo.DESPESA, data = data,
                            valor = o?.alteracao?.valor ?: v.valor,
                            descricao = o?.alteracao?.descricao ?: v.nome,
                            categoria = o?.alteracao?.categoria ?: v.categoria,
                            ocorrencia = chave, criadoEm = agora,
                        )
                        novos += l
                        occ[chave] = Ocorrencia(c.id, m, StatusOcorrencia.GERADA, l.id, o?.alteracao)
                    }
                }
                m = m.plusMonths(1)
            }
        }
        return Geracao(novos, occ)
    }
}
