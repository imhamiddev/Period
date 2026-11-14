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

    private fun <T> pick(vararg v: T): T = v.toList().random()

    fun partnerTip(p: Phase?, name: String): String = when (p) {
        Phase.MENSTRUAL -> pick(
            "$name این روزا گرفتگی داره؛ وظیفه‌ی توئه: کیسه‌آب‌گرم، شکلات و صفر بحث. عملیات قهرمانی 🦸",
            "پریود $name در جریانه 🩸 امروز تو نقش خدمتکار درجه‌یکی. شکلات، چای، ساکت.",
        )
        Phase.FOLLICULAR -> pick(
            "انرژی $name بالاست ⚡ ببرش بیرون قبل از اینکه خودش بگه 💃",
            "$name این روزا سر حاله؛ وقتشه یه برنامه‌ی درست‌وحسابی بچینی.",
        )
        Phase.FERTILE -> "پنجره‌ی باروری بازه 🪟 اگه هنوز آماده‌ی بابا شدن نیستی، حواست رو جمع کن 😅"
        Phase.OVULATION -> "تخمک‌گذاری امروزه 🥚 سیاره‌ها هم‌راستا شدن؛ حواست باشه."
        Phase.LUTEAL -> pick(
            "$name یه کم آروم‌تر و خسته‌ست. کم‌حرف‌تر، مهربون‌تر باش 🫶",
            "فاز لوتئال؛ یعنی $name دلش می‌خواد کسی ازش نپرسه «چته؟» 😅",
        )
        Phase.PMS -> pick(
            "PMS فعاله 🎢 قانون یک: بحث نکن. قانون دو: شکلات بیار. قانون سه: برگرد به قانون یک 🍫",
            "هشدار قرمز 🚨 $name تو فاز PMS ـه. فقط بغل و شکلات، هیچ توضیحی نده!",
        )
        null -> "هنوز اطلاعات کافی برای پیش‌بینی نیست."
    }

    fun herTip(p: Phase?): String = when (p) {
        Phase.MENSTRUAL -> "امروز مجازی هرچی دلت خواست بخوری و هیچ کاری نکنی 🛋️ کیسه‌آب‌گرم هم نذار زمین."
        Phase.FOLLICULAR -> "انرژیت داره برمی‌گرده 🔋 وقتشه دنیا رو بخوری."
        Phase.FERTILE -> "پنجره‌ی باروری بازه 🪟 مراقب باش."
        Phase.OVULATION -> "امروز تخمک‌گذاری تخمین زده می‌شه 🥚"
        Phase.LUTEAL -> "ممکنه آروم‌تر باشی؛ به خودت سخت نگیر و بذار مرد خونه هم یه کاری بکنه 😌"
        Phase.PMS -> pick(
            "PMS اومده 🎢 هر چی خواستی بخور، هر چی خواستی بگو؛ گناهش گردن هورمون‌هاست!",
            "از امروز رسماً مجازی غر بزنی و شکلات بخوری 🍫 هورمون‌ها پای همه‌چی وایسادن.",
        )
        null -> "پریودت رو ثبت کن تا پیش‌بینی شروع بشه."
    }

    fun notif(type: EventType, herName: String, forHer: Boolean): Pair<String, String> {
        return if (forHer) when (type) {
            EventType.PMS_TOMORROW -> pick(
                "فردا PMS میاد 🎢" to "به همه‌ی دنیا حق بده، به خودت حق بده، به شکلات هم 🍫",
                "هوی! فردا PMS استارت می‌خوره 🚨" to "امشب آخرین شبه که آدم حسابی‌ای 😂",
            )
            EventType.PMS_STARTS -> pick(
                "PMS استارت خورد 💥" to "امروز رسماً مجازی هرچی دلت خواست بخوری!",
                "حالت دفاعی فعال شد 🛡️" to "هورمون‌ها مسئولن، نه تو. شکلات یادت نره 🍫",
            )
            EventType.PERIOD_SOON -> pick(
                "۲ روز دیگه پریودی 🩸" to "نوار بهداشتی تو کیفته یا باز می‌خوای شاکی شی؟ 😅",
                "پریود داره میاد 🩸" to "دو روز مونده؛ مسکن و کیسه‌آب‌گرم رو آماده کن.",
            )
            EventType.PERIOD_TODAY -> pick(
                "امروز روز پریوده 🩸" to "اگه شروع شد تو برنامه ثبتش کن؛ بقیشو بسپار به من.",
                "الو؟ پریود دم دره 🚪" to "اگه اومد ثبتش کن و آقاتو بفرست شکلات بخره 🍫",
            )
            EventType.OVULATION -> "تخمک‌گذاری امروزه 🥚" to "اوج پنجره‌ی باروری؛ حواست به خودت و آقات باشه 😏"
            EventType.PERIOD_LATE -> "پریودت دیر کرده 👀" to "هول نکن، شاید استرس باشه. اگه شروع شد تو برنامه ثبت کن."
        } else when (type) {
            EventType.PMS_TOMORROW -> pick(
                "هوی! فردا PMS $herName شروع می‌شه 🚨" to "کلاه‌خودتو بذار سرت و شکلات بخر 🍫",
                "فردا مود $herName وارد فصل جدید می‌شه 🎢" to "کمربندتو ببند مرد حسابی.",
            )
            EventType.PMS_STARTS -> pick(
                "PMS $herName امروز استارت خورد 🎢" to "حالت دفاعی فعال؛ فقط بغل و شکلات، توضیح ممنوع 🍫",
                "هشدار قرمز 🚨 $herName تو فاز PMS ـه" to "قانون اول: بحث نکن. قانون دوم: به قانون اول برگرد 😂",
            )
            EventType.PERIOD_SOON -> pick(
                "۲ روز دیگه $herName پریود می‌شه 🩸" to "کیسه‌آب‌گرم و شکلات رو آماده کن مرد حسابی.",
                "هوی! دو روز تا پریود $herName 🩸" to "برو مسکن و خوراکی بخر، منتظر چی هستی؟",
            )
            EventType.PERIOD_TODAY -> pick(
                "هوی مرد حسابی! زن خوشگلت امروز پریود می‌شه 🩸" to "شکلات، مسکن، کیسه‌آب‌گرم؛ صفر بحث. قهرمان باش 🦸",
                "$herName امروز احتمالاً پریود شده 🩸" to "بدو یه شکلات بخر؛ مرگ و زندگیه 🍫",
            )
            EventType.OVULATION -> "تخمک‌گذاری $herName امروزه 🥚" to "اگه آماده‌ی بابا شدن نیستی، دقت کن 😅"
            EventType.PERIOD_LATE -> "پریود $herName دیر کرده 👀" to "هول نکن، شاید فقط استرس باشه. آروم و مهربون باش."
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
        "حالم خوب نیست، ریسک نکن 😤", "بغل کوووو 🤗 همین الان!", "شکلات بیار، جون خودت 🍫",
        "یه ذره تنهایی می‌خوام، قهر نیستما 😅", "گرفتگی دارم، مرد باش و کمک کن 🔥", "همه‌چی اوکیه، نگران نباش 💚",
    )
    val partnerQuick = listOf(
        "عشقم دوستت دارم 💚", "بغل می‌خوای؟ آماده‌م 🤗", "چای یا قهوه؟ بگو تا بیارم ☕",
        "امشب شام با من، «نه» نداریم 🍝", "حرف بزنی گوشم با توئه 🎧", "حالت چطوره ملکه‌ی من؟ 👑",
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

    fun range(d: LocalDate, v: Int): String =
        if (v <= 0) date(d) else "${date(d.minusDays(v.toLong()))} تا ${date(d.plusDays(v.toLong()))}"

    fun hour(h: Int) = digits("$h:00")
}
