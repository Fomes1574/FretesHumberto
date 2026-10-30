package io.github.fomes1574.freteshumberto.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco local do app. Esquemas exportados em app/schemas/ e versionados no Git.
 *
 * Nunca usar fallbackToDestructiveMigration: ao mudar uma tabela, subir [version] e
 * acrescentar uma Migration em [MIGRACOES], com teste (MigracaoTest).
 */
@Database(
    entities = [
        LancamentoEntidade::class, ContaEntidade::class, VersaoContaEntidade::class, OcorrenciaEntidade::class,
        MetaDiariaEntidade::class, AjusteEntidade::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BancoFretes : RoomDatabase() {
    abstract fun dao(): FretesDao

    companion object {
        const val NOME = "meus-fretes.db"

        /**
         * 1 → 2 (30/10/2026): sai a meta mensal e a conferência de dia, por decisão do usuário.
         * Apaga só essas tabelas e a meta mensal base; lançamentos, contas, metas diárias e
         * ajustes ficam como estão.
         */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `meta_mensal`")
                db.execSQL("DROP TABLE IF EXISTS `dia`")
                db.execSQL("DELETE FROM `ajuste` WHERE `chave` = 'metaMensalBase'")
            }
        }

        val MIGRACOES = arrayOf<Migration>(MIGRACAO_1_2)

        fun abrir(context: Context): BancoFretes =
            Room.databaseBuilder(context.applicationContext, BancoFretes::class.java, NOME)
                .addMigrations(*MIGRACOES)
                .build()
    }
}
