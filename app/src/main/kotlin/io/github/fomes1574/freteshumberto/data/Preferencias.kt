package io.github.fomes1574.freteshumberto.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

/** Tamanho da letra dos Ajustes, por cima do tamanho de fonte do Android. */
enum class TamanhoLetra(val escala: Float, val rotulo: String) {
    NORMAL(1f, "A"),
    GRANDE(20f / 18f, "A+"),
    MAIOR(23f / 18f, "A++"),
}

/**
 * Preferências do aparelho, sem histórico financeiro (DataStore). Metas, contas e ajustes
 * de combustível ficam no banco porque entram nas contas e na cópia de segurança.
 */
class Preferencias(context: Context) {
    private val store = context.applicationContext.dataStore

    val letra: Flow<TamanhoLetra> = store.data.map { p -> TamanhoLetra.entries.getOrElse(p[LETRA] ?: 0) { TamanhoLetra.NORMAL } }

    /** Quando a última cópia foi gravada com sucesso no arquivo escolhido. */
    val ultimaCopia: Flow<Instant?> = store.data.map { p -> p[ULTIMA_COPIA]?.let { Instant.ofEpochMilli(it) } }

    suspend fun mudarLetra(t: TamanhoLetra) {
        store.edit { it[LETRA] = t.ordinal }
    }

    suspend fun registrarCopia(quando: Instant) {
        store.edit { it[ULTIMA_COPIA] = quando.toEpochMilli() }
    }

    private companion object {
        val LETRA = intPreferencesKey("letra")
        val ULTIMA_COPIA = longPreferencesKey("ultima_copia")
    }
}
