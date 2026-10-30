package io.github.fomes1574.freteshumberto.ui.relatorios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.fomes1574.freteshumberto.core.Datas
import io.github.fomes1574.freteshumberto.core.FiltroRelatorio
import io.github.fomes1574.freteshumberto.core.ModoRelatorio
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.CampoData
import io.github.fomes1574.freteshumberto.ui.comum.Erro
import io.github.fomes1574.freteshumberto.ui.comum.TelaSobreposta
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import java.time.LocalDate

/** Período personalizado: duas datas incluídas, até 6 meses de calendário (R07). */
@Composable
fun TelaEscolherPeriodo(atual: FiltroRelatorio, hoje: LocalDate, aoEscolher: (FiltroRelatorio) -> Unit, aoFechar: () -> Unit) {
    val inicial = if (atual.modo == ModoRelatorio.PERIODO) atual.intervalo() else Datas.mes(hoje)
    var de by rememberSaveable { mutableStateOf(inicial.inicio.toString()) }
    var ate by rememberSaveable { mutableStateOf(minOf(inicial.fim, hoje).toString()) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    TelaSobreposta("Escolher período", aoFechar) {
        CampoData("Data inicial", LocalDate.parse(de), { it?.let { d -> de = d.toString(); erro = null } }, hoje)
        CampoData("Data final", LocalDate.parse(ate), { it?.let { d -> ate = d.toString(); erro = null } }, hoje)
        TextoApagado("Até 6 meses por consulta. O histórico mais antigo continua guardado e pode ser corrigido.")
        Erro(erro)
        Botao("Ver período", {
            val a = LocalDate.parse(de); val b = LocalDate.parse(ate)
            val msg = Datas.validarIntervalo(a, b, hoje)
            if (msg != null) erro = msg else aoEscolher(FiltroRelatorio(ModoRelatorio.PERIODO, b, de = a, ate = b))
        })
    }
}

/** Soma e compara só as duas datas, sem os dias do meio (R07). */
@Composable
fun TelaDoisDias(atual: FiltroRelatorio, hoje: LocalDate, aoEscolher: (FiltroRelatorio) -> Unit, aoFechar: () -> Unit) {
    val iniciais = if (atual.modo == ModoRelatorio.DOIS_DIAS && atual.doisDias.size == 2) atual.doisDias else listOf(hoje.minusDays(1), hoje)
    var a by rememberSaveable { mutableStateOf(iniciais[0].toString()) }
    var b by rememberSaveable { mutableStateOf(iniciais[1].toString()) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    TelaSobreposta("Comparar dois dias", aoFechar) {
        CampoData("Primeiro dia", LocalDate.parse(a), { it?.let { d -> a = d.toString(); erro = null } }, hoje)
        CampoData("Segundo dia", LocalDate.parse(b), { it?.let { d -> b = d.toString(); erro = null } }, hoje)
        Erro(erro)
        Botao("Somar e comparar", {
            val x = LocalDate.parse(a); val y = LocalDate.parse(b)
            when {
                x == y -> erro = "Escolha dois dias diferentes."
                x.isAfter(hoje) || y.isAfter(hoje) -> erro = "As datas não podem ser no futuro."
                else -> aoEscolher(FiltroRelatorio(ModoRelatorio.DOIS_DIAS, maxOf(x, y), doisDias = listOf(x, y).sorted()))
            }
        })
    }
}
