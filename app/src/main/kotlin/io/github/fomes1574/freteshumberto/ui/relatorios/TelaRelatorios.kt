package io.github.fomes1574.freteshumberto.ui.relatorios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.ContasDoMes
import io.github.fomes1574.freteshumberto.core.Datas
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.FiltroRelatorio
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.ModoRelatorio
import io.github.fomes1574.freteshumberto.core.Relatorio
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
import io.github.fomes1574.freteshumberto.ui.comum.Segmentado
import io.github.fomes1574.freteshumberto.ui.comum.Selo
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloCartao
import io.github.fomes1574.freteshumberto.ui.comum.TituloTela
import io.github.fomes1574.freteshumberto.ui.comum.Valor
import io.github.fomes1574.freteshumberto.ui.hoje.AcoesGastos
import io.github.fomes1574.freteshumberto.ui.hoje.ContaDoPeriodo
import io.github.fomes1574.freteshumberto.ui.hoje.ListaLancamentos
import io.github.fomes1574.freteshumberto.ui.hoje.numero
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate
import java.time.YearMonth

class AcoesRelatorio(
    val mudarFiltro: (FiltroRelatorio) -> Unit,
    val escolherPeriodo: () -> Unit,
    val escolherDoisDias: () -> Unit,
    val novo: (Tipo, LocalDate, String?) -> Unit,
    val editar: (Lancamento) -> Unit,
    val apagar: (Lancamento) -> Unit,
    val compartilhar: (String) -> Unit,
    val contas: (YearMonth) -> Unit,
)

@Composable
fun TelaRelatorios(e: Estado, f: FiltroRelatorio, hoje: LocalDate, posicoes: MutableMap<String, Float>, margens: PaddingValues, acoes: AcoesRelatorio) {
    val p = f.intervalo()
    val r = remember(e, f) { Relatorio.resumir(e.lancamentos, p, e.metas, f.selecionadas()) }
    val parcial = f.parcial(hoje)
    var indicadores by remember(f) { mutableStateOf(false) }
    var selecionado by remember(f) { mutableStateOf<LocalDate?>(null) }
    var verDias by remember(f) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TituloTela("Relatórios")
        val indice = when (f.modo) {
            ModoRelatorio.DIA -> 0
            ModoRelatorio.SEMANA -> 1
            ModoRelatorio.MES -> 2
            else -> 3
        }
        Segmentado(listOf("Dia", "Semana", "Mês", "Período"), indice) { i ->
            when (i) {
                0 -> acoes.mudarFiltro(FiltroRelatorio(ModoRelatorio.DIA, if (f.modo == ModoRelatorio.DIA) f.referencia else hoje))
                1 -> acoes.mudarFiltro(FiltroRelatorio(ModoRelatorio.SEMANA, hoje))
                2 -> acoes.mudarFiltro(FiltroRelatorio(ModoRelatorio.MES, hoje))
                else -> acoes.escolherPeriodo()
            }
        }

        if (f.modo == ModoRelatorio.PERIODO || f.modo == ModoRelatorio.DOIS_DIAS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.rotulo(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = if (f.modo == ModoRelatorio.DOIS_DIAS) acoes.escolherDoisDias else acoes.escolherPeriodo) {
                    Text("Alterar", style = MaterialTheme.typography.labelLarge, color = Cores.Azul)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotaoIcone(R.drawable.ic_esquerda, "Período anterior", { acoes.mudarFiltro(f.deslocar(-1)) })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(f.rotulo(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (parcial) Selo("Parcial", androidx.compose.ui.graphics.Color(0xFFFFF3C4), Cores.Tinta)
                }
                BotaoIcone(R.drawable.ic_direita, "Próximo período", { acoes.mudarFiltro(f.deslocar(1)) }, ligado = p.fim.isBefore(hoje))
            }
        }

        ContaDoPeriodo(r) {
            Textos.comparacao(e.lancamentos, f, r, e.metas, hoje)?.let {
                Text(it, color = Cores.TextoTotal, style = MaterialTheme.typography.bodyMedium)
            }
            if (f.modo == ModoRelatorio.DIA && r.qtdFretes > 0) {
                Text(textoMeta(r.lucro, e.metas.doDia(p.inicio)), color = Cores.TextoTotal)
            }
        }
        if (f.modo != ModoRelatorio.DOIS_DIAS) BotaoLinha("Comparar dois dias", acoes.escolherDoisDias)

        if (f.modo == ModoRelatorio.DOIS_DIAS && f.doisDias.size == 2) {
            val (a, b) = f.doisDias.sorted()
            val ra = Relatorio.resumir(e.lancamentos, Datas.periodo(io.github.fomes1574.freteshumberto.core.ModoPeriodo.DIA, a), e.metas)
            val rb = Relatorio.resumir(e.lancamentos, Datas.periodo(io.github.fomes1574.freteshumberto.core.ModoPeriodo.DIA, b), e.metas)
            Cartao {
                TituloCartao("Dois dias escolhidos")
                LinhaConta(DataTexto.curta(a) + " ${a.year}", Formato.reais(ra.lucro), negativo = ra.lucro < 0)
                LinhaConta(DataTexto.curta(b) + " ${b.year}", Formato.reais(rb.lucro), negativo = rb.lucro < 0)
                TextoApagado("Diferença entre os dias: ${Formato.reais(kotlin.math.abs(rb.lucro - ra.lucro))}. A soma inclui só esses dois dias.")
            }
        }

        if (f.modo != ModoRelatorio.DIA) {
            val dias = if (f.modo == ModoRelatorio.DOIS_DIAS) f.doisDias.sorted() else p.dias()
            Cartao {
                TituloCartao("Total por dia")
                GraficoLucro(dias, r.porDia.mapValues { it.value.lucro }, dias.map { e.metas.doDia(it) }, hoje, selecionado) { selecionado = it }
                val sel = selecionado?.takeIf { r.porDia.containsKey(it) }
                if (sel != null) {
                    Text("${DataTexto.curta(sel)}: ${Formato.reais(r.porDia.getValue(sel).lucro)}", style = MaterialTheme.typography.titleMedium)
                    BotaoLinha("Abrir este dia", { acoes.mudarFiltro(FiltroRelatorio(ModoRelatorio.DIA, sel)) })
                }
                TextButton(onClick = { verDias = !verDias }) {
                    Text(if (verDias) "Ocultar lançamentos por dia" else "Ver lançamentos por dia", style = MaterialTheme.typography.labelLarge, color = Cores.Azul)
                }
                if (verDias) {
                    if (r.porDia.isEmpty()) TextoApagado("Sem lançamentos neste período.")
                    r.porDia.keys.sorted().forEach { d ->
                        val v = r.porDia.getValue(d).lucro
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { acoes.mudarFiltro(FiltroRelatorio(ModoRelatorio.DIA, d)) }, modifier = Modifier.weight(1f)) {
                                Text("${DataTexto.curta(d)} ${d.year}", style = MaterialTheme.typography.bodyLarge, color = Cores.Tinta, modifier = Modifier.weight(1f))
                            }
                            Valor(v)
                        }
                    }
                }
            }
        }

        BotaoLinha(if (indicadores) "Ocultar indicadores" else "Metas, combustível e outros indicadores", { indicadores = !indicadores })
        if (indicadores) {
            if (f.modo != ModoRelatorio.DIA) Cartao {
                TituloCartao("Metas e média")
                Par(
                    { m -> Kpi("${r.diasMetaAtingida} de ${r.diasTrabalhados.size}", "dias trabalhados com a meta do dia atingida", m) },
                    { m -> Kpi(r.lucroMedioPorDiaTrabalhado?.let { Formato.reais(it) } ?: "—", "resultado do período por dia trabalhado", m, negativo = (r.lucroMedioPorDiaTrabalhado ?: 0) < 0) },
                )
            }
            val categorias = r.porCategoria.filterKeys { it != io.github.fomes1574.freteshumberto.core.CATEGORIA_PEDAGIO }
            if (categorias.isNotEmpty()) Cartao {
                TituloCartao("Outros gastos por categoria")
                categorias.forEach { (categoria, valor) -> LinhaConta(categoria, Formato.reais(-valor), negativo = true) }
            }
            Cartao(cor = Cores.CombustivelClaro) {
                TituloCartao("Combustível no período")
                Par(
                    { m -> Kpi(Formato.reais(r.combustivel), Textos.abastecimentos(r.qtdAbastecimentos), m, fundo = Cores.Cartao) },
                    { m -> Kpi(
                        if (r.litrosCentesimos > 0) "${numero(r.litrosCentesimos)} L" else "—",
                        r.precoMedioLitro?.let { "${Formato.reais(it)}/L nos registros com litros" } ?: "sem litros informados",
                        m, fundo = Cores.Cartao,
                    ) },
                )
                Par(
                    { m -> Kpi(r.percentualTanque?.let { "$it%" } ?: "—", "do valor ganho foi para combustível", m, fundo = Cores.Cartao) },
                    { m -> Kpi(if (r.kmCentesimos > 0) "${numero(r.kmCentesimos)} km" else "—", "anotados nos fretes", m, fundo = Cores.Cartao) },
                )
            }
        }

        if (f.modo == ModoRelatorio.DIA) {
            val dia = p.inicio
            val lista = e.lancamentos.filter { it.data == dia }
            Cartao {
                TituloCartao("Lançamentos")
                if (lista.isEmpty()) TextoApagado("Nada neste dia.")
                ListaLancamentos(lista, acoes.editar, acoes.apagar)
                if (!dia.isAfter(hoje)) {
                    Botao("Anotar frete neste dia", { acoes.novo(Tipo.FRETE, dia, null) }, icone = R.drawable.ic_mais)
                    AcoesGastos(dia, acoes.novo)
                }
            }
        }

        if (f.modo == ModoRelatorio.MES) {
            val fim = YearMonth.from(p.inicio)
            val previstas = remember(e, fim) { ContasDoMes.situacoes(e, fim).filter { it.texto == "Prevista" } }
            if (previstas.isNotEmpty()) Cartao {
                TituloCartao("Contas previstas")
                LinhaConta("Valor previsto", Formato.reais(previstas.sumOf { it.valor }))
                BotaoLinha("Ver contas do mês", { acoes.contas(fim) })
            }
            if (indicadores) {
                val meses = (5 downTo 0).map { fim.minusMonths(it.toLong()) }
                val totais = meses.map { Relatorio.resumir(e.lancamentos, Datas.mes(it), e.metas).combustivel }
                Cartao {
                    TituloCartao("Combustível nos últimos 6 meses")
                    GraficoCombustivel(meses, totais)
                    meses.forEachIndexed { i, m ->
                        LinhaConta(DataTexto.mes(m) + if (m == YearMonth.from(hoje)) " · parcial" else "", Formato.reais(totais[i]))
                    }
                }
            }
        }

        BotaoLinha("Mandar resumo pelo WhatsApp", { acoes.compartilhar(Textos.resumoParaEnviar(f, r, e.metas)) }, icone = R.drawable.ic_compartilhar)
    }
}
