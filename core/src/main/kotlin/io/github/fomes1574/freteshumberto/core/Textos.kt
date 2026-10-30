package io.github.fomes1574.freteshumberto.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Datas em português, sem depender do idioma do aparelho. */
object DataTexto {
    private val SEMANA = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    private val SEMANA_CURTA = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    private val MESES = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
    private val MESES_CURTOS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

    private fun dois(n: Int) = n.toString().padStart(2, '0')

    /** "Quinta, 29 de outubro" */
    fun longa(d: LocalDate) = "${SEMANA[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} de ${MESES[d.monthValue - 1]}"

    /** "Qui, 29 out" */
    fun curta(d: LocalDate) = "${SEMANA_CURTA[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} ${MESES_CURTOS[d.monthValue - 1]}"

    /** "Qui" */
    fun diaSemana(d: LocalDate) = SEMANA_CURTA[d.dayOfWeek.value - 1]

    /** "29/10" */
    fun diaMes(d: LocalDate) = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}"

    /** "29/10/2026" */
    fun completa(d: LocalDate) = "${diaMes(d)}/${d.year}"

    /** "Outubro de 2026" */
    fun mes(m: YearMonth) = "${MESES[m.monthValue - 1].replaceFirstChar { it.uppercase() }} de ${m.year}"

    /** "Outubro 2026" */
    fun mesTitulo(m: YearMonth) = "${MESES[m.monthValue - 1].replaceFirstChar { it.uppercase() }} ${m.year}"

    /** "out" */
    fun mesCurto(m: YearMonth) = MESES_CURTOS[m.monthValue - 1]
}

/** Período escolhido nos Relatórios. [DOIS_DIAS] soma só as duas datas, sem os dias do meio. */
enum class ModoRelatorio { DIA, SEMANA, MES, PERIODO, DOIS_DIAS }

data class FiltroRelatorio(
    val modo: ModoRelatorio,
    val referencia: LocalDate,
    val de: LocalDate = referencia,
    val ate: LocalDate = referencia,
    val doisDias: List<LocalDate> = emptyList(),
) {
    fun intervalo(): Intervalo = when (modo) {
        ModoRelatorio.DIA -> Datas.periodo(ModoPeriodo.DIA, referencia)
        ModoRelatorio.SEMANA -> Datas.periodo(ModoPeriodo.SEMANA, referencia)
        ModoRelatorio.MES -> Datas.periodo(ModoPeriodo.MES, referencia)
        ModoRelatorio.PERIODO -> Intervalo(de, ate)
        ModoRelatorio.DOIS_DIAS -> doisDias.sorted().let { Intervalo(it.first(), it.last()) }
    }

    /** Só as duas datas, quando o modo é "dois dias". */
    fun selecionadas(): Set<LocalDate>? = if (modo == ModoRelatorio.DOIS_DIAS) doisDias.toSet() else null

    fun modoPeriodo(): ModoPeriodo? = when (modo) {
        ModoRelatorio.DIA -> ModoPeriodo.DIA
        ModoRelatorio.SEMANA -> ModoPeriodo.SEMANA
        ModoRelatorio.MES -> ModoPeriodo.MES
        ModoRelatorio.PERIODO -> ModoPeriodo.PERSONALIZADO
        ModoRelatorio.DOIS_DIAS -> null
    }

    /** Setas: anda um dia, uma semana ou um mês. */
    fun deslocar(n: Long): FiltroRelatorio = when (modo) {
        ModoRelatorio.DIA -> copy(referencia = referencia.plusDays(n))
        ModoRelatorio.SEMANA -> copy(referencia = referencia.plusWeeks(n))
        ModoRelatorio.MES -> copy(referencia = Datas.somarMeses(referencia.withDayOfMonth(1), n))
        else -> this
    }

    fun parcial(hoje: LocalDate): Boolean {
        val p = intervalo()
        return modo != ModoRelatorio.DIA && modo != ModoRelatorio.DOIS_DIAS && hoje in p
    }

    fun rotulo(): String {
        val p = intervalo()
        return when (modo) {
            ModoRelatorio.DIA -> DataTexto.longa(p.inicio)
            ModoRelatorio.SEMANA -> "Semana de ${DataTexto.diaMes(p.inicio)} a ${DataTexto.diaMes(p.fim)}"
            ModoRelatorio.MES -> DataTexto.mes(YearMonth.from(p.inicio))
            ModoRelatorio.PERIODO -> "${DataTexto.completa(p.inicio)} a ${DataTexto.completa(p.fim)}"
            ModoRelatorio.DOIS_DIAS -> "${DataTexto.completa(p.inicio)} e ${DataTexto.completa(p.fim)}"
        }
    }

    fun titulo(): String = when (modo) {
        ModoRelatorio.DIA -> "LUCRO DO DIA"
        ModoRelatorio.SEMANA -> "LUCRO DA SEMANA"
        ModoRelatorio.MES -> "LUCRO DO MÊS"
        else -> "LUCRO DO PERÍODO"
    }
}

object Textos {
    /**
     * Comparação neutra com o período anterior equivalente (R07). Null se não houver
     * lançamentos no período anterior ou no modo "dois dias".
     */
    fun comparacao(lancamentos: List<Lancamento>, filtro: FiltroRelatorio, atual: Resumo, metas: Metas, hoje: LocalDate): String? {
        val modo = filtro.modoPeriodo() ?: return null
        val ant = Datas.anterior(modo, filtro.intervalo(), hoje)
        val r = Relatorio.resumir(lancamentos, ant, metas)
        if (r.porDia.isEmpty()) return null
        val delta = atual.lucro - r.lucro
        val parcial = filtro.parcial(hoje)
        return "${Formato.reais(kotlin.math.abs(delta))} ${if (delta >= 0) "a mais" else "a menos"} que " +
            "${DataTexto.diaMes(ant.inicio)} a ${DataTexto.diaMes(ant.fim)}${if (parcial) " (mesmos dias)" else ""}"
    }

    /** Texto para mandar pelo WhatsApp. Porta de `shareText`. */
    fun resumoParaEnviar(filtro: FiltroRelatorio, r: Resumo, metas: Metas): String {
        val p = filtro.intervalo()
        val meta = if (filtro.modo == ModoRelatorio.DIA) "\nMeta do dia: ${Formato.reais(metas.doDia(p.inicio))}" else ""
        return "Meus Fretes — ${filtro.rotulo()}\n" +
            "Fretes: ${Formato.reais(r.fretes)} (${r.qtdFretes})\n" +
            "Combustível: ${Formato.reais(r.combustivel)}\n" +
            "Outras despesas: ${Formato.reais(r.outras)}\n" +
            "Lucro: ${Formato.reais(r.lucro)}$meta"
    }

    fun fretes(n: Int) = "$n frete${if (n == 1) "" else "s"}"

    fun abastecimentos(n: Int) = "$n abastecimento${if (n == 1) "" else "s"}"
}

data class DiaHistorico(
    val data: LocalDate,
    val lucro: Long,
    val fretes: Int,
    val abasteceu: Boolean,
    /** Null em dia sem frete: só dias trabalhados contam para a meta (R06). */
    val metaAtingida: Boolean?,
    val textoMeta: String?,
)

data class MesHistorico(val mes: YearMonth, val lucro: Long, val dias: List<DiaHistorico>)

object Historico {
    /** Dias com lançamento, do mais novo para o mais antigo, agrupados por mês. */
    fun meses(di: Diario, metas: Metas): List<MesHistorico> {
        val porData = di.lancamentos.groupBy { it.data }
        return porData.keys.sortedDescending().groupBy { YearMonth.from(it) }.map { (mes, datas) ->
            val dias = datas.map { d ->
                val r = Relatorio.resumir(porData.getValue(d), Intervalo(d, d), metas)
                val meta = metas.doDia(d)
                DiaHistorico(
                    data = d, lucro = r.lucro, fretes = r.qtdFretes, abasteceu = r.qtdAbastecimentos > 0,
                    metaAtingida = if (r.qtdFretes > 0) r.lucro >= meta else null,
                    textoMeta = if (r.qtdFretes > 0) textoMeta(r.lucro, meta) else null,
                )
            }
            MesHistorico(mes, dias.sumOf { it.lucro }, dias)
        }
    }
}

object LembreteCopia {
    const val DIAS = 7L

    /** Lembrete semanal dentro do app (R09): sem cópia ou cópia com 7 dias ou mais. */
    fun precisa(ultima: Instant?, agora: Instant): Boolean = ultima == null || Duration.between(ultima, agora).toDays() >= DIAS
}
