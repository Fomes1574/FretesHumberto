package io.github.fomes1574.freteshumberto.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.fomes1574.freteshumberto.core.Copia
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.ModoAbastecimento
import io.github.fomes1574.freteshumberto.core.Ocorrencia
import io.github.fomes1574.freteshumberto.core.StatusOcorrencia
import io.github.fomes1574.freteshumberto.core.Tipo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * Operações do banco (etapa 2): cada uma numa transação, sem cascata entre frete e gastos,
 * sem duplicar contas do mês e sem gravar nada quando falha.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositorioTest {
    private lateinit var banco: BancoFretes
    private lateinit var repo: Repositorio
    private val seq = AtomicInteger()
    private val relogio = Clock.fixed(Instant.parse("2026-03-15T15:00:00Z"), ZoneOffset.UTC)
    private fun d(s: String) = LocalDate.parse(s)

    @Before fun abrir() {
        banco = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BancoFretes::class.java).build()
        repo = Repositorio(banco, relogio) { "id-${seq.incrementAndGet()}" }
    }

    @After fun fechar() = banco.close()

    private fun frete(data: String, valor: Long, id: String = repo.novoId()) =
        Lancamento(id, Tipo.FRETE, d(data), valor, "Frete", kmCentesimos = 4500, minutos = 17, criadoEm = "c")

    private fun despesa(data: String, valor: Long) =
        Lancamento(repo.novoId(), Tipo.DESPESA, d(data), valor, "Pedágio", categoria = "Pedágio", criadoEm = "c")

    private fun abast(data: String, valor: Long) =
        Lancamento(repo.novoId(), Tipo.ABASTECIMENTO, d(data), valor, "", litrosCentesimos = 5405, precoLitro = 370, modoAbastecimento = ModoAbastecimento.PRECO, criadoEm = "c")

    private fun ordenado(e: Estado) = e.copy(diario = e.diario.copy(lancamentos = e.lancamentos.sortedBy { it.id }))

    @Test fun `salvar e reler devolve exatamente o que foi gravado`() = runBlocking {
        val f = frete("2026-03-15", 28500); val a = abast("2026-03-15", 4000); val g = despesa("2026-03-14", 2000)
        repo.salvar(f); repo.salvar(a); repo.salvar(g)
        val e = repo.carregar()
        assertEquals(setOf(f, a, g), e.lancamentos.toSet())
        assertEquals(17, e.lancamentos.first { it.id == f.id }.minutos)
    }

    @Test fun `salvar duas vezes o mesmo lancamento nao duplica`() = runBlocking {
        val f = frete("2026-03-15", 28500)
        repo.salvar(f); repo.salvar(f)
        assertEquals(1, repo.carregar().lancamentos.size)
    }

    @Test fun `apagar e mover frete nao mexe em gastos e desfazer volta uma vez`() = runBlocking {
        val f = frete("2026-03-15", 8000); val a = abast("2026-03-15", 2000); val g = despesa("2026-03-15", 1000)
        repo.salvar(f); repo.salvar(a); repo.salvar(g)
        repo.salvar(f.copy(data = d("2026-03-01")))
        assertEquals(listOf(d("2026-03-15"), d("2026-03-15")), repo.carregar().lancamentos.filter { it.tipo != Tipo.FRETE }.map { it.data })
        val removido = repo.apagar(f.id)
        assertNotNull(removido)
        assertEquals(setOf(a.id, g.id), repo.carregar().lancamentos.map { it.id }.toSet())
        repo.desfazer(removido!!); repo.desfazer(removido)
        assertEquals(3, repo.carregar().lancamentos.size)
    }

    @Test fun `o dia vira a meia noite de Brasilia`() {
        // 2026-03-16 02:59 UTC = 15/03 23:59 em Brasília; 03:00 UTC = 16/03 00:00.
        val antes = Repositorio(banco, Clock.fixed(Instant.parse("2026-03-16T02:59:00Z"), ZoneOffset.UTC))
        val depois = Repositorio(banco, Clock.fixed(Instant.parse("2026-03-16T03:00:00Z"), ZoneOffset.UTC))
        assertEquals(d("2026-03-15"), antes.hoje())
        assertEquals(d("2026-03-16"), depois.hoje())
    }

    @Test fun `meta mudada a noite vale para aquele dia e os seguintes e nao para os passados`() = runBlocking {
        // relogio: 15/03 12:00 em Brasília.
        repo.salvarAjustes(metaDia = 30000, kmPorLitro = 850, preco = 379)
        repo.salvarAjustes(metaDia = 32000, kmPorLitro = 850, preco = 379)
        val e = repo.carregar()
        assertEquals(25000L, e.metas.doDia(d("2026-03-14")))
        assertEquals(32000L, e.metas.doDia(d("2026-03-15")))
        assertEquals(32000L, e.metas.doDia(d("2026-04-01")))
    }

    @Test fun `falha no meio da gravacao desfaz a transacao inteira`() = runBlocking {
        repo.cadastrarConta("Aluguel", 30000, 10, d("2026-01-01"), null)
        val antes = repo.carregar()
        val novo = frete("2026-03-15", 5000)
        try {
            repo.alterar { e ->
                // Segunda despesa para a mesma conta e mês: o índice único do banco recusa.
                val dup = despesa("2026-01-10", 100).copy(ocorrencia = "${e.contas.single().id}|2026-01")
                e.copy(diario = e.diario.salvar(novo).copy(lancamentos = e.diario.salvar(novo).lancamentos + dup))
            }
            fail("Era para falhar")
        } catch (_: Exception) {
        }
        assertEquals(ordenado(antes), ordenado(repo.carregar()))
    }

    @Test fun `contas do mes nao duplicam nem rodando junto`() = runBlocking {
        repo.cadastrarConta("Aluguel", 30000, 10, d("2026-01-01"), null)
        assertEquals(3, repo.carregar().lancamentos.size)
        (1..8).map { async(Dispatchers.Default) { repo.gerarContas(d("2026-05-20")) } }.awaitAll()
        val e = repo.carregar()
        assertEquals(5, e.lancamentos.size)
        assertEquals(5, e.diario.ocorrencias.size)
    }

    @Test fun `mes pulado continua pulado depois de apagar a despesa`() = runBlocking {
        repo.cadastrarConta("Aluguel", 30000, 10, d("2026-01-01"), null)
        val fev = repo.carregar().lancamentos.single { it.data == d("2026-02-10") }
        repo.apagar(fev.id)
        repo.gerarContas(d("2026-04-30"))
        val e = repo.carregar()
        assertEquals(3, e.lancamentos.size)
        assertEquals(StatusOcorrencia.PULADA, e.diario.ocorrencias.values.single { it.mes == YearMonth.of(2026, 2) }.status)
    }

    @Test fun `ajustes guardam metas historicas`() = runBlocking {
        repo.salvarAjustes(metaDia = 30000, kmPorLitro = 910, preco = 455)
        val e = repo.carregar()
        assertEquals(25000L, e.metas.doDia(d("2026-03-14")))
        assertEquals(30000L, e.metas.doDia(d("2026-03-15")))
        assertEquals(455L, e.precoReferencia())
        assertEquals(910L, e.ajustes.kmPorLitroCentesimos)
    }

    @Test fun `restaurar copia substitui tudo e volta igual`() = runBlocking {
        repo.salvar(frete("2026-03-10", 1000))
        repo.cadastrarConta("Aluguel", 30000, 31, d("2026-01-01"), null)
        repo.salvarAjustes(30000, 910, 455)
        val copia = Copia.paraJson(repo.carregar(), repo.agora())
        val original = Copia.abrir(copia, null)

        repo.salvar(frete("2026-03-15", 99900))
        repo.substituirTudo(original)
        assertEquals(ordenado(original), ordenado(repo.carregar()))
    }

    @Test fun `restauracao que falha mantem os dados anteriores`() = runBlocking {
        repo.salvar(frete("2026-03-10", 1000))
        val antes = repo.carregar()
        val a = despesa("2026-01-10", 100).copy(ocorrencia = "x|2026-01")
        val b = despesa("2026-01-10", 100).copy(ocorrencia = "x|2026-01")
        val ruim = Estado().copy(diario = Estado().diario.copy(lancamentos = listOf(a, b), ocorrencias = mapOf("x|2026-01" to Ocorrencia("x", YearMonth.of(2026, 1), StatusOcorrencia.GERADA, a.id))))
        try {
            repo.substituirTudo(ruim)
            fail("Era para falhar")
        } catch (_: Exception) {
        }
        assertEquals(ordenado(antes), ordenado(repo.carregar()))
    }

    @Test fun `estado observado acompanha as gravacoes`() = runBlocking {
        assertEquals(0, withTimeout(5_000) { repo.estado.first() }.lancamentos.size)
        repo.salvar(frete("2026-03-15", 1000))
        withTimeout(5_000) { repo.estado.first { it.lancamentos.size == 1 } }
        Unit
    }

    @Test fun `estado observado acompanha meta ajustes e contas mesmo sem lancamento novo`() = runBlocking {
        withTimeout(5_000) { repo.estado.first() }
        repo.salvarAjustes(metaDia = 30000, kmPorLitro = 1000, preco = 500)
        withTimeout(5_000) { repo.estado.first { it.metas.doDia(d("2026-03-15")) == 30000L && it.ajustes.kmPorLitroCentesimos == 1000L } }
        // Conta que só vence depois: grava conta e versão, nenhum lançamento.
        repo.cadastrarConta("Seguro", 20000, 20, d("2026-03-15"), null)
        withTimeout(5_000) { repo.estado.first { it.contas.size == 1 && it.lancamentos.isEmpty() } }
        repo.alternarPausa(repo.carregar().contas.single().id, YearMonth.of(2026, 3))
        withTimeout(5_000) { repo.estado.first { e -> e.contas.single().versaoEm(YearMonth.of(2026, 3))?.ativa == false } }
        Unit
    }
}
