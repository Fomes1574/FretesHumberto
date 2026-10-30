package io.github.fomes1574.freteshumberto.core

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Período inclusivo nas duas pontas. */
data class Intervalo(val inicio: LocalDate, val fim: LocalDate) {
    init { require(!fim.isBefore(inicio)) { "Fim antes do início" } }
    operator fun contains(d: LocalDate): Boolean = !d.isBefore(inicio) && !d.isAfter(fim)
    fun dias(): List<LocalDate> = generateSequence(inicio) { it.plusDays(1) }.takeWhile { !it.isAfter(fim) }.toList()
}

enum class ModoPeriodo { DIA, SEMANA, MES, PERSONALIZADO }

/**
 * Datas financeiras são LocalDate (sem horário, sem fuso). Porta de addD/addM/monthRange/
 * weekStart/validRange/previousRange do protótipo.
 */
object Datas {
    /**
     * O dia vira às 00:00 pelo horário de Brasília, mesmo se o celular estiver em outro fuso.
     * Depois da meia-noite, "hoje" é um dia novo, sem fretes nem despesas.
     */
    val FUSO: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun hoje(relogio: Clock): LocalDate = LocalDate.now(relogio.withZone(FUSO))

    /** Tempo até a próxima meia-noite de Brasília. */
    fun ateMeiaNoite(relogio: Clock): Duration {
        val agora = ZonedDateTime.now(relogio.withZone(FUSO))
        return Duration.between(agora, agora.toLocalDate().plusDays(1).atStartOfDay(FUSO))
    }

    /** Semana de segunda a domingo. */
    fun inicioSemana(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun semana(d: LocalDate): Intervalo = inicioSemana(d).let { Intervalo(it, it.plusDays(6)) }

    fun mes(d: LocalDate): Intervalo = Intervalo(d.withDayOfMonth(1), d.with(TemporalAdjusters.lastDayOfMonth()))

    fun mes(m: YearMonth): Intervalo = Intervalo(m.atDay(1), m.atEndOfMonth())

    /** Igual a `addM`: soma meses limitando o dia ao fim do mês (31/jan + 1 = 28/fev). */
    fun somarMeses(d: LocalDate, n: Long): LocalDate = d.plusMonths(n)

    fun diasEntre(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)

    /** Período do relatório conforme o modo e a data de referência. */
    fun periodo(modo: ModoPeriodo, referencia: LocalDate): Intervalo = when (modo) {
        ModoPeriodo.DIA -> Intervalo(referencia, referencia)
        ModoPeriodo.SEMANA -> semana(referencia)
        ModoPeriodo.MES -> mes(referencia)
        ModoPeriodo.PERSONALIZADO -> error("Período personalizado precisa de início e fim")
    }

    /** Período em andamento (contém hoje e não é só um dia) recebe o selo "Parcial". */
    fun parcial(modo: ModoPeriodo, p: Intervalo, hoje: LocalDate): Boolean = modo != ModoPeriodo.DIA && hoje in p

    /**
     * Valida intervalo personalizado. Retorna mensagem para a pessoa, ou null se estiver certo.
     * Até 6 meses, com as duas datas incluídas; fim não pode ser no futuro.
     */
    fun validarIntervalo(a: LocalDate, b: LocalDate, hoje: LocalDate): String? = when {
        a.isAfter(b) -> "Confira as datas inicial e final."
        b.isAfter(hoje) -> "A data final não pode ser no futuro."
        !b.isBefore(somarMeses(a, 6)) -> "Escolha um intervalo de até 6 meses."
        else -> null
    }

    /**
     * Período anterior equivalente para comparação. Se o período atual está em andamento,
     * compara o mesmo recorte (ex.: seg–qui com seg–qui da semana passada).
     */
    fun anterior(modo: ModoPeriodo, p: Intervalo, hoje: LocalDate): Intervalo {
        val emAndamento = !p.fim.isBefore(hoje) && !p.inicio.isAfter(hoje)
        val fim = if (emAndamento) hoje else p.fim
        return when (modo) {
            ModoPeriodo.DIA -> p.inicio.minusDays(1).let { Intervalo(it, it) }
            ModoPeriodo.SEMANA -> Intervalo(p.inicio.minusDays(7), fim.minusDays(7))
            ModoPeriodo.MES -> {
                val m = mes(somarMeses(p.inicio, -1))
                if (emAndamento) Intervalo(m.inicio, m.inicio.withDayOfMonth(minOf(hoje.dayOfMonth, m.fim.dayOfMonth)))
                else m
            }
            ModoPeriodo.PERSONALIZADO -> {
                val n = diasEntre(p.inicio, fim) + 1
                Intervalo(p.inicio.minusDays(n), p.inicio.minusDays(1))
            }
        }
    }
}
