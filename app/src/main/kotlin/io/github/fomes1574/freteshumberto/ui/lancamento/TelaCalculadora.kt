package io.github.fomes1574.freteshumberto.ui.lancamento

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.fomes1574.freteshumberto.core.Calculadora
import io.github.fomes1574.freteshumberto.core.Dinheiro
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.CampoNumero
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Kpi
import io.github.fomes1574.freteshumberto.ui.comum.LinhaConta
import io.github.fomes1574.freteshumberto.ui.comum.Par
import io.github.fomes1574.freteshumberto.ui.comum.TelaSobreposta
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.hoje.numero

/**
 * Calculadora isolada (R05). O estado é só desta tela: simular, limpar ou fechar não grava
 * frete, despesa, meta nem preço de referência. Não existe "Fiz este frete, salvar".
 */
@Composable
fun TelaCalculadora(kmPorLitro: Long, precoReferencia: Long, aoFechar: () -> Unit) {
    var valor by rememberSaveable { mutableStateOf("") }
    var km by rememberSaveable { mutableStateOf("") }
    var minutos by rememberSaveable { mutableStateOf("") }
    var outros by rememberSaveable { mutableStateOf("") }
    var consumo by rememberSaveable { mutableStateOf(Formato.paraCampo(kmPorLitro)) }
    var preco by rememberSaveable { mutableStateOf(Formato.paraCampo(precoReferencia)) }

    val sim = Calculadora.simular(
        valor = Dinheiro.parse(valor) ?: 0,
        kmCentesimos = Dinheiro.parse(km) ?: 0,
        kmPorLitroCentesimos = Dinheiro.parse(consumo) ?: 0,
        precoLitro = Dinheiro.parse(preco) ?: 0,
        minutos = minutos.toIntOrNull() ?: 0,
        outrosCustos = if (outros.isBlank()) 0 else Dinheiro.parse(outros) ?: -1,
    )

    TelaSobreposta("Calcular um frete", aoFechar) {
        TextoApagado("Esta conta não altera os lançamentos do dia.")
        CampoNumero("Quanto fica para você", valor, { valor = it }, grande = true, dica = "0,00")
        CampoNumero("Km totais", km, { km = it }, prefixo = null)
        TextoApagado("Inclua coleta e retorno, se houver.")
        CampoNumero("Duração (minutos)", minutos, { minutos = it }, prefixo = null, inteiro = true, dica = "Opcional")
        CampoNumero("Pedágio e outros custos", outros, { outros = it }, dica = "0,00")
        Par(
            { m -> CampoNumero("Km por litro", consumo, { consumo = it }, m, prefixo = null) },
            { m -> CampoNumero("Preço do litro", preco, { preco = it }, m) },
        )
        TextoApagado("Mudar consumo ou preço aqui vale só para esta conta.")

        Cartao {
            if (sim == null) {
                TextoApagado("Preencha valor, km, consumo e preço para ver a conta.")
            } else {
                LinhaConta("Valor do frete", Formato.reais(sim.valor))
                LinhaConta("Combustível", "− ${Formato.reais(sim.combustivel)}", "(${numero(sim.litrosCentesimos)} L)")
                if ((Dinheiro.parse(outros) ?: 0) > 0) LinhaConta("Outros custos", "− ${Formato.reais(Dinheiro.parse(outros) ?: 0)}")
                LinhaConta("Sobra", Formato.reais(sim.sobra), total = true, negativo = sim.sobra < 0)
                Par(
                    { m -> Kpi(Formato.reais(sim.porKm), "por km", m, negativo = sim.porKm < 0) },
                    { m -> if (sim.porHora != null) Kpi(Formato.reais(sim.porHora!!), "por hora", m, negativo = sim.porHora!! < 0) },
                )
            }
        }
        BotaoLinha("Limpar cálculo", { valor = ""; km = ""; minutos = ""; outros = "" })
        Botao("Fechar", aoFechar)
    }
}
