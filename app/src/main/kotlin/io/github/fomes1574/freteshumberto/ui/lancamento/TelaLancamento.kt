@file:OptIn(ExperimentalLayoutApi::class)

package io.github.fomes1574.freteshumberto.ui.lancamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.core.CATEGORIAS
import io.github.fomes1574.freteshumberto.core.Combustivel
import io.github.fomes1574.freteshumberto.core.Dinheiro
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.ModoAbastecimento
import io.github.fomes1574.freteshumberto.core.Tipo
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.CampoData
import io.github.fomes1574.freteshumberto.ui.comum.CampoNumero
import io.github.fomes1574.freteshumberto.ui.comum.CampoTexto
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Erro
import io.github.fomes1574.freteshumberto.ui.comum.Escolha
import io.github.fomes1574.freteshumberto.ui.comum.Segmentado
import io.github.fomes1574.freteshumberto.ui.comum.TelaSobreposta
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.LocalDate

/** O que abrir no formulário: novo lançamento (com data sugerida) ou correção. */
data class Formulario(val tipo: Tipo, val existente: Lancamento?, val data: LocalDate)

/**
 * Anotar frete, Abasteci, Outra despesa e Corrigir lançamento (ARQUITETURA §3.2–3.3).
 * [aoSalvar] recebe o lançamento pronto e se é "Salvar e anotar outro"; o formulário só
 * fecha (ou limpa) depois que a gravação der certo.
 */
@Composable
fun TelaLancamento(
    f: Formulario,
    hoje: LocalDate,
    precoReferencia: Long,
    ocupado: Boolean,
    novoId: () -> String,
    agora: () -> String,
    aoSalvar: (Lancamento, outro: Boolean, limpar: () -> Unit) -> Unit,
    aoFechar: () -> Unit,
) {
    val l = f.existente
    val tipo = f.tipo
    var valor by rememberSaveable { mutableStateOf(l?.let { Formato.paraCampo(it.valor) } ?: "") }
    var descricao by rememberSaveable { mutableStateOf(l?.descricao ?: "") }
    var km by rememberSaveable { mutableStateOf(l?.kmCentesimos?.let { Formato.paraCampo(it) } ?: "") }
    var minutos by rememberSaveable { mutableStateOf(l?.minutos?.toString() ?: "") }
    var detalhes by rememberSaveable { mutableStateOf(l?.kmCentesimos != null || l?.minutos != null) }
    var data by rememberSaveable { mutableStateOf((l?.data ?: f.data).toString()) }
    var modo by rememberSaveable { mutableStateOf(l?.modoAbastecimento ?: ModoAbastecimento.PRECO) }
    var auxiliar by rememberSaveable {
        mutableStateOf(
            if (l != null && l.modoAbastecimento == ModoAbastecimento.LITROS) Formato.paraCampo(l.litrosCentesimos ?: 0)
            else Formato.paraCampo(l?.precoLitro ?: precoReferencia),
        )
    }
    var categoria by rememberSaveable { mutableStateOf(l?.categoria ?: "Pedágio") }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }

    val titulo = when {
        l != null -> "Corrigir lançamento"
        tipo == Tipo.FRETE -> "Anotar frete"
        tipo == Tipo.ABASTECIMENTO -> "Abasteci"
        else -> "Outra despesa"
    }
    val calculo = if (tipo == Tipo.ABASTECIMENTO) {
        val v = Dinheiro.parse(valor); val a = Dinheiro.parse(auxiliar)
        if (v != null && a != null) Combustivel.calcular(v, a, modo) else null
    } else null

    fun montar(): Lancamento? {
        val v = Dinheiro.parse(valor)
        if (v == null || v <= 0) { erro = "Confira o valor. Ex.: 1.234,56 ou 85,50."; return null }
        val d = LocalDate.parse(data)
        if (d.isAfter(hoje)) { erro = "Informe uma data válida, até hoje."; return null }
        var kmC: Long? = null
        var min: Int? = null
        if (tipo == Tipo.FRETE) {
            kmC = if (km.isBlank()) null else Dinheiro.parse(km)
            min = if (minutos.isBlank()) null else minutos.toIntOrNull()
            if ((km.isNotBlank() && kmC == null) || (minutos.isNotBlank() && (min == null || min < 0))) {
                erro = "Confira os km e a duração em minutos."; return null
            }
        }
        if (tipo == Tipo.ABASTECIMENTO && calculo == null) { erro = "Confira o valor e o preço do litro ou os litros."; return null }
        val instante = agora()
        return try {
            Lancamento(
                id = l?.id ?: novoId(),
                tipo = tipo,
                data = d,
                valor = v,
                descricao = descricao.trim(),
                kmCentesimos = kmC?.takeIf { it > 0 },
                minutos = min?.takeIf { it > 0 },
                litrosCentesimos = calculo?.litrosCentesimos,
                precoLitro = calculo?.precoLitro,
                modoAbastecimento = if (tipo == Tipo.ABASTECIMENTO) modo else null,
                categoria = if (tipo == Tipo.DESPESA) categoria else null,
                ocorrencia = l?.ocorrencia,
                criadoEm = l?.criadoEm ?: instante,
                editadoEm = if (l != null) instante else null,
            )
        } catch (e: IllegalArgumentException) {
            erro = "Confira os valores digitados."; null
        }
    }

    fun salvar(outro: Boolean) {
        if (ocupado) return
        erro = null
        val novo = montar() ?: return
        aoSalvar(novo, outro) {
            valor = ""; descricao = ""; km = ""; minutos = ""
        }
    }

    TelaSobreposta(titulo, aoFechar) {
        CampoNumero(
            when (tipo) {
                Tipo.FRETE -> "Quanto ficou para você"
                Tipo.ABASTECIMENTO -> "Quanto pagou no posto"
                Tipo.DESPESA -> "Valor"
            },
            valor, { valor = it; erro = null }, grande = true, dica = "0,00",
        )
        if (tipo == Tipo.FRETE) TextoApagado("O valor que ficou para você, já sem a taxa do aplicativo.")

        if (tipo == Tipo.ABASTECIMENTO) {
            Text("Você sabe…", style = MaterialTheme.typography.titleMedium)
            Segmentado(listOf("o preço do litro", "quantos litros"), if (modo == ModoAbastecimento.PRECO) 0 else 1) { i ->
                val novoModo = if (i == 0) ModoAbastecimento.PRECO else ModoAbastecimento.LITROS
                if (novoModo != modo) {
                    // Leva junto o número já calculado.
                    calculo?.let { auxiliar = Formato.paraCampo(if (novoModo == ModoAbastecimento.PRECO) it.precoLitro else it.litrosCentesimos) }
                    modo = novoModo
                }
            }
            if (modo == ModoAbastecimento.PRECO) {
                CampoNumero("Preço do litro", auxiliar, { auxiliar = it; erro = null })
            } else {
                CampoNumero("Litros que entraram no tanque", auxiliar, { auxiliar = it; erro = null }, prefixo = "L")
            }
            Cartao(cor = Cores.CombustivelClaro) {
                when {
                    calculo == null -> TextoApagado("Informe o valor pago e o preço ou os litros.")
                    modo == ModoAbastecimento.PRECO -> {
                        TextoApagado("Entraram no tanque")
                        Text("${Formato.centesimos(calculo.litrosCentesimos)} litros", style = MaterialTheme.typography.headlineMedium)
                    }
                    else -> {
                        TextoApagado("Você pagou por litro")
                        Text(Formato.reais(calculo.precoLitro), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        if (tipo == Tipo.DESPESA) {
            Text("Tipo", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val opcoes = if (categoria in CATEGORIAS) CATEGORIAS else CATEGORIAS + categoria
                opcoes.forEach { c -> Escolha(c, c == categoria, { categoria = c }) }
            }
        }

        CampoTexto(if (tipo == Tipo.FRETE) "Cliente ou descrição" else "Observação", descricao, { descricao = it }, opcional = true)

        if (tipo == Tipo.FRETE) {
            if (!detalhes) {
                TextButton(onClick = { detalhes = true }) {
                    Text("Mais detalhes: km e duração", style = MaterialTheme.typography.titleMedium, color = Cores.Azul)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero("Km rodados", km, { km = it; erro = null }, prefixo = null)
                    CampoNumero("Duração (minutos)", minutos, { minutos = it; erro = null }, prefixo = null, inteiro = true, dica = "Ex.: 90")
                    TextoApagado("Só para consulta. A conta por km e por hora fica na calculadora.")
                }
            }
        }

        CampoData("Data", LocalDate.parse(data), { it?.let { d -> data = d.toString() } }, hoje)

        Erro(erro)
        Botao("Salvar", { salvar(false) }, ligado = !ocupado, alto = true)
        if (tipo == Tipo.FRETE && l == null) BotaoLinha("Salvar e anotar outro", { salvar(true) }, ligado = !ocupado)
        if (l?.ocorrencia != null) TextoApagado("Esta despesa veio de uma conta do mês. Corrigir aqui muda só este mês.")
    }
}
