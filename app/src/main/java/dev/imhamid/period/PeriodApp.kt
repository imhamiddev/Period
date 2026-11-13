package dev.imhamid.period

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.imhamid.period.notify.Notifier
import dev.imhamid.period.notify.SyncWorker
import java.util.concurrent.TimeUnit

class PeriodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        val req = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
