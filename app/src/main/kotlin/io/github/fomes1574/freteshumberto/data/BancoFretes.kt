package io.github.fomes1574.freteshumberto.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local do app. Esquema exportado em app/schemas/ e versionado no Git.
 *
 * Nunca usar fallbackToDestructiveMigration: ao mudar uma tabela, subir [version] e
 * acrescentar uma Migration em [MIGRACOES], com teste.
 */
@Database(
    entities = [
        LancamentoEntidade::class, ContaEntidade::class, VersaoContaEntidade::class, OcorrenciaEntidade::class,
        MetaDiariaEntidade::class, MetaMensalEntidade::class, DiaEntidade::class, AjusteEntidade::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BancoFretes : RoomDatabase() {
    abstract fun dao(): FretesDao

    companion object {
        const val NOME = "meus-fretes.db"

        /** Migrações entre versões. Vazio enquanto só existe a versão 1. */
        val MIGRACOES = arrayOf<androidx.room.migration.Migration>()

        fun abrir(context: Context): BancoFretes =
            Room.databaseBuilder(context.applicationContext, BancoFretes::class.java, NOME)
                .addMigrations(*MIGRACOES)
                .build()
    }
}
