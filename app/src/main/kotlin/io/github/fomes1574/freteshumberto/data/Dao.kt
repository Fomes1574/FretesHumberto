package io.github.fomes1574.freteshumberto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FretesDao {
    /*
     * Sinais de mudança, um por tabela. O repositório junta todos e relê o estado inteiro
     * quando qualquer um muda. Consulta simples por tabela: o Room acompanha cada uma com
     * certeza (a versão anterior, uma consulta só com subconsultas, não avisava quando
     * mudavam só as metas, os ajustes ou as contas).
     */
    @Query("SELECT COUNT(*) FROM lancamento") fun sinalLancamentos(): Flow<Long>
    @Query("SELECT COUNT(*) FROM ocorrencia") fun sinalOcorrencias(): Flow<Long>
    @Query("SELECT COUNT(*) FROM conta") fun sinalContas(): Flow<Long>
    @Query("SELECT COUNT(*) FROM conta_versao") fun sinalVersoes(): Flow<Long>
    @Query("SELECT COUNT(*) FROM meta_diaria") fun sinalMetas(): Flow<Long>
    @Query("SELECT COUNT(*) FROM ajuste") fun sinalAjustes(): Flow<Long>

    @Query("SELECT * FROM lancamento ORDER BY data, criadoEm, id") suspend fun lancamentos(): List<LancamentoEntidade>
    @Query("SELECT * FROM ocorrencia") suspend fun ocorrencias(): List<OcorrenciaEntidade>
    @Query("SELECT * FROM conta ORDER BY inicio, id") suspend fun contas(): List<ContaEntidade>
    @Query("SELECT * FROM conta_versao ORDER BY contaId, desde") suspend fun versoes(): List<VersaoContaEntidade>
    @Query("SELECT * FROM meta_diaria ORDER BY desde") suspend fun metasDiarias(): List<MetaDiariaEntidade>
    @Query("SELECT * FROM ajuste") suspend fun ajustes(): List<AjusteEntidade>

    /** Inserir recusa ID ou ocorrência repetidos (a transação inteira é desfeita). */
    @Insert suspend fun inserirLancamentos(l: List<LancamentoEntidade>)
    @Update suspend fun atualizarLancamentos(l: List<LancamentoEntidade>)
    @Query("DELETE FROM lancamento WHERE id IN (:ids)") suspend fun apagarLancamentos(ids: List<String>)

    @Insert suspend fun inserirOcorrencias(o: List<OcorrenciaEntidade>)
    @Update suspend fun atualizarOcorrencias(o: List<OcorrenciaEntidade>)
    @Query("DELETE FROM ocorrencia WHERE contaId = :contaId AND mes = :mes") suspend fun apagarOcorrencia(contaId: String, mes: String)

    @Insert suspend fun inserirContas(c: List<ContaEntidade>)
    @Insert suspend fun inserirVersoes(v: List<VersaoContaEntidade>)
    @Query("DELETE FROM conta_versao") suspend fun apagarVersoes()
    @Query("DELETE FROM conta") suspend fun apagarContas()

    @Insert suspend fun inserirMetasDiarias(m: List<MetaDiariaEntidade>)
    @Query("DELETE FROM meta_diaria") suspend fun apagarMetasDiarias()

    @Upsert suspend fun gravarAjustes(a: List<AjusteEntidade>)
    @Query("DELETE FROM ajuste") suspend fun apagarAjustes()

    @Query("DELETE FROM lancamento") suspend fun apagarTodosLancamentos()
    @Query("DELETE FROM ocorrencia") suspend fun apagarTodasOcorrencias()
}
