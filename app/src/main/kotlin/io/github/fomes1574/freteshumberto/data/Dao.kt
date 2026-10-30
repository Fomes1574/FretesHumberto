package io.github.fomes1574.freteshumberto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FretesDao {
    /**
     * Muda sempre que qualquer tabela muda (o Room emite a cada alteração, mesmo com o mesmo
     * número). O repositório usa isto como sinal para reler tudo numa leitura só.
     */
    @Query(
        "SELECT (SELECT COUNT(*) FROM lancamento) + (SELECT COUNT(*) FROM ocorrencia) + (SELECT COUNT(*) FROM conta)" +
            " + (SELECT COUNT(*) FROM conta_versao) + (SELECT COUNT(*) FROM meta_diaria) + (SELECT COUNT(*) FROM meta_mensal)" +
            " + (SELECT COUNT(*) FROM dia) + (SELECT COUNT(*) FROM ajuste)",
    )
    fun sinal(): Flow<Long>

    @Query("SELECT * FROM lancamento ORDER BY data, criadoEm, id") suspend fun lancamentos(): List<LancamentoEntidade>
    @Query("SELECT * FROM ocorrencia") suspend fun ocorrencias(): List<OcorrenciaEntidade>
    @Query("SELECT * FROM conta ORDER BY inicio, id") suspend fun contas(): List<ContaEntidade>
    @Query("SELECT * FROM conta_versao ORDER BY contaId, desde") suspend fun versoes(): List<VersaoContaEntidade>
    @Query("SELECT * FROM meta_diaria ORDER BY desde") suspend fun metasDiarias(): List<MetaDiariaEntidade>
    @Query("SELECT * FROM meta_mensal ORDER BY mes") suspend fun metasMensais(): List<MetaMensalEntidade>
    @Query("SELECT * FROM dia") suspend fun dias(): List<DiaEntidade>
    @Query("SELECT * FROM ajuste") suspend fun ajustes(): List<AjusteEntidade>

    /** Inserir recusa ID ou ocorrência repetidos (a transação inteira é desfeita). */
    @Insert suspend fun inserirLancamentos(l: List<LancamentoEntidade>)
    @Update suspend fun atualizarLancamentos(l: List<LancamentoEntidade>)
    @Query("DELETE FROM lancamento WHERE id IN (:ids)") suspend fun apagarLancamentos(ids: List<String>)

    @Insert suspend fun inserirOcorrencias(o: List<OcorrenciaEntidade>)
    @Update suspend fun atualizarOcorrencias(o: List<OcorrenciaEntidade>)
    @Query("DELETE FROM ocorrencia WHERE contaId = :contaId AND mes = :mes") suspend fun apagarOcorrencia(contaId: String, mes: String)

    @Upsert suspend fun gravarDias(d: List<DiaEntidade>)
    @Query("DELETE FROM dia WHERE data IN (:datas)") suspend fun apagarDias(datas: List<String>)

    @Insert suspend fun inserirContas(c: List<ContaEntidade>)
    @Insert suspend fun inserirVersoes(v: List<VersaoContaEntidade>)
    @Query("DELETE FROM conta_versao") suspend fun apagarVersoes()
    @Query("DELETE FROM conta") suspend fun apagarContas()

    @Insert suspend fun inserirMetasDiarias(m: List<MetaDiariaEntidade>)
    @Insert suspend fun inserirMetasMensais(m: List<MetaMensalEntidade>)
    @Query("DELETE FROM meta_diaria") suspend fun apagarMetasDiarias()
    @Query("DELETE FROM meta_mensal") suspend fun apagarMetasMensais()

    @Upsert suspend fun gravarAjustes(a: List<AjusteEntidade>)
    @Query("DELETE FROM ajuste") suspend fun apagarAjustes()

    @Query("DELETE FROM lancamento") suspend fun apagarTodosLancamentos()
    @Query("DELETE FROM ocorrencia") suspend fun apagarTodasOcorrencias()
    @Query("DELETE FROM dia") suspend fun apagarTodosDias()
}
