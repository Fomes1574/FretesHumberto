package io.github.fomes1574.freteshumberto.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.fomes1574.freteshumberto.core.Tipo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Banco da versão 1 (com meta mensal e conferência de dia) aberto pela versão 2:
 * os dados que continuam no app não podem se perder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MigracaoTest {
    private val nome = "migracao-teste.db"

    /** Tabelas exatamente como em app/schemas/.../1.json. */
    private val esquemaV1 = listOf(
        "CREATE TABLE IF NOT EXISTS `lancamento` (`id` TEXT NOT NULL, `tipo` TEXT NOT NULL, `data` TEXT NOT NULL, `valor` INTEGER NOT NULL, `descricao` TEXT NOT NULL, `kmCentesimos` INTEGER, `minutos` INTEGER, `litrosCentesimos` INTEGER, `precoLitro` INTEGER, `modoAbastecimento` TEXT, `categoria` TEXT, `ocorrencia` TEXT, `criadoEm` TEXT NOT NULL, `editadoEm` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_lancamento_data` ON `lancamento` (`data`)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_lancamento_ocorrencia` ON `lancamento` (`ocorrencia`)",
        "CREATE TABLE IF NOT EXISTS `conta` (`id` TEXT NOT NULL, `inicio` TEXT NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `conta_versao` (`contaId` TEXT NOT NULL, `desde` TEXT NOT NULL, `nome` TEXT NOT NULL, `valor` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `ativa` INTEGER NOT NULL, `categoria` TEXT NOT NULL, `fim` TEXT, PRIMARY KEY(`contaId`, `desde`), FOREIGN KEY(`contaId`) REFERENCES `conta`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `ocorrencia` (`contaId` TEXT NOT NULL, `mes` TEXT NOT NULL, `status` TEXT NOT NULL, `lancamentoId` TEXT, `altData` TEXT, `altValor` INTEGER, `altDescricao` TEXT, `altCategoria` TEXT, PRIMARY KEY(`contaId`, `mes`))",
        "CREATE TABLE IF NOT EXISTS `meta_diaria` (`desde` TEXT NOT NULL, `valor` INTEGER NOT NULL, PRIMARY KEY(`desde`))",
        "CREATE TABLE IF NOT EXISTS `meta_mensal` (`mes` TEXT NOT NULL, `valor` INTEGER NOT NULL, PRIMARY KEY(`mes`))",
        "CREATE TABLE IF NOT EXISTS `dia` (`data` TEXT NOT NULL, `revisao` INTEGER NOT NULL, `conferida` INTEGER, PRIMARY KEY(`data`))",
        "CREATE TABLE IF NOT EXISTS `ajuste` (`chave` TEXT NOT NULL, `valor` TEXT NOT NULL, PRIMARY KEY(`chave`))",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6daec56feda85a53c80bd5f44be7042d')",
    )

    private val dadosV1 = listOf(
        "INSERT INTO lancamento VALUES ('f1','FRETE','2026-10-29',28500,'Mudança',4500,17,NULL,NULL,NULL,NULL,NULL,'c',NULL)",
        "INSERT INTO lancamento VALUES ('a1','ABASTECIMENTO','2026-10-29',4000,'',NULL,NULL,1055,379,'PRECO',NULL,NULL,'c',NULL)",
        "INSERT INTO lancamento VALUES ('d1','DESPESA','2026-10-10',30000,'Aluguel',NULL,NULL,NULL,NULL,NULL,'Conta mensal','c1|2026-10','c',NULL)",
        "INSERT INTO conta VALUES ('c1','2026-10-01')",
        "INSERT INTO conta_versao VALUES ('c1','2026-10','Aluguel',30000,10,1,'Conta mensal',NULL)",
        "INSERT INTO ocorrencia VALUES ('c1','2026-10','GERADA','d1',NULL,NULL,NULL,NULL)",
        "INSERT INTO meta_diaria VALUES ('1900-01-01',25000)",
        "INSERT INTO meta_diaria VALUES ('2026-10-20',30000)",
        "INSERT INTO meta_mensal VALUES ('2026-10',700000)",
        "INSERT INTO dia VALUES ('2026-10-29',3,3)",
        "INSERT INTO ajuste VALUES ('kmPorLitro','910')",
        "INSERT INTO ajuste VALUES ('metaMensalBase','550000')",
    )

    @Test fun `migracao 1 para 2 mantem lancamentos metas diarias contas e ajustes`() = runBlocking {
        val c = ApplicationProvider.getApplicationContext<Context>()
        c.deleteDatabase(nome)
        SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(nome).apply { parentFile?.mkdirs() }, null).use { db ->
            (esquemaV1 + dadosV1).forEach { db.execSQL(it) }
            db.version = 1
        }

        val banco = Room.databaseBuilder(c, BancoFretes::class.java, nome).addMigrations(*BancoFretes.MIGRACOES).build()
        try {
            val e = Repositorio(banco).carregar()
            assertEquals(listOf("d1", "a1", "f1"), e.lancamentos.map { it.id })
            assertEquals(17, e.lancamentos.single { it.tipo == Tipo.FRETE }.minutos)
            assertEquals(25000L, e.metas.doDia(LocalDate.parse("2026-10-19")))
            assertEquals(30000L, e.metas.doDia(LocalDate.parse("2026-10-20")))
            assertEquals(30000L, e.contas.single().versaoEm(YearMonth.of(2026, 10))!!.valor)
            assertEquals("d1", e.diario.ocorrencias.getValue("c1|2026-10").lancamentoId)
            assertEquals(910L, e.ajustes.kmPorLitroCentesimos)

            val tabelas = banco.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cur ->
                buildList { while (cur.moveToNext()) add(cur.getString(0)) }
            }
            assertFalse("meta_mensal" in tabelas)
            assertFalse("dia" in tabelas)
            val base = banco.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ajuste WHERE chave = 'metaMensalBase'").use { cur -> cur.moveToFirst(); cur.getInt(0) }
            assertEquals(0, base)
        } finally {
            banco.close()
        }
    }
}
