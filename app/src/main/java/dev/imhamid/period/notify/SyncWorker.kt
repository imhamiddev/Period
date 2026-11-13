package dev.imhamid.period.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.imhamid.period.data.Api
import dev.imhamid.period.data.Repo
import dev.imhamid.period.data.SessionStore

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val store = SessionStore(applicationContext)
        if (!store.signedIn || !Api.configured) return Result.success()
        return try {
            val snap = Repo(store).snapshot()
            Notifier.process(applicationContext, store, snap)
            Result.success()
        } catch (e: Exception) {
            Result.success() // try again at the next period
        }
    }
}
