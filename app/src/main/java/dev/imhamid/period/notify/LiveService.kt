package dev.imhamid.period.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.imhamid.period.R
import dev.imhamid.period.data.Api
import dev.imhamid.period.data.Repo
import dev.imhamid.period.data.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Optional "instant messages" mode: a tiny foreground service that checks for new messages every ~30 s (backing off when offline),
 * so alerts arrive within seconds without Google services / Firebase.
 */
class LiveService : Service() {
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        if (job == null) {
            job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val store = SessionStore(applicationContext)
                val repo = Repo(store)
                var wait = POLL_MS
                while (isActive && store.liveNotify && store.signedIn && Api.configured) {
                    wait = try {
                        // light poll: only the ids of unread messages; the full snapshot is fetched only for news
                        val fresh = repo.unreadIds().any { !store.wasSent("msg-$it") }
                        if (fresh) Notifier.process(applicationContext, store, repo.snapshot())
                        POLL_MS
                    } catch (e: Exception) {
                        // offline or server hiccup: back off (30 s, 60 s, … up to 5 min) instead of hammering
                        minOf(wait * 2, MAX_BACKOFF_MS)
                    }
                    delay(wait)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        super.onDestroy()
    }

    private fun goForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "اعلان فوری پیام‌ها", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Luna")
            .setContentText("اعلان فوری پیام‌ها فعاله")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    companion object {
        private const val CHANNEL = "live"
        private const val NOTIF_ID = 7001
        private const val POLL_MS = 30_000L
        private const val MAX_BACKOFF_MS = 300_000L

        fun start(ctx: Context) {
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, LiveService::class.java)) }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, LiveService::class.java)) }
        }
    }
}

/** Restarts the instant-messages service after a reboot (only if the user turned it on). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = SessionStore(context)
        if (store.liveNotify && store.signedIn) LiveService.start(context)
    }
}
