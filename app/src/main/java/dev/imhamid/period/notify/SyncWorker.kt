package dev.imhamid.period.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.imhamid.period.data.Api
import dev.imhamid.period.data.Offline
import dev.imhamid.period.data.Repo
import dev.imhamid.period.data.SessionStore
import dev.imhamid.period.data.Snapshot

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val store = SessionStore(applicationContext)
        if (!store.signedIn || !Api.configured) return Result.success()
        val repo = Repo(store)
        // online: push queued changes and refresh; offline: use the saved copy so cycle reminders still fire
        var fresh: Snapshot? = null
        try {
            repo.flush()
            fresh = repo.snapshot().also { Offline.save(store, it) }
        } catch (e: Exception) {
            // offline or server hiccup: fall through to the saved copy
        }
        val snap = fresh ?: Offline.load(store) ?: return Result.success()
        return try {
            Notifier.process(applicationContext, store, snap)
            Result.success()
        } catch (e: Exception) {
            Result.success() // try again at the next period
        }
    }
}
