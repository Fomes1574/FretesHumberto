package io.github.fomes1574.freteshumberto

import android.app.Application
import io.github.fomes1574.freteshumberto.contas.ContasWorker
import io.github.fomes1574.freteshumberto.data.BancoFretes
import io.github.fomes1574.freteshumberto.data.Preferencias
import io.github.fomes1574.freteshumberto.data.Repositorio

/** Guarda as instâncias únicas do banco, do repositório e das preferências. */
class FretesApplication : Application() {
    val banco: BancoFretes by lazy { BancoFretes.abrir(this) }
    val repositorio: Repositorio by lazy { Repositorio(banco) }
    val preferencias: Preferencias by lazy { Preferencias(this) }

    override fun onCreate() {
        super.onCreate()
        ContasWorker.agendar(this)
    }
}
