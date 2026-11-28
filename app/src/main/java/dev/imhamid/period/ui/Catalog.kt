package dev.imhamid.period.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Everything the log form offers. Ids are what gets stored in Supabase; never rename an id. */
class Opt(val id: String, val label: String, val icon: ImageVector)

class Mood(val id: String, val label: String, val icon: ImageVector, val valence: Int)

object Catalog {
    // Flow ids are stored inside `symptoms` as flow_* (older logs may also contain flow_spot).
    val flows = listOf(
        Opt("flow_light", "کم", Icons.Rounded.WaterDrop),
        Opt("flow_medium", "متوسط", Icons.Rounded.WaterDrop),
        Opt("flow_heavy", "زیاد", Icons.Rounded.WaterDrop),
        Opt("flow_disaster", "خیلی زیاد", Icons.Rounded.WaterDrop),
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
        Opt("headache", "سردرد", Icons.Rounded.Psychology),
        Opt("insomnia", "بی‌خوابی", Icons.Rounded.Bedtime),
        Opt("weight_gain", "افزایش وزن", Icons.Rounded.MonitorWeight),
        Opt("mood_changes", "نوسان خلقی", Icons.Rounded.TheaterComedy),
        Opt("irregular", "بی‌نظمی پریود", Icons.Rounded.EventBusy),
        Opt("memory", "مشکل حافظه", Icons.Rounded.Lightbulb),
        Opt("night_sweats", "عرق شبانه", Icons.Rounded.Opacity),
        Opt("hair_thinning", "ریزش مو", Icons.Rounded.ContentCut),
        Opt("tender", "حساسیت سینه", Icons.Rounded.Favorite),
        Opt("vaginal_dryness", "خشکی واژن", Icons.Rounded.Spa),
        Opt("hot_flashes", "گُرگرفتگی", Icons.Rounded.LocalFireDepartment),
        Opt("joint_pain", "درد مفاصل", Icons.Rounded.Accessibility),
        // older ids from the first versions (still shown in history and "more")
        Opt("cramps", "گرفتگی شکم", Icons.Rounded.Bolt),
        Opt("backpain", "کمردرد", Icons.Rounded.AirlineSeatReclineNormal),
        Opt("bloating", "نفخ", Icons.Rounded.BubbleChart),
        Opt("acne", "جوش", Icons.Rounded.RadioButtonChecked),
        Opt("fatigue", "خستگی", Icons.Rounded.BatteryAlert),
        Opt("nausea", "حالت تهوع", Icons.Rounded.SentimentVeryDissatisfied),
        Opt("cravings", "هوس خوراکی", Icons.Rounded.Cake),
        Opt("irritable", "زودرنجی", Icons.Rounded.MoodBad),
        Opt("sad", "غمگینی", Icons.Rounded.SentimentDissatisfied),
    )

    val moods = listOf(
        Mood("normal", "عادی", Icons.Rounded.SentimentNeutral, 3),
        Mood("angry", "عصبانی", Icons.Rounded.MoodBad, 2),
        Mood("happy", "شاد", Icons.Rounded.Mood, 4),
        Mood("sad", "غمگین", Icons.Rounded.SentimentDissatisfied, 1),
        Mood("exhausted", "بی‌حال", Icons.Rounded.BatteryAlert, 2),
        Mood("anxious", "مضطرب", Icons.Rounded.Warning, 2),
        Mood("depressed", "افسرده", Icons.Rounded.Cloud, 1),
        Mood("in_love", "عاشق", Icons.Rounded.Favorite, 5),
        Mood("bored", "بی‌حوصله", Icons.Rounded.HourglassEmpty, 3),
        Mood("confident", "با اعتمادبه‌نفس", Icons.Rounded.EmojiEvents, 5),
        Mood("excited", "هیجان‌زده", Icons.Rounded.Celebration, 5),
        Mood("relaxed", "آرام", Icons.Rounded.Spa, 4),
    )

    val medicines = listOf(
        Opt("condom", "کاندوم", Icons.Rounded.Shield),
        Opt("pill", "قرص", Icons.Rounded.Medication),
        Opt("spermicide", "اسپرم‌کش", Icons.Rounded.Science),
        Opt("ring", "حلقه", Icons.Rounded.RadioButtonUnchecked),
        Opt("iud", "آی‌یو‌دی", Icons.Rounded.Anchor),
        Opt("shot", "آمپول", Icons.Rounded.Vaccines),
        Opt("implant", "ایمپلنت", Icons.Rounded.Straighten),
        Opt("patch", "پچ", Icons.Rounded.Healing),
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

    /** Icon for the old 1..5 mood score. */
    fun legacyMoodIcon(score: Int): ImageVector = when (score) {
        1 -> Icons.Rounded.SentimentVeryDissatisfied
        2 -> Icons.Rounded.SentimentDissatisfied
        3 -> Icons.Rounded.SentimentNeutral
        4 -> Icons.Rounded.Mood
        else -> Icons.Rounded.Celebration
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
