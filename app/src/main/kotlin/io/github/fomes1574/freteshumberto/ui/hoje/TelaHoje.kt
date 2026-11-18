package io.github.fomes1574.freteshumberto.ui.hoje

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.CATEGORIA_PEDAGIO
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Datas
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Intervalo
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.Relatorio
import io.github.fomes1574.freteshumberto.core.Resumo
import io.github.fomes1574.freteshumberto.core.Tipo
import io.github.fomes1574.freteshumberto.core.textoMeta
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoIcone
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Kpi
import io.github.fomes1574.freteshumberto.ui.comum.Par
import io.github.fomes1574.freteshumberto.ui.comum.Rotulo
import io.github.fomes1574.freteshumberto.ui.comum.Selo
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloCartao
import io.github.fomes1574.freteshumberto.ui.comum.TituloTela
import io.github.fomes1574.freteshumberto.ui.estrada.CenaEstrada
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate

/** Ações da tela Hoje e da lista de lançamentos. */
class AcoesHoje(
    val novo: (Tipo, LocalDate, String?) -> Unit,
    val calcular: () -> Unit,
    val editar: (Lancamento) -> Unit,
    val apagar: (Lancamento) -> Unit,
    val verMes: () -> Unit,
    val irCopia: () -> Unit,
    val verDia: () -> Unit,
)

@Composable
fun TelaHoje(e: Estado, hoje: LocalDate, posicoes: MutableMap<String, Float>, lembrarCopia: Boolean, margens: PaddingValues, acoes: AcoesHoje) {
    val r = remember(e, hoje) { Relatorio.resumir(e.lancamentos, Intervalo(hoje, hoje), e.metas) }
    val meta = e.metas.doDia(hoje)
    val mes = remember(e, hoje) { Relatorio.resumir(e.lancamentos, Intervalo(Datas.mes(hoje).inicio, hoje), e.metas) }
    val doDia = e.lancamentos.filter { it.data == hoje }.asReversed()

    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TituloTela("Meus Fretes", DataTexto.longa(hoje))
        ContaDoPeriodo(r)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Meta ${Formato.reais(meta)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (r.lucro < meta) "Faltam ${Formato.reais(meta - r.lucro)}" else textoMeta(r.lucro, meta),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                    color = Cores.Verde, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                )
            }
            CenaEstrada(r.lucro, meta, "dia", posicoes)
        }
        Botao("Anotar frete", { acoes.novo(Tipo.FRETE, hoje, null) }, icone = R.drawable.ic_mais, alto = true)
        AcoesGastos(hoje, acoes.novo)
        BotaoLinha("Calcular frete", acoes.calcular, icone = R.drawable.ic_calculadora)

        Cartao {
            TituloCartao("Últimos lançamentos")
            if (doDia.isEmpty()) TextoApagado("Nada anotado ainda hoje.")
            ListaLancamentos(doDia.take(3), acoes.editar, acoes.apagar)
            if (doDia.size > 3) BotaoLinha("Ver todos", acoes.verDia)
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

/** O mesmo quadro financeiro na tela Hoje e nos relatórios. */
@Composable
fun ContaDoPeriodo(r: Resumo, aoRodape: @Composable () -> Unit = {}) {
    Cartao(cor = Cores.FundoTotal) {
        Text("TOTAL DO TRABALHO", color = Cores.TextoTotal, style = MaterialTheme.typography.labelLarge)
        Text(Formato.reais(r.lucro), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        LinhaTotal("Valor ganho", r.fretes, Cores.GanhoNoTotal)
        LinhaTotal("Valor gasto", r.gastos, Cores.GastoNoTotal, gasto = true)
        LinhaTotal("Combustível", r.combustivel, Cores.GastoNoTotal, gasto = true, informado = r.qtdAbastecimentos > 0)
        LinhaTotal("Pedágio", r.pedagio, Cores.GastoNoTotal, gasto = true, informado = CATEGORIA_PEDAGIO in r.porCategoria)
        val outros = r.outras - r.pedagio
        if (outros > 0) LinhaTotal("Outros gastos", outros, Cores.GastoNoTotal, gasto = true)
        aoRodape()
    }
}

@Composable
private fun LinhaTotal(rotulo: String, valor: Long, cor: Color, gasto: Boolean = false, informado: Boolean = true) {
    HorizontalDivider(color = Cores.LinhaTotal)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, color = Cores.TextoTotal, modifier = Modifier.weight(1f))
        val exibido = if (informado) (if (gasto && valor > 0) "− " else "") + Formato.reais(valor) else "Sem registro"
        Text(exibido, color = cor, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun AcoesGastos(dia: LocalDate, novo: (Tipo, LocalDate, String?) -> Unit) {
    val combustivel = "Combustível"
    val pedagio = "Pedágio"
    val margem = 12.dp
    val espacoInterno = PaddingValues(horizontal = margem, vertical = 8.dp)
    val botaoCombustivel: @Composable (Modifier) -> Unit = { m ->
        Botao(combustivel, { novo(Tipo.ABASTECIMENTO, dia, null) }, m, R.drawable.ic_combustivel, Cores.Cartao, Cores.Negativo, espacoInterno = espacoInterno)
    }
    val botaoPedagio: @Composable (Modifier) -> Unit = { m ->
        Botao(pedagio, { novo(Tipo.DESPESA, dia, CATEGORIA_PEDAGIO) }, m, R.drawable.ic_despesa, Cores.Cartao, Cores.Negativo, espacoInterno = espacoInterno)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val larguraBotao = (maxWidth - 10.dp) / 2
        val estilo = MaterialTheme.typography.labelLarge
        val medidor = rememberTextMeasurer()
        val larguraTexto = maxOf(
            medidor.measure(AnnotatedString(combustivel), estilo, softWrap = false).size.width,
            medidor.measure(AnnotatedString(pedagio), estilo, softWrap = false).size.width,
        )
        // Espaço para as margens internas do botão, o ícone e a distância até o texto.
        val disponivel = with(LocalDensity.current) { (larguraBotao - margem * 2 - 24.dp - 10.dp).roundToPx() }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (larguraTexto <= disponivel) {
                Par(botaoCombustivel, botaoPedagio)
            } else {
                botaoCombustivel(Modifier)
                botaoPedagio(Modifier)
            }
            BotaoLinha("Outro gasto", { novo(Tipo.DESPESA, dia, null) })
        }
    }
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
        Tipo.ABASTECIMENTO -> l.litrosCentesimos?.let { "${numero(it)} L · ${Formato.reais(l.precoLitro!!)}/L" }.orEmpty()
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
        Text(valor, color = if (l.tipo == Tipo.FRETE) Cores.Verde else Cores.Negativo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
        BotaoIcone(R.drawable.ic_lixeira, "Apagar ${descricaoDe(l)} de ${Formato.reais(l.valor)}", { aoApagar(l) })
    }
}
