package io.github.fomes1574.freteshumberto.ui.historico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.DiaHistorico
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Historico
import io.github.fomes1574.freteshumberto.core.Textos
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Par
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloTela
import io.github.fomes1574.freteshumberto.ui.comum.Valor
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate

/**
 * Dias agrupados por mês, com o lucro do mês no cabeçalho (ARQUITETURA §3.6). Tocar no dia
 * abre o relatório daquele dia para conferir ou corrigir, sem prazo.
 */
@Composable
fun TelaHistorico(
    e: Estado,
    hoje: LocalDate,
    margens: PaddingValues,
    aoAbrirDia: (LocalDate) -> Unit,
    aoCompararDias: () -> Unit,
    aoContas: () -> Unit,
) {
    val meses = remember(e) { Historico.meses(e.diario, e.metas) }
    LazyColumn(
        Modifier.fillMaxSize().padding(margens),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TituloTela("Histórico", "Toque num dia para ver ou corrigir.") }
        item {
            Par(
                { m -> BotaoLinha("Comparar dois dias", aoCompararDias, m) },
                { m -> BotaoLinha("Contas do mês", aoContas, m, R.drawable.ic_conta_mes) },
            )
        }
        if (meses.isEmpty()) item { Cartao { TextoApagado("Ainda não há lançamentos.") } }
        meses.forEach { mes ->
            item(key = "m-${mes.mes}") {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 6.dp, end = 6.dp).semantics(mergeDescendants = true) { heading() }) {
                    Text(DataTexto.mesTitulo(mes.mes), style = MaterialTheme.typography.titleLarge, color = Cores.Apagado, modifier = Modifier.weight(1f))
                    Text(Formato.reais(mes.lucro), style = MaterialTheme.typography.titleLarge, color = if (mes.lucro < 0) Cores.Negativo else Cores.Apagado)
                }
            }
            items(mes.dias, key = { "d-${it.data}" }) { dia -> CartaoDia(dia, hoje, aoAbrirDia) }
        }
    }
}

@Composable
private fun CartaoDia(dia: DiaHistorico, hoje: LocalDate, aoAbrir: (LocalDate) -> Unit) {
    Cartao(Modifier.clickable(onClickLabel = "Abrir este dia", role = Role.Button) { aoAbrir(dia.data) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(DataTexto.curta(dia.data) + if (dia.data == hoje) " · hoje" else "", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ponto verde acompanha o texto: a cor nunca é a única informação.
                    if (dia.metaAtingida != null) {
                        Box(Modifier.size(12.dp).background(if (dia.metaAtingida == true) Cores.Verde else Cores.Linha, CircleShape))
                        Spacer(Modifier.width(6.dp))
                    }
                    val partes = listOfNotNull(
                        if (dia.metaAtingida == true) "Meta atingida" else if (dia.metaAtingida == false) "Abaixo da meta" else null,
                        Textos.fretes(dia.fretes),
                        if (dia.abasteceu) "abasteceu" else null,
                    )
                    TextoApagado(partes.joinToString(" · "))
                }
            }
            Valor(dia.lucro)
        }
    }
}
