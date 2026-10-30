package io.github.fomes1574.freteshumberto.ui.hoje

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Datas
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Intervalo
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.Relatorio
import io.github.fomes1574.freteshumberto.core.Resumo
import io.github.fomes1574.freteshumberto.core.Textos
import io.github.fomes1574.freteshumberto.core.Tipo
import io.github.fomes1574.freteshumberto.core.textoMeta
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoIcone
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Kpi
import io.github.fomes1574.freteshumberto.ui.comum.LinhaConta
import io.github.fomes1574.freteshumberto.ui.comum.Par
import io.github.fomes1574.freteshumberto.ui.comum.Rotulo
import io.github.fomes1574.freteshumberto.ui.comum.Selo
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloCartao
import io.github.fomes1574.freteshumberto.ui.comum.TituloTela
import io.github.fomes1574.freteshumberto.ui.comum.Valor
import io.github.fomes1574.freteshumberto.ui.estrada.CenaEstrada
import io.github.fomes1574.freteshumberto.ui.estrada.rotuloMeta
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate

/** Ações da tela Hoje e da lista de lançamentos. */
class AcoesHoje(
    val novo: (Tipo, LocalDate) -> Unit,
    val calcular: () -> Unit,
    val editar: (Lancamento) -> Unit,
    val apagar: (Lancamento) -> Unit,
    val conferir: (LocalDate) -> Unit,
    val verMes: () -> Unit,
    val irCopia: () -> Unit,
)

@Composable
fun TelaHoje(e: Estado, hoje: LocalDate, posicoes: MutableMap<String, Float>, lembrarCopia: Boolean, margens: PaddingValues, acoes: AcoesHoje) {
    val r = Relatorio.resumir(e.lancamentos, Intervalo(hoje, hoje), e.metas)
    val meta = e.metas.doDia(hoje)
    val mes = Relatorio.resumir(e.lancamentos, Intervalo(Datas.mes(hoje).inicio, hoje), e.metas)
    val doDia = e.lancamentos.filter { it.data == hoje }.asReversed()

    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TituloTela("Meus Fretes", DataTexto.longa(hoje))
        Cartao {
            Rotulo("Lucro de hoje")
            Valor(r.lucro, grande = true)
            Text(textoMeta(r.lucro, meta), style = MaterialTheme.typography.titleMedium)
            CenaEstrada(r.lucro, meta, "dia", posicoes, rotuloMeta(meta))
        }
        Botao("Anotar frete", { acoes.novo(Tipo.FRETE, hoje) }, icone = R.drawable.ic_mais, alto = true)
        Par(
            { m -> Botao("Abasteci", { acoes.novo(Tipo.ABASTECIMENTO, hoje) }, m, R.drawable.ic_combustivel, Cores.CombustivelClaro, Cores.Combustivel) },
            { m -> Botao("Outra despesa", { acoes.novo(Tipo.DESPESA, hoje) }, m, R.drawable.ic_despesa, androidx.compose.ui.graphics.Color(0xFFE7E3D8), Cores.Tinta) },
        )
        BotaoLinha("Calcular um frete", acoes.calcular, icone = R.drawable.ic_calculadora)

        Cartao {
            TituloCartao("Conta de hoje")
            ContaDoPeriodo(r)
            TextoApagado("Combustível pelos abastecimentos anotados.")
        }

        Cartao {
            TituloCartao("Lançado hoje")
            if (doDia.isEmpty()) TextoApagado("Nada anotado ainda hoje.")
            ListaLancamentos(doDia, acoes.editar, acoes.apagar)
            if (doDia.isNotEmpty()) BotaoLinha(Textos.conferencia(e.diario, hoje), { acoes.conferir(hoje) }, icone = R.drawable.ic_conferido)
        }

        Cartao {
            Rotulo("Este mês até agora")
            Par(
                { m -> Kpi(Formato.reais(mes.lucro), "lucro no mês", m, negativo = mes.lucro < 0) },
                { m -> Kpi(Formato.reais(mes.combustivel), "em combustível", m) },
            )
            BotaoLinha("Ver relatório do mês", acoes.verMes)
        }

        if (lembrarCopia && e.lancamentos.isNotEmpty()) {
            Cartao(cor = Cores.VerdeClaro) {
                Text("Faz uma semana ou mais sem cópia de segurança.", style = MaterialTheme.typography.titleMedium)
                BotaoLinha("Fazer cópia agora", acoes.irCopia)
            }
        }
    }
}

/** Fretes − combustível − outras despesas = lucro (R02). */
@Composable
fun ContaDoPeriodo(r: Resumo) {
    LinhaConta("Fretes", Formato.reais(r.fretes), "(${r.qtdFretes})")
    LinhaConta("Combustível", "− ${Formato.reais(r.combustivel)}", if (r.qtdAbastecimentos > 0) "(${r.qtdAbastecimentos} abastec.)" else null)
    LinhaConta("Outras despesas", "− ${Formato.reais(r.outras)}")
    LinhaConta("Lucro", Formato.reais(r.lucro), total = true, negativo = r.lucro < 0)
}

@Composable
fun ListaLancamentos(lista: List<Lancamento>, aoEditar: (Lancamento) -> Unit, aoApagar: (Lancamento) -> Unit) {
    lista.forEachIndexed { i, l ->
        if (i > 0) HorizontalDivider(color = Cores.Linha)
        LinhaLancamento(l, aoEditar, aoApagar)
    }
}

/** Número em centésimos sem ",00" quando inteiro: 4500 → "45"; 5405 → "54,05". */
fun numero(c: Long): String = if (c % 100 == 0L) Formato.centesimos(c).substringBefore(',') else Formato.centesimos(c)

fun descricaoDe(l: Lancamento): String = l.descricao.ifBlank {
    when (l.tipo) {
        Tipo.FRETE -> "Frete"
        Tipo.ABASTECIMENTO -> "Abastecimento"
        Tipo.DESPESA -> l.categoria ?: "Despesa"
    }
}

/** Toque no item corrige; lixeira apaga só ele (com Desfazer). */
@Composable
fun LinhaLancamento(l: Lancamento, aoEditar: (Lancamento) -> Unit, aoApagar: (Lancamento) -> Unit) {
    val (tipo, fundo, cor) = when (l.tipo) {
        Tipo.FRETE -> Triple("Frete", Cores.VerdeClaro, Cores.Verde)
        Tipo.ABASTECIMENTO -> Triple("Combustível", Cores.CombustivelClaro, Cores.Combustivel)
        Tipo.DESPESA -> Triple("Despesa", androidx.compose.ui.graphics.Color(0xFFE7E3D8), Cores.Tinta)
    }
    val detalhe = when (l.tipo) {
        Tipo.FRETE -> listOfNotNull(l.kmCentesimos?.let { "${numero(it)} km" }, l.minutos?.let { "$it min" }).joinToString(" · ")
        Tipo.ABASTECIMENTO -> "${numero(l.litrosCentesimos ?: 0)} L · ${Formato.reais(l.precoLitro ?: 0)}/L"
        Tipo.DESPESA -> if (l.ocorrencia != null) "mensal" else ""
    }
    val valor = (if (l.tipo == Tipo.FRETE) "" else "− ") + Formato.reais(l.valor)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).clickable(onClickLabel = "Corrigir", role = Role.Button) { aoEditar(l) }.padding(vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Selo(tipo, fundo, cor)
                if (detalhe.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    TextoApagado(detalhe)
                }
            }
            Text(descricaoDe(l), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
        BotaoIcone(R.drawable.ic_lixeira, "Apagar ${descricaoDe(l)} de ${Formato.reais(l.valor)}", { aoApagar(l) })
    }
}
