package dev.imhamid.period.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.imhamid.period.MainActivity
import dev.imhamid.period.R
import dev.imhamid.period.data.SessionStore
import dev.imhamid.period.data.Snapshot
import dev.imhamid.period.engine.toEngine
import dev.imhamid.period.engine.EventType
import dev.imhamid.period.ui.Fa
import java.time.LocalDate
import java.time.LocalTime

object Notifier {
    const val CHANNEL = "cycle"

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "یادآوری‌های چرخه", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "PMS، پریود و تخمک‌گذاری"
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(ctx: Context, id: Int, title: String, text: String, channel: String = CHANNEL) {
        if (!canPost(ctx)) return
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFFFF6B9D.toInt())
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }

    private val reminderTexts = listOf(
        "امروز حالت چطور بود؟ یه لاگ کوچیک ثبت کن 💗" to "چند ثانیه بیشتر وقت نمی‌بره.",
        "یادت نره امروزت رو ثبت کنی 🌸" to "حال‌وهوا، علائم یا یه یادداشت کوتاه.",
        "وقت ثبت روزانه‌ست ✨" to "هرچی دقیق‌تر ثبت کنی، پیش‌بینی‌ها بهتر می‌شن.",
        "سلام! امروز چه خبر از بدنت؟ 🌷" to "یه نگاه به حال‌وهوات بنداز و ثبتش کن.",
        "یه لحظه برای خودت 🫶" to "لاگ امروزت رو بنویس تا چرخه‌ات رو بهتر بشناسی.",
        "امروز آب خوردی؟ 💧" to "آب، حال‌وهوا و علائمت رو ثبت کن.",
    )

    /** Daily "log your day" reminder: at the chosen time, only if nothing was logged today. */
    private fun dailyReminder(ctx: Context, store: SessionStore, snap: Snapshot) {
        val pf = store.prefs()
        if (pf["rem_on"] != "true") return
        val minutes = pf["rem_min"]?.toIntOrNull() ?: 540
        val now = LocalTime.now()
        if (now.hour * 60 + now.minute < minutes) return
        val today = LocalDate.now()
        if (snap.days[today] != null || snap.health[today] != null || today in snap.logs) return
        if (!store.claim("rem-$today")) return
        val tone = pf["rem_tone"].orEmpty()
        val vib = pf["rem_vib"] != "false"
        val channel = "daily_" + (tone + vib).hashCode()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channel) == null) {
                val ch = NotificationChannel(channel, "یادآور ثبت روزانه", NotificationManager.IMPORTANCE_DEFAULT)
                val uri = if (tone.isBlank()) android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION) else android.net.Uri.parse(tone)
                ch.setSound(uri, android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
                ch.enableVibration(vib)
                nm.createNotificationChannel(ch)
            }
        }
        val t = reminderTexts.random()
        post(ctx, ("rem-$today").hashCode(), t.first, t.second, channel)
    }

    /** Called by the background worker with fresh data. */
    fun process(ctx: Context, store: SessionStore, snap: Snapshot) {
        val me = snap.me
        if (me.role == "her") dailyReminder(ctx, store, snap)
        if (!me.notifyEnabled) return
        val today = LocalDate.now()

        // Pending connection request → tell her right away (any hour).
        if (me.role == "her") {
            snap.pairs.filter { it.status == "pending" }.forEach { p ->
                val key = "req-${p.id}"
                if (store.claim(key)) {
                    val who = snap.others[p.partnerId]?.name?.takeIf { it.isNotBlank() } ?: "کسی"
                    post(ctx, key.hashCode(), "درخواست اتصال", "$who می‌خواد بهت وصل بشه. داخل برنامه تایید کن.")
                }
            }
        }

        // Quick messages → immediately.
        snap.inbox.filter { it.toId == me.id && it.readAt == null }.forEach { m ->
            val key = "msg-${m.id}"
            if (store.claim(key)) {
                val from = snap.others[m.fromId]?.name?.takeIf { it.isNotBlank() } ?: "پیام جدید"
                post(ctx, key.hashCode(), "پیام از $from", m.body)
            }
        }

        val owner = snap.owner ?: return
        if (!owner.shareEnabled && me.role == "partner") return
        val forHer = me.role == "her"
        val herName = owner.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"
        val partnerTone = forHer || Fa.isPartnerLabel(snap.activePair?.label)

        // Her mood → tell the partner right away (any hour), once per day and mood value.
        // Only the last two days are considered, so old entries never trigger a burst of alerts.
        if (!forHer) {
            val since = today.minusDays(1)
            snap.days.values
                .filter { it.userId == owner.id && it.mood != null }
                .mapNotNull { dl -> runCatching { LocalDate.parse(dl.day) }.getOrNull()?.takeIf { !it.isBefore(since) }?.let { it to dl } }
                .sortedBy { it.first }
                .forEach { (day, dl) ->
                    val mood = dl.mood ?: return@forEach
                    val key = "mood-$day-$mood-${dl.moods.sorted().joinToString(",")}"
                    if (store.claim(key)) {
                        val text = Fa.moodNotif(mood, herName, partnerTone)
                        val picked = dl.moods.mapNotNull { dev.imhamid.period.ui.Catalog.mood(it) }.joinToString("  ") { "${it.emoji} ${it.label}" }
                        post(ctx, key.hashCode(), text.first, if (picked.isBlank()) text.second else "${text.second}\n$picked")
                    }
                }
        }

        // Cycle events show up from the user's chosen hour.
        if (LocalTime.now().hour < me.notifyHour) return
        val engine = owner.toEngine(today, snap.logs)
        engine.dueEvents().forEach { ev ->
            val allowed = when (ev) {
                EventType.PMS_TOMORROW, EventType.PMS_STARTS -> me.notifyPms
                EventType.PERIOD_SOON, EventType.PERIOD_TODAY -> me.notifyPeriod
                EventType.OVULATION -> me.notifyOvulation
                EventType.PERIOD_LATE -> me.notifyLate
            }
            val key = "${ev.name}-$today"
            // claim() marks the key atomically, so two runs never both notify for it
            if (allowed && store.claim(key)) {
                val text = if (partnerTone) Fa.notif(ev, herName, forHer) else Fa.supportNotif(ev, herName)
                if (text != null) post(ctx, key.hashCode(), text.first, text.second)
            }
        }
    }
}
