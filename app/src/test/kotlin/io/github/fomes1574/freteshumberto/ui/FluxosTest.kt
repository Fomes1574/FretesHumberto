package io.github.fomes1574.freteshumberto.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.fomes1574.freteshumberto.FretesApplication
import io.github.fomes1574.freteshumberto.MainActivity
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.data.TamanhoLetra
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fluxos principais na tela de verdade (Compose + banco Room), rodando na JVM com Robolectric.
 * Não substitui o teste no celular: não mede toque, teclado, TalkBack nem desempenho.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FluxosTest {
    @get:Rule val tela = createAndroidComposeRule<MainActivity>()

    /** Cada teste começa com o banco vazio e letra normal (o arquivo do banco sobrevive entre testes). */
    @Before fun limpar() {
        val app = ApplicationProvider.getApplicationContext<FretesApplication>()
        runBlocking {
            app.repositorio.substituirTudo(Estado())
            app.preferencias.mudarLetra(TamanhoLetra.NORMAL)
        }
        esperar("R$ 0,00")
    }

    private fun existe(texto: String, parte: Boolean = false) = tela.onAllNodesWithText(texto, substring = parte).fetchSemanticsNodes().isNotEmpty()

    private fun esperar(texto: String, parte: Boolean = false) = tela.waitUntil(15_000) { existe(texto, parte) }

    private fun esperarSumir(texto: String) = tela.waitUntil(15_000) { !existe(texto) }

    private fun campo(i: Int) = tela.onAllNodes(hasSetTextAction())[i]

    private fun tocar(texto: String) = tela.onNodeWithText(texto).performScrollTo().performClick()

    /** Abre o formulário pelo botão da tela Hoje, digita o valor e salva. */
    private fun anotar(botao: String, valor: String) {
        esperar(botao)
        tocar(botao)
        esperar("Salvar")
        campo(0).performTextInput(valor)
        tocar("Salvar")
        esperarSumir("Salvar")
    }

    @Test fun `fretes menos combustivel menos despesas da o lucro do dia`() {
        anotar("Anotar frete", "285")
        anotar("Abasteci", "40")
        anotar("Outra despesa", "20")
        esperar("R$ 225,00")
        esperar("Faltam R$ 25,00 para a meta")
    }

    @Test fun `apagar frete deixa a despesa e desfazer traz de volta`() {
        anotar("Anotar frete", "80")
        anotar("Outra despesa", "10")
        esperar("R$ 70,00")
        tela.onNodeWithContentDescription("Apagar Frete de R$ 80,00").performScrollTo().performClick()
        esperar("-R$ 10,00")
        esperar("Desfazer")
        tela.onNodeWithText("Desfazer").performClick()
        esperar("R$ 70,00")
    }

    @Test fun `valor colado com milhar e salvo sem mudar`() {
        anotar("Anotar frete", "R$ 1.234,56")
        esperar("R$ 1.234,56")
    }

    @Test fun `calculadora mostra a conta e nao grava nada`() {
        esperar("Calcular um frete")
        tocar("Calcular um frete")
        esperar("Limpar cálculo")
        campo(0).performTextInput("80")
        campo(1).performTextInput("40")
        campo(2).performTextInput("90")
        campo(4).performTextClearance(); campo(4).performTextInput("8")
        campo(5).performTextClearance(); campo(5).performTextInput("4")
        esperar("R$ 60,00")
        esperar("R$ 1,50")
        esperar("R$ 40,00")
        tocar("Fechar")
        esperarSumir("Limpar cálculo")
        esperar("Nada anotado ainda hoje.")
    }

    @Test fun `abas abrem e letra maior funciona`() {
        esperar("Anotar frete")
        tela.onNodeWithText("Histórico").performClick()
        esperar("Ainda não há lançamentos.")
        tela.onNodeWithText("Relatórios").performClick()
        esperar("LUCRO DA SEMANA")
        tela.onNodeWithText("Ajustes").performClick()
        esperar("Tamanho da letra")
        tocar("A++")
        tela.onAllNodesWithText("Hoje")[0].performClick()
        esperar("Anotar frete")
    }

    @Test fun `conta do mes cadastrada hoje e lancada uma vez`() {
        esperar("Anotar frete")
        tela.onNodeWithText("Histórico").performClick()
        esperar("Contas do mês")
        tocar("Contas do mês")
        esperar("Cadastrar despesa mensal")
        tocar("Cadastrar despesa mensal")
        esperar("Salvar conta mensal")
        campo(0).performTextInput("Aluguel da vaga")
        campo(1).performTextInput("300")
        tocar("Salvar conta mensal")
        esperarSumir("Salvar conta mensal")
        esperar("Aluguel da vaga")
        esperar("Lançada", parte = true)
    }
}
