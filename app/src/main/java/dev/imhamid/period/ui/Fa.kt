package dev.imhamid.period.ui

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import dev.imhamid.period.engine.EventType
import dev.imhamid.period.engine.Phase
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/** Persian helpers: digits, Jalali dates, copy. */
object Fa {
    private val locale = ULocale("fa_IR@calendar=persian")
    private val zone = ZoneId.systemDefault()

    fun digits(s: String): String = buildString {
        for (c in s) append(if (c in '0'..'9') ('۰' + (c - '0')) else c)
    }

    fun n(i: Int): String = digits(i.toString())

    fun date(d: LocalDate, pattern: String = "d MMMM"): String {
        val f = SimpleDateFormat(pattern, locale)
        return f.format(Date.from(d.atStartOfDay(zone).toInstant()))
    }

    /** First day (Gregorian date) of the Jalali month containing [d], and its length. */
    fun jalaliMonth(d: LocalDate): Pair<LocalDate, Int> {
        val cal = Calendar.getInstance(locale)
        cal.timeInMillis = d.atStartOfDay(zone).toInstant().toEpochMilli()
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        val len = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return d.minusDays(dom - 1L) to len
    }

    fun jalaliDay(d: LocalDate): String = date(d, "d")

    fun daysText(n: Int): String = when {
        n == 0 -> "امروز"
        n == 1 -> "فردا"
        n == -1 -> "دیروز"
        n > 1 -> "${n(n)} روز دیگه"
        else -> "${n(-n)} روز پیش"
    }

    fun phaseName(p: Phase?): String = when (p) {
        Phase.MENSTRUAL -> "پریود"
        Phase.FOLLICULAR -> "فولیکولار"
        Phase.FERTILE -> "پنجره‌ی باروری"
        Phase.OVULATION -> "تخمک‌گذاری"
        Phase.LUTEAL -> "لوتئال"
        Phase.PMS -> "PMS"
        null -> "—"
    }

    fun phaseHint(p: Phase?): String = when (p) {
        Phase.MENSTRUAL -> "روزهای پریود"
        Phase.FOLLICULAR -> "انرژی در حال بالا رفتنه"
        Phase.FERTILE -> "احتمال بارداری بالاست"
        Phase.OVULATION -> "اوج پنجره‌ی باروری"
        Phase.LUTEAL -> "آروم‌تر و درون‌گراتر"
        Phase.PMS -> "ممکنه حساس‌تر باشه"
        null -> "اطلاعات کافی نیست"
    }

    fun partnerTip(p: Phase?, name: String): String = when (p) {
        Phase.MENSTRUAL -> "ممکنه $name گرفتگی و بی‌حالی داشته باشه. کیسه‌ی آب‌گرم، یه خوراکی دوست‌داشتنی و برداشتن یکی از کارهاش خیلی کمک می‌کنه."
        Phase.FOLLICULAR -> "انرژی $name معمولاً این روزها بالا می‌ره؛ وقت خوبیه برای برنامه‌ی بیرون رفتن و کارهای فعال."
        Phase.FERTILE -> "پنجره‌ی باروریه؛ اگه برنامه‌ی خاصی داری حواست باشه."
        Phase.OVULATION -> "تخمک‌گذاری امروز تخمین زده می‌شه؛ اوج پنجره‌ی باروری."
        Phase.LUTEAL -> "$name ممکنه آروم‌تر و خسته‌تر باشه. کمی فضا و مهربونی اضافه کار خودش رو می‌کنه."
        Phase.PMS -> "PMS شروع شده. ممکنه $name حساس‌تر یا بی‌حوصله‌تر باشه؛ صبوری، شکلات و یه بغل محکم کمک می‌کنه 💚"
        null -> "هنوز اطلاعات کافی برای پیش‌بینی نیست."
    }

    fun herTip(p: Phase?): String = when (p) {
        Phase.MENSTRUAL -> "به خودت استراحت بده؛ آب‌گرم و غذای گرم کمک می‌کنه."
        Phase.FOLLICULAR -> "انرژیت داره برمی‌گرده؛ وقت خوبیه برای شروع کارهای جدید."
        Phase.FERTILE -> "در پنجره‌ی باروری هستی."
        Phase.OVULATION -> "امروز تخمک‌گذاری تخمین زده می‌شه."
        Phase.LUTEAL -> "ممکنه آروم‌تر باشی؛ به خودت سخت نگیر."
        Phase.PMS -> "نزدیک پریودی. اگه حس کردی حساس‌تری، طبیعیه؛ به خودت مهربون باش."
        null -> "پریودت رو ثبت کن تا پیش‌بینی شروع بشه."
    }

    fun notif(type: EventType, herName: String, forHer: Boolean): Pair<String, String> {
        return if (forHer) when (type) {
            EventType.PMS_TOMORROW -> "فردا PMS شروع می‌شه" to "به خودت مهربون باش 💚"
            EventType.PMS_STARTS -> "امروز PMS شروع شده" to "اگه حساس‌تری، طبیعیه."
            EventType.PERIOD_SOON -> "۲ روز تا پریود" to "آماده باش؛ وسایلت رو چک کن."
            EventType.PERIOD_TODAY -> "امروز روز پریوده" to "اگه شروع شد، تو برنامه ثبتش کن."
            EventType.OVULATION -> "تخمک‌گذاری امروز" to "اوج پنجره‌ی باروری."
            EventType.PERIOD_LATE -> "پریودت کمی دیر کرده" to "اگه شروع شده تو برنامه ثبتش کن تا پیش‌بینی دقیق‌تر بشه."
        } else when (type) {
            EventType.PMS_TOMORROW -> "فردا PMS $herName شروع می‌شه" to "آماده‌ی کمی صبوری و مهربونی بیشتر باش 💚"
            EventType.PMS_STARTS -> "امروز PMS $herName شروع شده" to "ممکنه حساس‌تر باشه؛ شکلات و یه بغل یادت نره."
            EventType.PERIOD_SOON -> "۲ روز تا پریود $herName" to "کیسه‌ی آب‌گرم و خوراکی مورد علاقه‌ش رو آماده کن."
            EventType.PERIOD_TODAY -> "امروز احتمالاً پریود $herName شروع می‌شه" to "حواست بهش باشه."
            EventType.OVULATION -> "تخمک‌گذاری $herName امروز تخمین زده می‌شه" to "اوج پنجره‌ی باروری."
            EventType.PERIOD_LATE -> "پریود $herName کمی دیر کرده" to "ممکنه استرس یا تغییر برنامه باشه؛ آروم و مهربون کنارش باش."
        }
    }

    val moods = listOf("😣", "😕", "😐", "🙂", "😄")
    val moodNames = listOf("خیلی بد", "بد", "معمولی", "خوب", "عالی")
    val energyNames = listOf("کم", "متوسط", "زیاد")

    val symptoms = listOf(
        "cramps" to "گرفتگی", "headache" to "سردرد", "backpain" to "کمردرد", "bloating" to "نفخ",
        "tender" to "حساسیت سینه", "acne" to "جوش", "fatigue" to "خستگی", "insomnia" to "بی‌خوابی",
        "cravings" to "هوس خوراکی", "nausea" to "تهوع", "irritable" to "زودرنجی", "sad" to "غمگینی",
    )
    fun symptomName(id: String) = symptoms.firstOrNull { it.first == id }?.second ?: id

    val herQuick = listOf(
        "امروز حالم خوب نیست 😔", "بغل می‌خوام 🤗", "شکلات می‌خوام 🍫",
        "کمی تنهایی لازم دارم", "گرفتگی دارم، کمکم کن", "همه‌چی خوبه 💚",
    )
    val partnerQuick = listOf(
        "دوستت دارم 💚", "بغل می‌خوای؟ 🤗", "چای یا قهوه برات بیارم؟ ☕",
        "امشب شام با من 🍝", "اگه خواستی حرف بزنیم، هستم", "فکرت هستم 🌿",
    )

    fun ago(iso: String): String {
        val t = runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return ""
        val m = java.time.Duration.between(t, java.time.Instant.now()).toMinutes().coerceAtLeast(0)
        return when {
            m < 1 -> "همین الان"
            m < 60 -> "${n(m.toInt())} دقیقه پیش"
            m < 60 * 24 -> "${n((m / 60).toInt())} ساعت پیش"
            else -> "${n((m / 1440).toInt())} روز پیش"
        }
    }

    fun hour(h: Int) = digits("$h:00")
}
