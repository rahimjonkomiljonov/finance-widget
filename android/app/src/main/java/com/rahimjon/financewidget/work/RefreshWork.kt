package com.rahimjon.financewidget.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rahimjon.financewidget.FinanceApp
import java.util.concurrent.TimeUnit

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as FinanceApp
        val result = runCatching { app.refreshQuotes() }.getOrNull()
        val everythingFailed = result == null ||
            (result.failedSymbols.isNotEmpty() && result.quotes.isEmpty())
        return if (everythingFailed && runAttemptCount < 3) Result.retry() else Result.success()
    }
}

object RefreshScheduler {
    private const val PERIODIC = "periodic-refresh"
    private const val ONCE = "refresh-now"

    // WorkManager will not run periodic work more often than every 15 minutes.
    const val MIN_INTERVAL_MINUTES = 15

    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context, minutes: Int) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(
            maxOf(MIN_INTERVAL_MINUTES, minutes).toLong(), TimeUnit.MINUTES,
        ).setConstraints(needsNetwork).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(needsNetwork).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.KEEP, request)
    }
}
