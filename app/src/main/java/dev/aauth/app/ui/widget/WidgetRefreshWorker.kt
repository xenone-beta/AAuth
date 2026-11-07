package dev.aauth.app.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Runs when a revealed code expires, to hide it again on the home screen. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetState.clearExpired(applicationContext, System.currentTimeMillis())
        CodesWidget().updateAll(applicationContext)
        scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "aauth_widget_refresh"

        /** Schedules a refresh for the soonest reveal that is still active. */
        fun scheduleNext(context: Context) {
            val now = System.currentTimeMillis()
            val next = WidgetState.nextExpiry(context, now) ?: return
            val delay = (next - now + 500L).coerceAtLeast(500L)
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
