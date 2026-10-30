package io.github.fomes1574.freteshumberto.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.fomes1574.freteshumberto.FretesApplication
import io.github.fomes1574.freteshumberto.MainActivity
import io.github.fomes1574.freteshumberto.core.Datas
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.data.TamanhoLetra
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fluxos principais na tela de verdade (Compose + banco Room), rodando na JVM com Robolectric.
 * Não substitui o teste no celular: não mede toque, teclado, TalkBack nem desempenho.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class FluxosTest {
    val tela = createAndroidComposeRule<MainActivity>()

    /** Apaga banco e preferências ANTES de abrir a tela: os arquivos sobrevivem entre testes. */
    private val arquivosLimpos = object : ExternalResource() {
        override fun before() {
            val c = ApplicationProvider.getApplicationContext<android.content.Context>()
            c.deleteDatabase(io.github.fomes1574.freteshumberto.data.BancoFretes.NOME)
            java.io.File(c.filesDir, "datastore").deleteRecursively()
            java.io.File(c.filesDir, "copias").deleteRecursively()
        }
    }

    @get:Rule val regras: TestRule = RuleChain.outerRule(arquivosLimpos).around(tela)

    @Before fun limpar() {
        val app = ApplicationProvider.getApplicationContext<FretesApplication>()
        runBlocking {
            app.repositorio.substituirTudo(Estado())
            app.preferencias.mudarLetra(TamanhoLetra.NORMAL)
        }
        esperar("R$ 0,00")
    }

    private fun existe(texto: String, parte: Boolean = false) = tela.onAllNodesWithText(texto, substring = parte).fetchSemanticsNodes().isNotEmpty()

    /** Textos na tela agora, para a mensagem de erro dizer o que apareceu no lugar. */
    private fun textosNaTela(): String = tela.onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { raiz ->
        fun coletar(n: androidx.compose.ui.semantics.SemanticsNode): List<String> =
            (n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) + n.children.flatMap { coletar(it) }
        coletar(raiz)
    }.joinToString(" | ")

    private fun aguardar(descricao: String, condicao: () -> Boolean) {
        try {
            tela.waitUntil(15_000) { condicao() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Esperava $descricao. Na tela: ${textosNaTela()}", e)
        }
    }

    private fun esperar(texto: String, parte: Boolean = false) = aguardar("ver \"$texto\"") { existe(texto, parte) }

    private fun esperarSumir(texto: String) = aguardar("sumir \"$texto\"") { !existe(texto) }

    /** Clique pela ação de acessibilidade: não depende de posição nem de aviso por cima. */
    private fun SemanticsNodeInteraction.clicar() = performSemanticsAction(SemanticsActions.OnClick)

    private fun campo(i: Int) = tela.onAllNodes(hasSetTextAction())[i]

    private fun tocar(texto: String) = tela.onNodeWithText(texto).performScrollTo().clicar()

    private fun aba(nome: String) = tela.onAllNodesWithText(nome)[0].clicar()

    /** Troca o valor de um campo (apaga o que tinha e digita o novo). */
    private fun trocar(i: Int, texto: String) {
        campo(i).performTextClearance()
        campo(i).performTextInput(texto)
    }

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
        tela.onNodeWithContentDescription("Apagar Frete de R$ 80,00").performScrollTo().clicar()
        esperar("-R$ 10,00")
        esperar("Desfazer")
        tela.onNodeWithText("Desfazer").clicar()
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
        tela.onNodeWithText("Histórico").clicar()
        esperar("Ainda não há lançamentos.")
        tela.onNodeWithText("Relatórios").clicar()
        esperar("LUCRO DA SEMANA")
        tela.onNodeWithText("Ajustes").clicar()
        esperar("Tamanho da letra")
        tocar("A++")
        tela.onAllNodesWithText("Hoje")[0].clicar()
        esperar("Anotar frete")
    }

    @Test fun `conta do mes cadastrada hoje e lancada uma vez`() {
        esperar("Anotar frete")
        tela.onNodeWithText("Histórico").clicar()
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

    @Test fun `meta nova aparece na hora em todas as telas e continua depois de reabrir`() {
        anotar("Anotar frete", "280")
        esperar("Meta atingida · R$ 30,00 acima")
        aba("Ajustes")
        esperar("Salvar ajustes")
        trocar(0, "300")
        tocar("Salvar ajustes")
        esperar("Ajustes salvos.")
        aba("Hoje")
        esperar("Meta R$ 300,00")
        esperar("Faltam R$ 20,00 para a meta")
        aba("Ajustes")
        esperar("300,00")
        aba("Relatórios")
        tela.onNodeWithText("Dia").clicar()
        esperar("Faltam R$ 20,00 para a meta")
        aba("Hoje")
        esperar("Meta R$ 300,00")
        // Gravado no banco: ao fechar e abrir o app, continua R$ 300.
        val app = ApplicationProvider.getApplicationContext<FretesApplication>()
        val salvo = runBlocking { app.repositorio.carregar().metas.doDia(app.repositorio.hoje()) }
        org.junit.Assert.assertEquals(30000L, salvo)
    }

    @Test fun `consumo e preco salvos chegam na calculadora`() {
        aba("Ajustes")
        esperar("Salvar ajustes")
        trocar(1, "10")
        trocar(2, "5")
        tocar("Salvar ajustes")
        esperar("Ajustes salvos.")
        aba("Hoje")
        tocar("Calcular um frete")
        esperar("Limpar cálculo")
        esperar("10,00")
        esperar("5,00")
        tocar("Fechar")
    }

    @Test fun `abastecimento por litros calcula o preco e vira a referencia`() {
        esperar("Abasteci")
        tocar("Abasteci")
        esperar("Salvar")
        campo(0).performTextInput("200")
        tocar("quantos litros")
        trocar(1, "54,05")
        esperar("R$ 3,70")
        tocar("Salvar")
        esperarSumir("Salvar")
        esperar("54,05 L · R$ 3,70/L")
        esperar("− R$ 200,00")
        aba("Ajustes")
        esperar("3,70")
    }

    @Test fun `corrigir um lancamento troca o valor em todas as contas`() {
        anotar("Anotar frete", "100")
        esperar("R$ 100,00")
        tela.onAllNodes(hasText("Frete") and hasClickAction())[0].clicar()
        esperar("Corrigir lançamento")
        trocar(0, "150")
        tocar("Salvar")
        esperarSumir("Corrigir lançamento")
        esperar("R$ 150,00")
        esperarSumir("R$ 100,00")
    }

    @Test fun `conta que ainda nao venceu aparece e pausar e pular mudam na hora`() {
        val amanha = Datas.hoje(java.time.Clock.systemUTC()).plusDays(1)
        aba("Histórico")
        esperar("Contas do mês")
        tocar("Contas do mês")
        tocar("Cadastrar despesa mensal")
        esperar("Salvar conta mensal")
        campo(0).performTextInput("Seguro da Fiorino")
        campo(1).performTextInput("120")
        trocar(2, amanha.dayOfMonth.toString())
        tocar("Salvar conta mensal")
        esperarSumir("Salvar conta mensal")
        esperar("Seguro da Fiorino")
        esperar("Prevista", parte = true)
        tocar("Pausar repetição")
        esperar("Retomar repetição")
        esperar("Repetição pausada", parte = true)
        tocar("Retomar repetição")
        esperar("Pausar repetição")
        tocar("Pular este mês")
        esperar("Retomar este mês")
        esperar("Pulada neste mês", parte = true)
    }

    @Test fun `relatorio mostra o que foi anotado`() {
        anotar("Anotar frete", "300")
        anotar("Outra despesa", "50")
        aba("Relatórios")
        esperar("LUCRO DA SEMANA")
        esperar("R$ 250,00")
        esperar("R$ 300,00")
        tela.onNodeWithText("Mês").clicar()
        esperar("LUCRO DO MÊS")
        esperar("R$ 250,00")
        esperarSumir("Meta do mês")
    }
}
