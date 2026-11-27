package dev.imhamid.period.ui

import androidx.compose.ui.graphics.Color

/** Everything the log form offers. Ids are what gets stored in Supabase; never rename an id. */
class Opt(val id: String, val label: String, val emoji: String)

class Mood(val id: String, val label: String, val emoji: String, val valence: Int)

object Catalog {
    // Flow ids are stored inside `symptoms` as flow_* (older logs may also contain flow_spot).
    val flows = listOf(
        Opt("flow_light", "کم", "💧"),
        Opt("flow_medium", "متوسط", "💧"),
        Opt("flow_heavy", "زیاد", "💧"),
        Opt("flow_disaster", "خیلی زیاد", "💧"),
    )

    /** How many drops each flow level shows. */
    fun drops(id: String): Int = when (id) {
        "flow_spot", "flow_light" -> 1
        "flow_medium" -> 2
        "flow_heavy" -> 3
        else -> 4
    }

    /** Older logs used flow_spot; show it as "light". */
    fun normalizeFlow(id: String) = if (id == "flow_spot") "flow_light" else id

    val symptoms = listOf(
        Opt("headache", "سردرد", "🤕"),
        Opt("insomnia", "بی‌خوابی", "😴"),
        Opt("weight_gain", "افزایش وزن", "⚖️"),
        Opt("mood_changes", "نوسان خلقی", "🎭"),
        Opt("irregular", "بی‌نظمی پریود", "📅"),
        Opt("memory", "مشکل حافظه", "🧠"),
        Opt("night_sweats", "عرق شبانه", "💦"),
        Opt("hair_thinning", "ریزش مو", "💇‍♀️"),
        Opt("tender", "حساسیت سینه", "🎀"),
        Opt("vaginal_dryness", "خشکی واژن", "🌵"),
        Opt("hot_flashes", "گُرگرفتگی", "🔥"),
        Opt("joint_pain", "درد مفاصل", "🦴"),
        // older ids from the first versions (still shown in history and "more")
        Opt("cramps", "گرفتگی شکم", "😣"),
        Opt("backpain", "کمردرد", "🧍‍♀️"),
        Opt("bloating", "نفخ", "🎈"),
        Opt("acne", "جوش", "🔴"),
        Opt("fatigue", "خستگی", "🥱"),
        Opt("nausea", "حالت تهوع", "🤢"),
        Opt("cravings", "هوس خوراکی", "🍫"),
        Opt("irritable", "زودرنجی", "😤"),
        Opt("sad", "غمگینی", "🥺"),
    )

    val moods = listOf(
        Mood("normal", "عادی", "😐", 3),
        Mood("angry", "عصبانی", "😡", 2),
        Mood("happy", "شاد", "😊", 4),
        Mood("sad", "غمگین", "😢", 1),
        Mood("exhausted", "بی‌حال", "😵", 2),
        Mood("anxious", "مضطرب", "😰", 2),
        Mood("depressed", "افسرده", "😔", 1),
        Mood("in_love", "عاشق", "😍", 5),
        Mood("bored", "بی‌حوصله", "😒", 3),
        Mood("confident", "با اعتمادبه‌نفس", "😎", 5),
        Mood("excited", "هیجان‌زده", "🤩", 5),
        Mood("relaxed", "آرام", "😌", 4),
    )

    val medicines = listOf(
        Opt("condom", "کاندوم", "🛡️"),
        Opt("pill", "قرص", "💊"),
        Opt("spermicide", "اسپرم‌کش", "🧴"),
        Opt("ring", "حلقه", "⭕"),
        Opt("iud", "آی‌یو‌دی", "⚓"),
        Opt("shot", "آمپول", "💉"),
        Opt("implant", "ایمپلنت", "📏"),
        Opt("patch", "پچ", "🩹"),
    )

    fun symptom(id: String): Opt? = symptoms.firstOrNull { it.id == id }
    fun mood(id: String): Mood? = moods.firstOrNull { it.id == id }
    fun medicine(id: String): Opt? = medicines.firstOrNull { it.id == id }

    /** 1..5 score kept on the server (partner notifications and charts) = average valence of the picked moods. */
    fun valenceOf(ids: List<String>): Int? {
        val v = ids.mapNotNull { mood(it)?.valence }
        if (v.isEmpty()) return null
        return Math.round(v.average()).toInt().coerceIn(1, 5)
    }

    fun moodWord(score: Int): String = when (score) {
        1 -> "خیلی بد"; 2 -> "بد"; 3 -> "معمولی"; 4 -> "خوب"; else -> "عالی"
    }
}

/** Body mass index. Ranges follow the WHO classes used in the design. */
object Bmi {
    class Band(val label: String, val from: Double, val to: Double, val color: Color)

    val bands = listOf(
        Band("کم‌وزنی شدید", 0.0, 16.0, Color(0xFF2196F3)),
        Band("کم‌وزنی متوسط", 16.0, 17.0, Color(0xFF29B6F6)),
        Band("کم‌وزن", 17.0, 18.5, Color(0xFF00BCD4)),
        Band("نرمال", 18.5, 25.0, Color(0xFF4CAF50)),
        Band("اضافه‌وزن", 25.0, 30.0, Color(0xFFFFC107)),
        Band("چاقی درجه ۱", 30.0, 35.0, Color(0xFFFF9800)),
        Band("چاقی درجه ۲", 35.0, 40.0, Color(0xFFFF5722)),
        Band("چاقی درجه ۳", 40.0, 1000.0, Color(0xFFE53935)),
    )

    fun of(weightKg: Double?, heightCm: Double?): Double? {
        if (weightKg == null || heightCm == null || heightCm < 80 || weightKg < 20) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun band(bmi: Double): Band = bands.firstOrNull { bmi >= it.from && bmi < it.to } ?: bands.last()
}
