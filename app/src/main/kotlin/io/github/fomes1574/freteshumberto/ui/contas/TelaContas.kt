package io.github.fomes1574.freteshumberto.ui.contas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.ContasDoMes
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Dinheiro
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.SituacaoConta
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoIcone
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.CampoData
import io.github.fomes1574.freteshumberto.ui.comum.CampoNumero
import io.github.fomes1574.freteshumberto.ui.comum.CampoTexto
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Erro
import io.github.fomes1574.freteshumberto.ui.comum.Escolha
import io.github.fomes1574.freteshumberto.ui.comum.TelaSobreposta
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloCartao
import io.github.fomes1574.freteshumberto.ui.comum.Valor
import java.time.LocalDate
import java.time.YearMonth

class AcoesContas(
    val mudarMes: (YearMonth) -> Unit,
    val nova: () -> Unit,
    val editar: (SituacaoConta) -> Unit,
    val pular: (SituacaoConta) -> Unit,
    val pausar: (SituacaoConta) -> Unit,
    val verLancamento: (String) -> Unit,
    val fechar: () -> Unit,
)

/**
 * Contas do mês (R08): abre pelo Histórico ou pelos Ajustes, sem quinta aba. São registros
 * locais, não pagamentos. Cada despesa lançada é comum e independente de qualquer frete.
 */
@Composable
fun TelaContas(e: Estado, mes: YearMonth, ocupado: Boolean, acoes: AcoesContas) {
    val situacoes = ContasDoMes.situacoes(e, mes)
    TelaSobreposta("Contas do mês", acoes.fechar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotaoIcone(R.drawable.ic_esquerda, "Mês anterior", { acoes.mudarMes(mes.minusMonths(1)) })
            Text(DataTexto.mes(mes), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            BotaoIcone(R.drawable.ic_direita, "Mês seguinte", { acoes.mudarMes(mes.plusMonths(1)) })
        }
        Botao("Cadastrar despesa mensal", acoes.nova, icone = R.drawable.ic_mais)
        if (situacoes.isEmpty()) Cartao { TextoApagado("Nenhuma conta para este mês.") }
        situacoes.forEach { s ->
            Cartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        TituloCartao(s.descricao)
                        TextoApagado("${DataTexto.diaMes(s.data)} · ${s.texto}")
                    }
                    Valor(s.valor)
                }
                BotaoLinha("Editar conta", { acoes.editar(s) }, ligado = !ocupado)
                BotaoLinha(if (s.pulada) "Retomar este mês" else "Pular este mês", { acoes.pular(s) }, ligado = !ocupado)
                BotaoLinha(if (s.ativa) "Pausar repetição" else "Retomar repetição", { acoes.pausar(s) }, ligado = !ocupado)
                s.lancamentoId?.let { id -> BotaoLinha("Ver lançamento", { acoes.verLancamento(id) }) }
            }
        }
        TextoApagado("As contas são lançadas sozinhas quando a data chega, mesmo se o app ficar fechado. Não há pagamento bancário.")
    }
}

/**
 * Cadastrar ou editar conta. Ao editar, escolhe "Só este mês" (exceção) ou
 * "Deste mês em diante" (nova versão; meses anteriores ficam como estavam).
 */
@Composable
fun TelaFormConta(
    situacao: SituacaoConta?,
    mes: YearMonth,
    hoje: LocalDate,
    ocupado: Boolean,
    aoCadastrar: (nome: String, valor: Long, dia: Int, inicio: LocalDate, fim: LocalDate?) -> Unit,
    aoEditar: (soEsteMes: Boolean, nome: String, valor: Long, dia: Int, fim: LocalDate?) -> Unit,
    aoFechar: () -> Unit,
) {
    val v = situacao?.versao
    var nome by rememberSaveable { mutableStateOf(situacao?.descricao ?: "") }
    var valor by rememberSaveable { mutableStateOf(situacao?.let { Formato.paraCampo(it.valor) } ?: "") }
    var dia by rememberSaveable { mutableStateOf((v?.dia ?: hoje.dayOfMonth).toString()) }
    var inicio by rememberSaveable { mutableStateOf((situacao?.conta?.inicio ?: hoje).toString()) }
    var fim by rememberSaveable { mutableStateOf(v?.fim?.toString() ?: "") }
    var soEsteMes by rememberSaveable { mutableStateOf(true) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }

    TelaSobreposta(if (situacao != null) "Editar conta mensal" else "Despesa mensal", aoFechar) {
        CampoTexto("Nome da despesa", nome, { nome = it; erro = null })
        CampoNumero("Valor mensal", valor, { valor = it; erro = null }, grande = true, dica = "0,00")
        CampoNumero("Dia do mês", dia, { dia = it.take(2); erro = null }, prefixo = null, inteiro = true)
        TextoApagado("O lançamento é automático na data. Dia 31 vira 30 nos meses de 30 dias; fevereiro usa o dia 28.")
        if (situacao == null) CampoData("Data inicial", LocalDate.parse(inicio), { it?.let { d -> inicio = d.toString() } }, hoje, maximo = null)
        CampoData("Última data (opcional)", fim.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }, { fim = it?.toString() ?: "" }, hoje, maximo = null, opcional = true)
        if (situacao != null) {
            Text("Aplicar a alteração", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Escolha("Só em ${DataTexto.mes(mes)}", soEsteMes, { soEsteMes = true }, Modifier.fillMaxWidth())
                Escolha("De ${DataTexto.mes(mes)} em diante", !soEsteMes, { soEsteMes = false }, Modifier.fillMaxWidth())
            }
            if (soEsteMes) TextoApagado("Só este mês muda nome, valor e dia. A última data vale para \"em diante\".")
        }
        Erro(erro)
        Botao("Salvar conta mensal", {
            val vv = Dinheiro.parse(valor)
            val dd = dia.toIntOrNull()
            val ini = if (situacao == null) LocalDate.parse(inicio) else situacao.conta.inicio
            val ff = fim.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
            val msg = ContasDoMes.validar(nome, vv, dd, ini, ff)
            if (msg != null) {
                erro = msg
            } else if (situacao == null) {
                aoCadastrar(nome, vv!!, dd!!, ini, ff)
            } else {
                aoEditar(soEsteMes, nome, vv!!, dd!!, ff)
            }
        }, ligado = !ocupado)
    }
}
