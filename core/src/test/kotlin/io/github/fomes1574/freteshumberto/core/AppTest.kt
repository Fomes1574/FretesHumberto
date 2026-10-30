package io.github.fomes1574.freteshumberto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Regras usadas pelas telas, pelo banco e pela cópia de segurança do app Android.
 * Os casos de referência do protótipo continuam em RegrasTest.
 */
class AppTest {
    private var seq = 0
    private fun id() = "t-${++seq}"
    private fun d(s: String) = LocalDate.parse(s)
    private fun m(s: String) = YearMonth.parse(s)
    private fun frete(data: String, valor: Long, min: Int? = null) = Lancamento(id(), Tipo.FRETE, d(data), valor, "Frete", kmCentesimos = 4500, minutos = min, criadoEm = "c")
    private fun abast(data: String, valor: Long) = Lancamento(id(), Tipo.ABASTECIMENTO, d(data), valor, "", litrosCentesimos = 5405, precoLitro = 370, modoAbastecimento = ModoAbastecimento.PRECO, criadoEm = "c")
    private fun despesa(data: String, valor: Long) = Lancamento(id(), Tipo.DESPESA, d(data), valor, "Pedágio", categoria = "Pedágio", criadoEm = "c")

    private fun comConta(hoje: String, dia: Int = 10, inicio: String = "2026-01-01"): Estado =
        ContasDoMes.cadastrar(Estado(), "aluguel", "Aluguel da vaga", 30000, dia, d(inicio), null, d(hoje), ::id)

    // ---- Campo de valor

    @Test fun `valor colado com milhar vira campo sem mudar o valor`() {
        assertEquals("1234,56", Dinheiro.campo("R$ 1.234,56"))
        assertEquals("1234,56", Dinheiro.campo("1.234,56"))
        assertEquals("54,05", Dinheiro.campo("54.0599"))
        assertEquals("85,5", Dinheiro.campo("85.5"))
        assertEquals(123456L, Dinheiro.parse(Dinheiro.campo("R$ 1.234,56")))
    }

    // ---- Diferença gravada no banco

    @Test fun `diferenca grava so o que mudou`() {
        val f = frete("2026-10-29", 8000); val g = despesa("2026-10-29", 1000)
        val antes = Estado(Diario().salvar(f).salvar(g))
        val depois = antes.copy(diario = antes.diario.salvar(f.copy(valor = 9000)))
        val mud = Mudancas.entre(antes, depois)
        assertEquals(listOf(f.id), mud.lancamentosGravar.map { it.id })
        assertTrue(mud.lancamentosApagar.isEmpty())
        assertEquals(setOf(d("2026-10-29")), mud.diasGravar.keys)
        assertNull(mud.contas); assertNull(mud.metas); assertNull(mud.ajustes)
        assertTrue(Mudancas.entre(depois, depois).vazia)
    }

    @Test fun `apagar frete gera so a exclusao dele`() {
        val f = frete("2026-10-29", 8000); val g = despesa("2026-10-29", 1000); val a = abast("2026-10-29", 2000)
        val antes = Estado(Diario().salvar(f).salvar(g).salvar(a))
        val depois = antes.copy(diario = antes.diario.apagar(f.id).first)
        val mud = Mudancas.entre(antes, depois)
        assertEquals(listOf(f.id), mud.lancamentosApagar)
        assertTrue(mud.lancamentosGravar.isEmpty())
    }

    // ---- Contas do mês

    @Test fun `cadastrar conta gera os meses vencidos`() {
        val e = comConta("2026-03-15")
        assertEquals(3, e.lancamentos.size)
        assertTrue(e.lancamentos.all { it.categoria == CATEGORIA_CONTA && it.valor == 30000L })
        assertEquals(listOf("Lançada"), ContasDoMes.situacoes(e, m("2026-03")).map { it.texto })
        assertEquals(listOf("Prevista"), ContasDoMes.situacoes(e, m("2026-04")).map { it.texto })
    }

    @Test fun `so este mes muda um mes e preserva os outros`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.editarSoEsteMes(e, "aluguel", m("2026-02"), "Aluguel com desconto", 25000, 12, d("2026-03-15"), ::id)
        val fev = e.lancamentos.single { it.data.monthValue == 2 }
        assertEquals(25000L, fev.valor); assertEquals(d("2026-02-12"), fev.data)
        assertEquals(30000L, e.lancamentos.single { it.data.monthValue == 3 }.valor)
        e = e.gerarContas(d("2026-04-30"), ::id)
        assertEquals(30000L, e.lancamentos.single { it.data.monthValue == 4 }.valor)
        assertEquals(4, e.lancamentos.size)
    }

    @Test fun `so este mes com data futura vira previsao sem lancamento`() {
        var e = comConta("2026-03-15", dia = 10)
        e = ContasDoMes.editarSoEsteMes(e, "aluguel", m("2026-03"), "Aluguel", 30000, 25, d("2026-03-15"), ::id)
        assertEquals(2, e.lancamentos.size)
        assertEquals(StatusOcorrencia.PREVISTA, e.diario.ocorrencias["aluguel|2026-03"]!!.status)
        e = e.gerarContas(d("2026-03-25"), ::id)
        assertEquals(d("2026-03-25"), e.lancamentos.single { it.data.monthValue == 3 }.data)
    }

    @Test fun `deste mes em diante cria versao nova e preserva meses anteriores`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.editarDesteMesEmDiante(e, "aluguel", m("2026-03"), "Aluguel novo", 35000, 10, null, d("2026-03-15"), ::id)
        assertEquals(listOf(30000L, 30000L, 35000L), e.lancamentos.sortedBy { it.data }.map { it.valor })
        assertEquals(2, e.contas.single().versoes.size)
        assertNull(e.diario.ocorrencias["aluguel|2026-03"]!!.alteracao)
        e = e.gerarContas(d("2026-05-10"), ::id)
        assertEquals(35000L, e.lancamentos.single { it.data.monthValue == 5 }.valor)
    }

    @Test fun `deste mes em diante preserva excecao individual`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.editarSoEsteMes(e, "aluguel", m("2026-03"), "Só março", 1000, 10, d("2026-03-15"), ::id)
        e = ContasDoMes.editarDesteMesEmDiante(e, "aluguel", m("2026-02"), "Aluguel novo", 35000, 10, null, d("2026-03-15"), ::id)
        assertEquals(1000L, e.lancamentos.single { it.data.monthValue == 3 }.valor)
        assertEquals(35000L, e.lancamentos.single { it.data.monthValue == 2 }.valor)
    }

    @Test fun `pular e retomar um mes nao duplica`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-02"), d("2026-03-15"), ::id)
        assertEquals(2, e.lancamentos.size)
        assertTrue(ContasDoMes.situacoes(e, m("2026-02")).single().pulada)
        e = e.gerarContas(d("2026-03-15"), ::id)
        assertEquals(2, e.lancamentos.size)
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-02"), d("2026-03-15"), ::id)
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-02"), d("2026-03-15"), ::id).let { ContasDoMes.alternarPulo(it, "aluguel", m("2026-02"), d("2026-03-15"), ::id) }
        assertEquals(3, e.lancamentos.size)
        assertEquals(1, e.lancamentos.count { it.data.monthValue == 2 })
    }

    @Test fun `pular mes futuro impede a geracao dele`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-04"), d("2026-03-15"), ::id)
        e = e.gerarContas(d("2026-05-15"), ::id)
        assertEquals(listOf(1, 2, 3, 5), e.lancamentos.map { it.data.monthValue }.sorted())
    }

    @Test fun `pausar preserva o que ja foi lancado e retomar volta a gerar`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.alternarPausa(e, "aluguel", m("2026-04"), d("2026-03-15"), ::id)
        e = e.gerarContas(d("2026-06-15"), ::id)
        assertEquals(3, e.lancamentos.size)
        assertEquals("Repetição pausada", ContasDoMes.situacoes(e, m("2026-05")).single().texto)
        e = ContasDoMes.alternarPausa(e, "aluguel", m("2026-06"), d("2026-06-15"), ::id)
        assertEquals(4, e.lancamentos.size)
        assertEquals(setOf(1, 2, 3, 6), e.lancamentos.map { it.data.monthValue }.toSet())
    }

    @Test fun `retomar so um mes com repeticao pausada`() {
        var e = comConta("2026-03-15")
        e = ContasDoMes.alternarPausa(e, "aluguel", m("2026-04"), d("2026-03-15"), ::id)
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-05"), d("2026-05-15"), ::id)
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-05"), d("2026-05-15"), ::id)
        e = e.gerarContas(d("2026-06-15"), ::id)
        assertEquals(setOf(1, 2, 3, 5), e.lancamentos.map { it.data.monthValue }.toSet())
    }

    @Test fun `validacao do formulario de conta`() {
        assertNull(ContasDoMes.validar("Aluguel", 100, 31, d("2026-01-01"), null))
        assertEquals("Confira nome, valor, dia e datas.", ContasDoMes.validar("", 100, 31, d("2026-01-01"), null))
        assertEquals("Confira nome, valor, dia e datas.", ContasDoMes.validar("A", 100, 32, d("2026-01-01"), null))
        assertEquals("Confira nome, valor, dia e datas.", ContasDoMes.validar("A", 100, 5, d("2026-02-01"), d("2026-01-01")))
    }

    // ---- Textos e relatórios

    @Test fun `datas em portugues`() {
        assertEquals("Quinta, 29 de outubro", DataTexto.longa(d("2026-10-29")))
        assertEquals("Qui, 29 out", DataTexto.curta(d("2026-10-29")))
        assertEquals("Outubro de 2026", DataTexto.mes(m("2026-10")))
        assertEquals("05/01/2026", DataTexto.completa(d("2026-01-05")))
    }

    @Test fun `filtro de relatorio rotulo setas e parcial`() {
        val f = FiltroRelatorio(ModoRelatorio.SEMANA, d("2026-10-29"))
        assertEquals("Semana de 26/10 a 01/11", f.rotulo())
        assertTrue(f.parcial(d("2026-10-29")))
        assertEquals("Semana de 19/10 a 25/10", f.deslocar(-1).rotulo())
        val mes = FiltroRelatorio(ModoRelatorio.MES, d("2026-03-31")).deslocar(-1)
        assertEquals(Intervalo(d("2026-02-01"), d("2026-02-28")), mes.intervalo())
        val dois = FiltroRelatorio(ModoRelatorio.DOIS_DIAS, d("2026-10-29"), doisDias = listOf(d("2026-10-29"), d("2026-10-01")))
        assertEquals(setOf(d("2026-10-01"), d("2026-10-29")), dois.selecionadas())
        assertFalse(dois.parcial(d("2026-10-29")))
    }

    @Test fun `comparacao usa os mesmos dias da semana anterior`() {
        val l = listOf(frete("2026-10-26", 30000), frete("2026-10-19", 20000), frete("2026-10-24", 99900))
        val f = FiltroRelatorio(ModoRelatorio.SEMANA, d("2026-10-26"))
        val r = Relatorio.resumir(l, f.intervalo(), Metas())
        assertEquals("R$ 100,00 a mais que 19/10 a 19/10 (mesmos dias)", Textos.comparacao(l, f, r, Metas(), d("2026-10-26")))
    }

    @Test fun `dois dias soma so as duas datas`() {
        val l = listOf(frete("2026-10-01", 10000), frete("2026-10-15", 50000), frete("2026-10-29", 20000))
        val f = FiltroRelatorio(ModoRelatorio.DOIS_DIAS, d("2026-10-29"), doisDias = listOf(d("2026-10-01"), d("2026-10-29")))
        assertEquals(30000L, Relatorio.resumir(l, f.intervalo(), Metas(), f.selecionadas()).lucro)
    }

    @Test fun `resumo para whatsapp`() {
        val l = listOf(frete("2026-10-29", 28500), abast("2026-10-29", 4000), despesa("2026-10-29", 2000))
        val f = FiltroRelatorio(ModoRelatorio.DIA, d("2026-10-29"))
        val r = Relatorio.resumir(l, f.intervalo(), Metas())
        assertEquals(
            "Meus Fretes — Quinta, 29 de outubro\nFretes: R$ 285,00 (1)\nCombustível: R$ 40,00\nOutras despesas: R$ 20,00\nLucro: R$ 225,00\nMeta do dia: R$ 250,00",
            Textos.resumoParaEnviar(f, r, Metas()),
        )
    }

    @Test fun `historico agrupa por mes e so dia com frete conta meta`() {
        val di = Diario().salvar(frete("2026-10-29", 30000)).salvar(despesa("2026-10-28", 1000)).salvar(frete("2026-09-30", 10000)).conferir(d("2026-10-29"))
        val h = Historico.meses(di, Metas())
        assertEquals(listOf(m("2026-10"), m("2026-09")), h.map { it.mes })
        assertEquals(29000L, h[0].lucro)
        assertEquals(listOf(true, null), h[0].dias.map { it.metaAtingida })
        assertTrue(h[0].dias[0].conferido)
        assertEquals("Faltam R$ 150,00 para a meta", h[1].dias[0].textoMeta)
    }

    @Test fun `texto da conferencia`() {
        val f = frete("2026-10-29", 1000)
        var di = Diario().salvar(f)
        assertEquals("Conferir dia", Textos.conferencia(di, f.data))
        di = di.conferir(f.data)
        assertEquals("Dia conferido", Textos.conferencia(di, f.data))
        di = di.salvar(f.copy(valor = 2000))
        assertEquals("Alterado após a conferência", Textos.conferencia(di, f.data))
    }

    @Test fun `lembrete semanal de copia`() {
        val agora = Instant.parse("2026-10-30T12:00:00Z")
        assertTrue(LembreteCopia.precisa(null, agora))
        assertFalse(LembreteCopia.precisa(Instant.parse("2026-10-25T12:00:00Z"), agora))
        assertTrue(LembreteCopia.precisa(Instant.parse("2026-10-23T12:00:00Z"), agora))
    }

    // ---- Cópia de segurança

    private fun estadoCompleto(): Estado {
        var e = comConta("2026-03-15")
        e = ContasDoMes.alternarPulo(e, "aluguel", m("2026-02"), d("2026-03-15"), ::id)
        e = ContasDoMes.editarSoEsteMes(e, "aluguel", m("2026-03"), "Só março", 1000, 10, d("2026-03-15"), ::id)
        e = ContasDoMes.editarDesteMesEmDiante(e, "aluguel", m("2026-04"), "Aluguel novo", 35000, 31, d("2026-12-31"), d("2026-03-15"), ::id)
        val di = e.diario.salvar(frete("2026-03-14", 12345, min = 17)).salvar(abast("2026-03-14", 20000)).salvar(despesa("2020-01-01", 500)).conferir(d("2026-03-14"))
        return e.copy(
            diario = di,
            metas = Metas().comMetaDiaria(d("2026-03-01"), 30000).comMetaMensal(m("2026-03"), 700000),
            ajustes = Ajustes(910, Combustivel.ReferenciaManual(d("2026-03-01"), 455, "2026-03-01T10:00:00Z")),
        )
    }

    @Test fun `copia sem senha volta igual`() {
        val e = estadoCompleto()
        val volta = Copia.abrir(Copia.paraJson(e, "2026-03-15T10:00:00Z"), null)
        assertEquals(e, volta)
        assertEquals(17, volta.lancamentos.first { it.tipo == Tipo.FRETE }.minutos)
    }

    @Test fun `copia com senha volta igual e senha errada e recusada`() {
        val e = estadoCompleto()
        val arq = Copia.cifrar(Copia.paraJson(e, "x"), "segredo123")
        assertTrue(Copia.temSenha(arq))
        assertFalse(arq.contains("Só março"))
        assertEquals(e, Copia.abrir(arq, "segredo123"))
        assertRecusa("Senha incorreta ou arquivo alterado.") { Copia.abrir(arq, "outra") }
        assertRecusa("Esta cópia tem senha. Digite a senha para restaurar.") { Copia.abrir(arq, null) }
    }

    @Test fun `copia adulterada ou cortada e recusada`() {
        val arq = Copia.cifrar(Copia.paraJson(estadoCompleto(), "x"), "s")
        val dados = Regex("\"dados\":\"([^\"]+)\"").find(arq)!!.groupValues[1]
        val trocado = dados.replaceRange(10, 11, if (dados[10] == 'A') "B" else "A")
        assertRecusa("Senha incorreta ou arquivo alterado.") { Copia.abrir(arq.replace(dados, trocado), "s") }
        val semSenha = Copia.paraJson(estadoCompleto(), "x")
        assertRecusa("O arquivo não é uma cópia do Meus Fretes ou está incompleto.") { Copia.abrir(semSenha.take(semSenha.length / 2), null) }
        assertRecusa("O arquivo não é uma cópia do Meus Fretes ou está incompleto.") { Copia.abrir("", null) }
    }

    @Test fun `copia de versao futura e recusada`() {
        val futura = Copia.paraJson(Estado(), "x").replace("\"versao\":1", "\"versao\":99")
        assertRecusa("Esta cópia foi feita por uma versão mais nova do app. Atualize o app antes de restaurar.") { Copia.abrir(futura, null) }
    }

    @Test fun `copia com dados incoerentes e recusada`() {
        val e = estadoCompleto()
        val base = Copia.paraJson(e, "x")
        val f = e.lancamentos.first { it.tipo == Tipo.FRETE }
        // ID repetido
        val dup = base.replace("\"lancamentos\":[", "\"lancamentos\":[" + Copia.paraJson(Estado(Diario(listOf(f))), "x").substringAfter("\"lancamentos\":[").substringBefore("]") + ",")
        assertRecusa("Lançamento repetido ou sem identificação.") { Copia.abrir(dup, null) }
        // valor zero
        assertRecusa("A cópia tem dados inválidos: Valor deve ser positivo.") { Copia.abrir(base.replace("\"valor\":12345", "\"valor\":0"), null) }
        // data impossível
        assertRecusa("Data inválida: 2026-02-30.") { Copia.abrir(base.replace("2026-03-14", "2026-02-30"), null) }
        // ocorrência apontando para despesa que não existe
        val semDespesa = Estado(Diario(ocorrencias = mapOf("x|2026-01" to Ocorrencia("x", m("2026-01"), StatusOcorrencia.GERADA, "nao-existe"))))
        assertRecusa("Ocorrência de uma conta que não existe.") { Copia.abrir(Copia.paraJson(semDespesa, "x"), null) }
    }

    @Test fun `cada copia com senha usa sal novo`() {
        val j = Copia.paraJson(Estado(), "x")
        assertNotEquals(Copia.cifrar(j, "s"), Copia.cifrar(j, "s"))
    }

    private fun assertRecusa(msg: String, bloco: () -> Unit) {
        try {
            bloco()
            fail("Era para recusar: $msg")
        } catch (e: CopiaInvalida) {
            assertEquals(msg, e.mensagem)
        }
    }
}
