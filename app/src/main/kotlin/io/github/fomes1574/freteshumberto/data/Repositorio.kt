package io.github.fomes1574.freteshumberto.data

import androidx.room.withTransaction
import io.github.fomes1574.freteshumberto.core.Ajustes
import io.github.fomes1574.freteshumberto.core.Combustivel
import io.github.fomes1574.freteshumberto.core.ContaMensal
import io.github.fomes1574.freteshumberto.core.ContasDoMes
import io.github.fomes1574.freteshumberto.core.Diario
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.MetaDiaria
import io.github.fomes1574.freteshumberto.core.Metas
import io.github.fomes1574.freteshumberto.core.Mudancas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Único caminho para ler e gravar dados. Cada operação:
 * 1. lê o estado atual dentro de uma transação;
 * 2. aplica a regra pura do módulo :core (Diario, ContasDoMes...);
 * 3. grava só a diferença, na mesma transação.
 *
 * Se qualquer passo falhar, a transação é desfeita e a exceção sobe: a tela mostra o erro
 * e nunca "Salvo" (R09).
 */
class Repositorio(
    private val banco: BancoFretes,
    private val relogio: Clock = Clock.systemDefaultZone(),
    private val geradorId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = banco.dao()

    /** Estado completo, relido a cada alteração em qualquer tabela. */
    val estado: Flow<Estado> = dao.sinal().conflate().map { carregar() }

    fun hoje(): LocalDate = LocalDate.now(relogio)

    fun agora(): String = Instant.now(relogio).toString()

    fun novoId(): String = geradorId()

    suspend fun carregar(): Estado = banco.withTransaction { ler() }

    /** Aplica [op] numa transação e devolve o valor extra que ela produzir. */
    suspend fun <T> alterarCom(op: (Estado) -> Pair<Estado, T>): T = banco.withTransaction {
        val antes = ler()
        val (depois, resultado) = op(antes)
        gravar(antes, depois)
        resultado
    }

    suspend fun alterar(op: (Estado) -> Estado) {
        alterarCom { op(it) to Unit }
    }

    // ---------------------------------------------------------------- lançamentos

    suspend fun salvar(l: Lancamento) = alterar { it.copy(diario = it.diario.salvar(l)) }

    /** Apaga só este lançamento. Devolve o necessário para o "Desfazer". */
    suspend fun apagar(id: String): Diario.Removido? = alterarCom { e ->
        val (di, removido) = e.diario.apagar(id)
        e.copy(diario = di) to removido
    }

    suspend fun desfazer(r: Diario.Removido) = alterar { it.copy(diario = it.diario.restaurar(r)) }

    suspend fun conferir(d: LocalDate) = alterar { it.copy(diario = it.diario.conferir(d)) }

    // ---------------------------------------------------------------- ajustes e metas

    /**
     * Meta diária vale de hoje em diante (R06); meta mensal só para o mês escolhido.
     * O preço manual só vira referência nova se mudou: não reescreve abastecimentos.
     */
    suspend fun salvarAjustes(metaDia: Long, mes: YearMonth, metaMes: Long, kmPorLitro: Long, preco: Long) = alterar { e ->
        val hoje = hoje()
        var metas = e.metas
        if (metas.doDia(hoje) != metaDia) metas = metas.comMetaDiaria(hoje, metaDia)
        if (metas.doMes(mes) != metaMes) metas = metas.comMetaMensal(mes, metaMes)
        val precoManual = if (preco != e.precoReferencia()) Combustivel.ReferenciaManual(hoje, preco, agora()) else e.ajustes.precoManual
        e.copy(metas = metas, ajustes = Ajustes(kmPorLitro, precoManual))
    }

    // ---------------------------------------------------------------- contas do mês

    /** Mesma função para a abertura do app e para o agendador: idempotente. */
    suspend fun gerarContas(hoje: LocalDate = hoje()) = alterar { it.gerarContas(hoje, ::novoId, agora()) }

    suspend fun cadastrarConta(nome: String, valor: Long, dia: Int, inicio: LocalDate, fim: LocalDate?) = alterar {
        ContasDoMes.cadastrar(it, novoId(), nome, valor, dia, inicio, fim, hoje(), ::novoId, agora())
    }

    suspend fun editarContaSoEsteMes(contaId: String, mes: YearMonth, nome: String, valor: Long, dia: Int) = alterar {
        ContasDoMes.editarSoEsteMes(it, contaId, mes, nome, valor, dia, hoje(), ::novoId, agora())
    }

    suspend fun editarContaDesteMes(contaId: String, mes: YearMonth, nome: String, valor: Long, dia: Int, fim: LocalDate?) = alterar {
        ContasDoMes.editarDesteMesEmDiante(it, contaId, mes, nome, valor, dia, fim, hoje(), ::novoId, agora())
    }

    suspend fun alternarPulo(contaId: String, mes: YearMonth) = alterar { ContasDoMes.alternarPulo(it, contaId, mes, hoje(), ::novoId, agora()) }

    suspend fun alternarPausa(contaId: String, mes: YearMonth) = alterar { ContasDoMes.alternarPausa(it, contaId, mes, hoje(), ::novoId, agora()) }

    // ---------------------------------------------------------------- restauração

    /**
     * Troca todos os dados pelos da cópia, numa transação. Se algo falhar no meio,
     * o banco anterior continua intacto.
     */
    suspend fun substituirTudo(novo: Estado) = banco.withTransaction {
        dao.apagarTodosLancamentos()
        dao.apagarTodasOcorrencias()
        dao.apagarTodosDias()
        dao.apagarVersoes()
        dao.apagarContas()
        dao.apagarMetasDiarias()
        dao.apagarMetasMensais()
        dao.apagarAjustes()
        gravar(ler(), novo)
    }

    // ---------------------------------------------------------------- leitura e gravação

    private suspend fun ler(): Estado {
        val dias = dao.dias()
        val diario = Diario(
            lancamentos = dao.lancamentos().map { it.paraCore() },
            ocorrencias = dao.ocorrencias().map { it.paraCore() }.associateBy { it.chave },
            revisoes = dias.filter { it.revisao > 0 }.associate { LocalDate.parse(it.data) to it.revisao },
            conferidos = dias.filter { it.conferida != null }.associate { LocalDate.parse(it.data) to it.conferida!! },
        )
        val versoes = dao.versoes().groupBy { it.contaId }
        val contas = dao.contas().mapNotNull { c ->
            versoes[c.id]?.takeIf { it.isNotEmpty() }?.let { v -> ContaMensal(c.id, LocalDate.parse(c.inicio), v.map { it.paraCore() }) }
        }
        val aj = dao.ajustes().associate { it.chave to it.valor }
        val padrao = Ajustes()
        val padraoMetas = Metas()
        val diarias = dao.metasDiarias().map { MetaDiaria(LocalDate.parse(it.desde), it.valor) }
        val metas = Metas(
            diarias = diarias.ifEmpty { padraoMetas.diarias },
            mensais = dao.metasMensais().associate { YearMonth.parse(it.mes) to it.valor },
            baseMensal = aj[META_BASE]?.toLongOrNull() ?: padraoMetas.baseMensal,
        )
        val ajustes = Ajustes(
            kmPorLitroCentesimos = aj[KM_POR_LITRO]?.toLongOrNull() ?: padrao.kmPorLitroCentesimos,
            precoManual = Combustivel.ReferenciaManual(
                data = aj[PRECO_DATA]?.let { LocalDate.parse(it) } ?: padrao.precoManual.data,
                preco = aj[PRECO_VALOR]?.toLongOrNull() ?: padrao.precoManual.preco,
                carimbo = aj[PRECO_CARIMBO] ?: padrao.precoManual.carimbo,
            ),
        )
        return Estado(diario, contas, metas, ajustes)
    }

    private suspend fun gravar(antes: Estado, depois: Estado) {
        val m = Mudancas.entre(antes, depois)
        if (m.vazia) return

        // Apagar primeiro, para liberar a ocorrência única de despesas que saíram.
        m.lancamentosApagar.chunked(LOTE).forEach { dao.apagarLancamentos(it) }
        val idsAntes = antes.lancamentos.mapTo(HashSet()) { it.id }
        val (atualizar, inserir) = m.lancamentosGravar.partition { it.id in idsAntes }
        if (atualizar.isNotEmpty()) dao.atualizarLancamentos(atualizar.map(LancamentoEntidade::de))
        if (inserir.isNotEmpty()) dao.inserirLancamentos(inserir.map(LancamentoEntidade::de))

        for (chave in m.ocorrenciasApagar) {
            val o = antes.diario.ocorrencias.getValue(chave)
            dao.apagarOcorrencia(o.contaId, o.mes.toString())
        }
        val (occAtualizar, occInserir) = m.ocorrenciasGravar.partition { it.chave in antes.diario.ocorrencias }
        if (occAtualizar.isNotEmpty()) dao.atualizarOcorrencias(occAtualizar.map(OcorrenciaEntidade::de))
        if (occInserir.isNotEmpty()) dao.inserirOcorrencias(occInserir.map(OcorrenciaEntidade::de))

        m.diasApagar.map { it.toString() }.chunked(LOTE).forEach { dao.apagarDias(it) }
        if (m.diasGravar.isNotEmpty()) dao.gravarDias(m.diasGravar.map { (d, r) -> DiaEntidade(d.toString(), r.revisao, r.conferida) })

        m.contas?.let { contas ->
            dao.apagarVersoes()
            dao.apagarContas()
            val ent = contas.map { it.entidades() }
            dao.inserirContas(ent.map { it.first })
            dao.inserirVersoes(ent.flatMap { it.second })
        }
        m.metas?.let { metas ->
            dao.apagarMetasDiarias()
            dao.apagarMetasMensais()
            dao.inserirMetasDiarias(metas.diarias.map { MetaDiariaEntidade(it.desde.toString(), it.valor) })
            dao.inserirMetasMensais(metas.mensais.map { (mes, v) -> MetaMensalEntidade(mes.toString(), v) })
            dao.gravarAjustes(listOf(AjusteEntidade(META_BASE, metas.baseMensal.toString())))
        }
        m.ajustes?.let { a ->
            dao.gravarAjustes(
                listOf(
                    AjusteEntidade(KM_POR_LITRO, a.kmPorLitroCentesimos.toString()),
                    AjusteEntidade(PRECO_DATA, a.precoManual.data.toString()),
                    AjusteEntidade(PRECO_VALOR, a.precoManual.preco.toString()),
                    AjusteEntidade(PRECO_CARIMBO, a.precoManual.carimbo),
                ),
            )
        }
    }

    private companion object {
        const val LOTE = 500
        const val KM_POR_LITRO = "kmPorLitro"
        const val PRECO_DATA = "precoManualData"
        const val PRECO_VALOR = "precoManualValor"
        const val PRECO_CARIMBO = "precoManualCarimbo"
        const val META_BASE = "metaMensalBase"
    }
}
