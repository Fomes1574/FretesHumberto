package io.github.fomes1574.freteshumberto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mesmos casos de referência de tests/regras.test.cjs (protótipo), em Kotlin.
 * Se um teste daqui falhar, a regra do app divergiu do contrato em docs/REGRAS_DE_NEGOCIO.md.
 */
class RegrasTest {
    private var seq = 0
    private fun id() = "t-${++seq}"
    private fun d(s: String) = LocalDate.parse(s)
    private fun frete(data: String, valor: Long, km: Long? = null, min: Int? = null) = Lancamento(id(), Tipo.FRETE, d(data), valor, "Teste", kmCentesimos = km, minutos = min)
    private fun abast(data: String, valor: Long, litros: Long, preco: Long) = Lancamento(id(), Tipo.ABASTECIMENTO, d(data), valor, "Teste", litrosCentesimos = litros, precoLitro = preco)
    private fun despesa(data: String, valor: Long, cat: String = "Outros") = Lancamento(id(), Tipo.DESPESA, d(data), valor, "Teste", categoria = cat)
    private fun dia(s: String) = Intervalo(d(s), d(s))
    private val metas = Metas()

    // ---- Dinheiro (R03)

    @Test fun `valores brasileiros colados preservam milhares e centavos`() {
        for (s in listOf("1.234,56", "R$ 1.234,56", "1234,56")) assertEquals(123456L, Dinheiro.parse(s))
        assertEquals(8550L, Dinheiro.parse("85.50"))
        assertEquals(5405L, Dinheiro.parse("54.0599"))
        assertEquals(5405L, Dinheiro.parse("54,0599"))
        assertNull(Dinheiro.parse("1.234"))
        assertNull(Dinheiro.parse("1:30"))
        assertNull(Dinheiro.parse("1e3"))
        assertNull(Dinheiro.parse(""))
    }

    @Test fun `centavos exatos e limite maximo`() {
        assertEquals(101L, Dinheiro.parse("1,01"))
        assertEquals(99_999_999_999L, Dinheiro.parse("999.999.999,99"))
        assertNull(Dinheiro.parse("99999999999999999999"))
    }

    @Test fun `digitacao limita a duas casas e troca ponto por virgula`() {
        assertEquals("54,05", Dinheiro.filtrarDigitacao("54.0599"))
        assertEquals("0,5", Dinheiro.filtrarDigitacao(",5"))
        assertEquals("1234,59", Dinheiro.filtrarDigitacao("1a2b3c4,5,9"))
    }

    @Test fun `formato em reais`() {
        assertEquals("R$ 1.234,56", Formato.reais(123456))
        assertEquals("-R$ 60,00", Formato.reais(-6000))
        assertEquals("R$ 0,05", Formato.reais(5))
        assertEquals("R$ 5.500", Formato.reaisInteiros(550000))
        assertEquals("54,05", Formato.centesimos(5405))
        assertEquals("250,00", Formato.paraCampo(25000))
    }

    @Test fun `arredondamento metade longe de zero`() {
        assertEquals(2L, Dinheiro.roundDiv(3, 2))
        assertEquals(-2L, Dinheiro.roundDiv(-3, 2))
        assertEquals(1L, Dinheiro.roundDiv(4, 3))
    }

    // ---- Lucro e meta (R02)

    @Test fun `caso financeiro do projeto e meta diaria`() {
        val l = listOf(frete("2026-10-29", 28500), abast("2026-10-29", 4000, 1000, 400), despesa("2026-10-29", 2000))
        val o = Relatorio.resumir(l, dia("2026-10-29"), metas)
        assertEquals(22500L, o.lucro)
        assertEquals(2500L, metas.doDia(d("2026-10-29")) - o.lucro)
        assertEquals("Faltam R$ 25,00 para a meta", textoMeta(o.lucro, metas.doDia(d("2026-10-29"))))
    }

    @Test fun `prejuizo nunca e cortado em zero`() {
        val l = listOf(abast("2026-10-29", 4000, 1000, 400), despesa("2026-10-29", 2000))
        assertEquals(-6000L, Relatorio.resumir(l, dia("2026-10-29"), metas).lucro)
    }

    @Test fun `textos da meta sem julgamento`() {
        assertEquals("Meta atingida", textoMeta(25000, 25000))
        assertEquals("Meta atingida · R$ 65,00 acima", textoMeta(31500, 25000))
    }

    // ---- Calculadora (R05)

    @Test fun `calculadora 80 reais 40km 8kml 4 reais 90min`() {
        val c = Calculadora.simular(8000, 4000, 800, 400, minutos = 90)!!
        assertEquals(2000L, c.combustivel)
        assertEquals(6000L, c.sobra)
        assertEquals(150L, c.porKm)
        assertEquals(4000L, c.porHora)
        assertEquals(500L, c.litrosCentesimos)
    }

    @Test fun `calculadora preserva negativo e omite por hora sem tempo`() {
        assertEquals(-1000L, Calculadora.simular(1000, 4000, 800, 400)!!.sobra)
        assertNull(Calculadora.simular(8000, 4000, 800, 400)!!.porHora)
    }

    @Test fun `calculadora rejeita divisao por zero`() {
        assertNull(Calculadora.simular(8000, 0, 800, 400))
        assertNull(Calculadora.simular(8000, 4000, 0, 400))
        assertNull(Calculadora.simular(8000, 4000, 800, 400, outrosCustos = -1))
    }

    // ---- Combustível (R04)

    @Test fun `abastecimento aceita preco ou litros`() {
        assertEquals(Abastecimento(5405, 370), Combustivel.calcular(20000, 370, ModoAbastecimento.PRECO))
        assertEquals(Abastecimento(5405, 370), Combustivel.calcular(20000, 5405, ModoAbastecimento.LITROS))
        assertNull(Combustivel.calcular(0, 370, ModoAbastecimento.PRECO))
    }

    @Test fun `combustivel do periodo preco medio e percentual`() {
        val l = listOf(frete("2026-10-01", 100000), abast("2026-10-01", 20000, 5405, 370), abast("2026-10-02", 10000, 2632, 380))
        val o = Relatorio.resumir(l, Datas.mes(d("2026-10-15")), metas)
        assertEquals(30000L, o.combustivel)
        assertEquals(8037L, o.litrosCentesimos)
        assertEquals(373L, o.precoMedioLitro)
        assertEquals(30L, o.percentualTanque)
        assertNull(Relatorio.resumir(emptyList(), dia("2026-10-01"), metas).precoMedioLitro)
    }

    @Test fun `abastecimento antigo nao toma o lugar do preco mais recente`() {
        val novo = abast("2026-10-28", 2000, 500, 400)
        val velho = abast("2025-01-01", 2000, 400, 500)
        assertEquals(400L, Combustivel.precoReferencia(listOf(novo, velho), null, 379))
        assertEquals(500L, Combustivel.precoReferencia(listOf(velho), null, 379))
        assertEquals(379L, Combustivel.precoReferencia(emptyList(), null, 379))
    }

    // ---- Metas

    @Test fun `metas diarias respeitam a vigencia`() {
        var m = metas.comMetaDiaria(d("2026-10-29"), 30000)
        assertEquals(25000L, m.doDia(d("2026-10-28")))
        assertEquals(30000L, m.doDia(d("2026-10-29")))
        m = m.comMetaDiaria(d("2026-11-01"), 35000)
        assertEquals(30000L, m.doDia(d("2026-10-30")))
    }

    @Test fun `meta mudada a noite vale para aquele dia e os seguintes e nao para os passados`() {
        // 23:30: muda a meta de R$ 250 para R$ 300 no mesmo dia em que fez os fretes.
        val m = metas.comMetaDiaria(d("2026-10-30"), 30000).comMetaDiaria(d("2026-10-30"), 32000)
        assertEquals(25000L, m.doDia(d("2026-10-29")))
        assertEquals(32000L, m.doDia(d("2026-10-30")))
        assertEquals(32000L, m.doDia(d("2026-11-15")))
        assertEquals(2, m.diarias.size)
    }

    @Test fun `historico de meta nao muda ao alterar configuracao atual`() {
        val l = listOf(frete("2026-10-20", 26000))
        assertEquals(1, Relatorio.resumir(l, dia("2026-10-20"), metas).diasMetaAtingida)
        val m = metas.comMetaDiaria(d("2026-10-29"), 40000)
        assertEquals(1, Relatorio.resumir(l, dia("2026-10-20"), m).diasMetaAtingida)
    }

    // ---- Datas e relatórios

    @Test fun `semana comeca segunda e inclui o domingo`() {
        assertEquals(d("2026-10-26"), Datas.inicioSemana(d("2026-10-26")))
        assertEquals(d("2026-10-26"), Datas.inicioSemana(d("2026-11-01")))
        assertEquals(d("2026-11-02"), Datas.inicioSemana(d("2026-11-02")))
        val l = listOf(frete("2026-10-26", 100), frete("2026-11-01", 200), frete("2026-11-02", 400))
        assertEquals(300L, Relatorio.resumir(l, Datas.semana(d("2026-10-29")), metas).fretes)
    }

    @Test fun `periodo maximo de seis meses com as duas datas incluidas`() {
        val hoje = d("2026-10-29")
        assertNull(Datas.validarIntervalo(d("2026-01-01"), d("2026-06-30"), hoje))
        assertNotNull(Datas.validarIntervalo(d("2026-01-01"), d("2026-07-01"), hoje))
        assertNotNull(Datas.validarIntervalo(d("2026-10-29"), d("2026-10-28"), hoje))
        assertNotNull(Datas.validarIntervalo(d("2026-10-01"), d("2026-10-30"), hoje))
    }

    @Test fun `dois dias escolhidos nao incluem os intermediarios`() {
        val l = listOf(frete("2026-10-01", 100), frete("2026-10-02", 200), frete("2026-10-03", 300))
        val o = Relatorio.resumir(l, Intervalo(d("2026-10-01"), d("2026-10-03")), metas, setOf(d("2026-10-01"), d("2026-10-03")))
        assertEquals(400L, o.fretes)
    }

    @Test fun `comparacao da semana parcial usa o mesmo recorte`() {
        val p = Datas.anterior(ModoPeriodo.SEMANA, Datas.semana(d("2026-10-29")), d("2026-10-29"))
        assertEquals(Intervalo(d("2026-10-19"), d("2026-10-22")), p)
    }

    @Test fun `comparacao do mes limita ao dia disponivel no mes anterior`() {
        val p = Datas.anterior(ModoPeriodo.MES, Datas.mes(d("2026-03-15")), d("2026-03-31"))
        assertEquals(Intervalo(d("2026-02-01"), d("2026-02-28")), p)
    }

    @Test fun `selo parcial so para periodo em andamento`() {
        val hoje = d("2026-10-29")
        assertTrue(Datas.parcial(ModoPeriodo.SEMANA, Datas.semana(hoje), hoje))
        assertFalse(Datas.parcial(ModoPeriodo.SEMANA, Datas.semana(d("2026-10-01")), hoje))
        assertFalse(Datas.parcial(ModoPeriodo.DIA, Intervalo(hoje, hoje), hoje))
    }

    @Test fun `dia so com gastos aparece e media usa dias trabalhados`() {
        val l = listOf(frete("2026-10-01", 30000), despesa("2026-10-02", 5000))
        val o = Relatorio.resumir(l, Intervalo(d("2026-10-01"), d("2026-10-02")), metas)
        assertEquals(2, o.porDia.size)
        assertEquals(1, o.diasTrabalhados.size)
        assertEquals(25000L, o.lucroMedioPorDiaTrabalhado)
    }

    // ---- Diário: independência, desfazer, conferência (R01)

    @Test fun `excluir e mover frete nao altera despesas ou abastecimentos`() {
        val f = frete("2026-10-29", 8000); val dsp = despesa("2026-10-29", 1000); val a = abast("2026-10-29", 2000, 500, 400)
        var di = Diario(listOf(f, dsp, a))
        di = di.salvar(f.copy(data = d("2020-01-01")))
        assertEquals(d("2026-10-29"), di.lancamentos.first { it.id == dsp.id }.data)
        di = di.apagar(f.id).first
        assertEquals(listOf(dsp.id, a.id), di.lancamentos.map { it.id })
    }

    @Test fun `desfazer nao duplica um registro`() {
        val f = frete("2026-10-29", 8000)
        val (sem, removido) = Diario(listOf(f)).apagar(f.id)
        val volta = sem.restaurar(removido!!).restaurar(removido)
        assertEquals(1, volta.lancamentos.size)
    }

    @Test fun `editar uma duracao de 17 minutos mantem 17`() {
        val f = frete("2026-10-29", 8000, min = 17)
        val di = Diario(listOf(f)).salvar(f.copy(descricao = "Descrição nova"))
        assertEquals(17, di.lancamentos.single().minutos)
    }

    @Test fun `edicao antiga continua permitida`() {
        val f = frete("2020-01-01", 1000)
        val di = Diario().salvar(f).salvar(f.copy(valor = 2000))
        assertEquals(2000L, Relatorio.resumir(di.lancamentos, dia("2020-01-01"), metas).lucro)
    }

    @Test fun `o dia vira a meia noite de Brasilia`() {
        // 23:59 em Brasília = 02:59 UTC do dia seguinte.
        val antes = java.time.Clock.fixed(java.time.Instant.parse("2026-10-31T02:59:00Z"), java.time.ZoneOffset.UTC)
        val depois = java.time.Clock.fixed(java.time.Instant.parse("2026-10-31T03:00:00Z"), java.time.ZoneOffset.UTC)
        assertEquals(d("2026-10-30"), Datas.hoje(antes))
        assertEquals(d("2026-10-31"), Datas.hoje(depois))
        assertEquals(java.time.Duration.ofMinutes(1), Datas.ateMeiaNoite(antes))
        // Celular em outro fuso continua seguindo Brasília.
        assertEquals(d("2026-10-30"), Datas.hoje(antes.withZone(java.time.ZoneId.of("Europe/Lisbon"))))
    }

    // ---- Contas do mês

    private fun conta(inicio: String = "2026-01-01", dia: Int = 31, fim: LocalDate? = null) =
        ContaMensal("rec1", d(inicio), listOf(VersaoConta(YearMonth.from(d(inicio)), "Conta mensal", 10000, dia, fim = fim)))

    @Test fun `dia original preservado 31 jan 28 fev 31 mar 30 abr`() {
        val r = (1..4).map { Recorrencias.vencimento(YearMonth.of(2026, it), 31).toString() }
        assertEquals(listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"), r)
        assertEquals(d("2028-02-28"), Recorrencias.vencimento(YearMonth.of(2028, 2), 31))
        assertEquals(d("2026-04-29"), Recorrencias.vencimento(YearMonth.of(2026, 4), 29))
    }

    @Test fun `recorrencias sao idempotentes e respeitam o vencimento`() {
        var di = Diario().aplicarContasDoMes(listOf(conta()), d("2026-04-29"), ::id)
        assertEquals(3, di.lancamentos.size)
        di = di.aplicarContasDoMes(listOf(conta()), d("2026-04-29"), ::id)
        assertEquals(3, di.lancamentos.size)
        di = di.aplicarContasDoMes(listOf(conta()), d("2026-04-30"), ::id)
        assertEquals(4, di.lancamentos.size)
    }

    @Test fun `excluir despesa mensal pula apenas o mes sem recriar`() {
        var di = Diario().aplicarContasDoMes(listOf(conta()), d("2026-02-28"), ::id)
        di = di.apagar(di.lancamentos.first { it.data == d("2026-02-28") }.id).first
        di = di.aplicarContasDoMes(listOf(conta()), d("2026-02-28"), ::id)
        assertEquals(1, di.lancamentos.size)
        di = di.aplicarContasDoMes(listOf(conta()), d("2026-03-31"), ::id)
        assertEquals(2, di.lancamentos.size)
        assertEquals(StatusOcorrencia.PULADA, di.ocorrencias["rec1|2026-02"]!!.status)
    }

    @Test fun `repeticao pausada preserva o historico`() {
        var c = conta()
        var di = Diario().aplicarContasDoMes(listOf(c), d("2026-01-31"), ::id)
        c = c.copy(versoes = c.versoes + c.versoes[0].copy(desde = YearMonth.of(2026, 2), ativa = false))
        di = di.aplicarContasDoMes(listOf(c), d("2026-03-31"), ::id)
        assertEquals(1, di.lancamentos.size)
    }

    @Test fun `recorrencia com data final nao gera meses seguintes`() {
        val di = Diario().aplicarContasDoMes(listOf(conta(fim = d("2026-02-28"))), d("2026-06-30"), ::id)
        assertEquals(2, di.lancamentos.size)
    }

    @Test fun `alteracao so deste mes vale apenas naquele mes`() {
        var di = Diario().aplicarContasDoMes(listOf(conta()), d("2026-01-31"), ::id)
        val l = di.lancamentos.single()
        di = di.salvar(l.copy(valor = 12345))
        assertEquals(12345L, di.ocorrencias["rec1|2026-01"]!!.alteracao!!.valor)
        di = di.aplicarContasDoMes(listOf(conta()), d("2026-02-28"), ::id)
        assertEquals(10000L, di.lancamentos.first { it.data == d("2026-02-28") }.valor)
    }

    // ---- Estrada da meta

    @Test fun `estrada abaixo da meta e proporcional e meta atingida estaciona depois da linha`() {
        assertEquals(Estrada.INICIO, Estrada.alvo(0, 25000), 0.001f)
        assertEquals(Estrada.INICIO + (Estrada.ENCOSTA_NA_LINHA - Estrada.INICIO) / 2, Estrada.alvo(12500, 25000), 0.001f)
        assertEquals(Estrada.ESTACIONADA, Estrada.alvo(25000, 25000), 0.001f)
        assertEquals(Estrada.ESTACIONADA, Estrada.alvo(-500, 0), 0.001f)
        assertEquals(Estrada.INICIO, Estrada.alvo(-500, 25000), 0.001f)
    }

    @Test fun `acima da meta so o numero muda`() {
        val a = Estrada.alvo(31500, 25000); val b = Estrada.alvo(48500, 25000)
        assertFalse(Estrada.deveMover(a, b))
    }

    @Test fun `confete so ao cruzar para frente`() {
        val antes = Estrada.alvo(13500, 25000); val depois = Estrada.alvo(43500, 25000)
        assertTrue(Estrada.soltaConfete(antes, depois))
        assertFalse(Estrada.soltaConfete(depois, Estrada.alvo(18500, 25000)))
        assertFalse(Estrada.soltaConfete(depois, depois))
    }

    @Test fun `curva comeca em zero e termina em um`() {
        assertEquals(0f, Estrada.curva(0f), 0.0001f)
        assertEquals(1f, Estrada.curva(1f), 0.0001f)
        assertEquals(0.5f, Estrada.curva(0.5f), 0.0001f)
    }
}
