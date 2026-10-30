package io.github.fomes1574.freteshumberto.contas

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.fomes1574.freteshumberto.FretesApplication
import java.util.concurrent.TimeUnit

/**
 * Lança as contas do mês que venceram (R08), mesmo se o app ficar dias sem abrir.
 * Não depende de horário exato. Chama a mesma função da abertura do app
 * ([io.github.fomes1574.freteshumberto.data.Repositorio.gerarContas]), que é idempotente:
 * rodar duas vezes, ou junto com a abertura, não duplica.
 */
class ContasWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as FretesApplication).repositorio.gerarContas()
        Result.success()
    } catch (e: Exception) {
        // Banco ocupado ou cheio: tenta de novo mais tarde. A abertura do app também gera.
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        private const val NOME = "contas-do-mes"

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<ContasWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }
    }
}
