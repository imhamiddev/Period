package dev.imhamid.period

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.imhamid.period.notify.Notifier
import dev.imhamid.period.notify.SyncWorker
import java.util.concurrent.TimeUnit

class PeriodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        // no network constraint: offline runs still fire local cycle reminders from the saved copy
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
        val wm = WorkManager.getInstance(this)
        wm.cancelUniqueWork("sync")
        wm.enqueueUniquePeriodicWork("sync_offline", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
