package dev.imhamid.period.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import dev.imhamid.period.R
import dev.imhamid.period.data.Snapshot
import dev.imhamid.period.engine.CycleEngine
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** One-page "doctor report" PDF drawn with Android's PdfDocument (same layout as the sample the user approved). */
object Report {
    private const val W = 595f
    private const val H = 842f
    private const val L = 36f
    private const val RE = W - 36f

    private const val PINK = 0xFFE8457C.toInt()
    private const val SOFT = 0xFFFDE6EE.toInt()
    private const val INK = 0xFF212121.toInt()
    private const val MUTED = 0xFF8A8A8A.toInt()
    private const val LINE = 0xFFECECEC.toInt()
    private const val BG = 0xFFF7F7F7.toInt()
    private const val GREEN = 0xFF4CAF50.toInt()
    private const val ORANGE = 0xFFFF9F1C.toInt()
    private const val BLUE = 0xFF2196F3.toInt()
    private const val YEL = 0xFFFFC02D.toInt()

    private fun y(bottomOrigin: Float) = H - bottomOrigin

    fun build(ctx: Context, s: Snapshot, days: Int): File {
        val today = LocalDate.now()
        val since = today.minusDays(days.toLong())
        val owner = s.owner
        val reg = ResourcesCompat.getFont(ctx, R.font.vazirmatn_regular) ?: Typeface.DEFAULT
        val med = ResourcesCompat.getFont(ctx, R.font.vazirmatn_medium) ?: Typeface.DEFAULT
        val bold = ResourcesCompat.getFont(ctx, R.font.vazirmatn_bold) ?: Typeface.DEFAULT_BOLD

        fun paint(tf: Typeface, size: Float, color: Int, align: Paint.Align = Paint.Align.RIGHT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; this.color = color; textAlign = align }
        fun fill(color: Int, style: Paint.Style = Paint.Style.FILL, width: Float = 1f) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.style = style; strokeWidth = width }

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas

        fun text(t: String, x: Float, yb: Float, p: Paint) = c.drawText(Fa.digits(t), x, y(yb), p)
        fun rrect(x: Float, yb: Float, w: Float, h: Float, r: Float, p: Paint) = c.drawRoundRect(RectF(x, y(yb + h), x + w, y(yb)), r, r, p)
        fun card(x: Float, yb: Float, w: Float, h: Float, bg: Int = 0xFFFFFFFF.toInt(), stroke: Int = LINE) {
            rrect(x, yb, w, h, 10f, fill(bg))
            rrect(x, yb, w, h, 10f, fill(stroke, Paint.Style.STROKE, 0.8f))
        }

        // ---------------- data
        val periods = CycleEngine.derivePeriods(s.logs).sortedBy { it.start }
        val inRange = periods.filter { !it.start.isBefore(since.minusDays(40)) }
        val gaps = Insights.cycleGaps(s.logs).takeLast(6)
        val rangeGaps = periods.zipWithNext { a, b -> a to ChronoUnit.DAYS.between(a.start, b.start).toInt() }
            .filter { !it.first.start.isBefore(since) && it.second in 10..90 }.map { it.second }
        val avgCycle = (rangeGaps.ifEmpty { gaps }).takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val avgPeriod = periods.filter { !it.start.isBefore(since) }.ifEmpty { periods.takeLast(3) }.takeIf { it.isNotEmpty() }?.map { it.length }?.average()?.roundToInt()
        val cycleCount = periods.count { !it.start.isBefore(since) }
        val minMax = (rangeGaps.ifEmpty { gaps }).takeIf { it.isNotEmpty() }?.let { "${Fa.n(it.min())} تا ${Fa.n(it.max())} روز" } ?: "—"
        val body = s.body
        val latestKg = s.health.entries.filter { it.value.weightKg != null }.maxByOrNull { it.key }?.value?.weightKg ?: body?.weightKg
        val bmi = Bmi.of(latestKg, body?.heightCm)
        val age = body?.birthDate?.let { runCatching { ChronoUnit.YEARS.between(LocalDate.parse(it), today).toInt() }.getOrNull() }
        val dayLogs = s.days.values.filter { dl -> runCatching { !LocalDate.parse(dl.day).isBefore(since) }.getOrDefault(false) }
        val symCounts = dayLogs.flatMap { Fa.realSymptoms(it.symptoms) }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
        val moodCounts = dayLogs.flatMap { it.moods }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
        val healthIn = s.health.filter { !it.key.isBefore(since) && !it.key.isAfter(today) }.toSortedMap()
        val weights = healthIn.values.mapNotNull { it.weightKg }
        val temps = healthIn.values.mapNotNull { it.temperatureC }
        val meds = healthIn.values.flatMap { it.medicines }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val flags = Insights.flags(s, today)

        // ---------------- header
        c.drawRect(0f, 0f, W, 96f, fill(PINK))
        c.drawCircle(RE - 18, 48f, 18f, fill(0xFFFFFFFF.toInt()))
        val drop = Path().apply {
            val cx = RE - 18; val cy = 48f
            moveTo(cx, cy - 11); cubicTo(cx + 3, cy - 5, cx + 8, cy - 1, cx + 8, cy + 3); cubicTo(cx + 8, cy + 8, cx + 4, cy + 10, cx, cy + 10)
            cubicTo(cx - 4, cy + 10, cx - 8, cy + 8, cx - 8, cy + 3); cubicTo(cx - 8, cy - 1, cx - 3, cy - 5, cx, cy - 11); close()
        }
        c.drawPath(drop, fill(PINK))
        val white = 0xFFFFFFFF.toInt()
        val pale = 0xFFFFD3E2.toInt()
        text("گزارش چرخه‌ی قاعدگی", RE - 46, H - 44, paint(bold, 20f, white))
        text("تهیه‌شده با لونا · برای ارائه به پزشک", RE - 46, H - 64, paint(reg, 10f, pale))
        text(owner?.name.orEmpty().ifBlank { "—" }, L, H - 44, paint(bold, 13f, white, Paint.Align.LEFT))
        text("بازه‌ی گزارش: ${Fa.date(since, "d MMMM yyyy")} تا ${Fa.date(today, "d MMMM yyyy")}", L, H - 62, paint(reg, 9.5f, pale, Paint.Align.LEFT))
        text("تاریخ تهیه: ${Fa.date(today, "d MMMM yyyy")}", L, H - 78, paint(reg, 9.5f, pale, Paint.Align.LEFT))

        // ---------------- patient row
        var yb = H - 130
        card(L, yb - 34, RE - L, 40f, BG, BG)
        val items = listOf(
            "سن" to (age?.let { "${Fa.n(it)} سال" } ?: "—"),
            "قد" to (body?.heightCm?.let { "${Fa.digits(fmt1(it))} سانتی‌متر" } ?: "—"),
            "وزن" to (latestKg?.let { "${Fa.digits(fmt1(it))} کیلوگرم" } ?: "—"),
            "BMI" to (bmi?.let { "${Fa.digits(fmt1(Math.round(it * 10) / 10.0))} · ${Bmi.band(it).label}" } ?: "—"),
        )
        val colW = (RE - L) / 4
        items.forEachIndexed { i, (k, v) ->
            val x = RE - 12 - i * colW
            text(k, x, yb - 8, paint(reg, 8.5f, MUTED)); text(v, x, yb - 24, paint(med, 11f, INK))
        }

        // ---------------- summary cards
        yb = H - 190
        val stats = listOf(
            Triple("میانگین چرخه", avgCycle?.let { "${Fa.n(it)} روز" } ?: "—", PINK),
            Triple("میانگین پریود", avgPeriod?.let { "${Fa.n(it)} روز" } ?: "—", PINK),
            Triple("تعداد چرخه‌ی ثبت‌شده", Fa.n(cycleCount), BLUE),
            Triple("بازه‌ی طول چرخه", minMax, ORANGE),
        )
        val gap = 8f
        val cw = (RE - L - gap * 3) / 4
        stats.forEachIndexed { i, (k, v, col) ->
            val x = RE - (i + 1) * cw - i * gap
            card(x, yb - 56, cw, 62f)
            rrect(x + cw - 5, yb - 56 + 10, 3f, 42f, 1.5f, fill(col))
            text(k, x + cw - 14, yb - 16, paint(reg, 8.5f, MUTED)); text(v, x + cw - 14, yb - 40, paint(bold, 15f, INK))
        }

        // ---------------- cycle history
        yb = H - 270
        text("تاریخچه‌ی چرخه‌ها", RE, yb, paint(bold, 13f, INK))
        c.drawLine(L, y(yb - 8), RE, y(yb - 8), fill(LINE, Paint.Style.STROKE, 1f))
        val hy = yb - 26
        text("شروع پریود", RE, hy, paint(reg, 8.5f, MUTED)); text("طول پریود", RE - 95, hy, paint(reg, 8.5f, MUTED))
        text("طول چرخه", RE - 175, hy, paint(reg, 8.5f, MUTED)); text("نمای چرخه", RE - 255, hy, paint(reg, 8.5f, MUTED))
        val shown = inRange.takeLast(4).reversed()
        if (shown.isEmpty()) text("هنوز پریودی ثبت نشده.", RE, hy - 22, paint(reg, 10f, MUTED))
        val luteal = owner?.lutealLength ?: 14
        shown.forEachIndexed { i, p ->
            val ry = hy - 22 - i * 24
            val next = periods.getOrNull(periods.indexOf(p) + 1)
            val cl = next?.let { ChronoUnit.DAYS.between(p.start, it.start).toInt() }
            text(Fa.date(p.start, "d MMMM"), RE, ry, paint(med, 10f, INK))
            text("${Fa.n(p.length)} روز", RE - 95, ry, paint(reg, 10f, INK))
            text(if (cl != null) "${Fa.n(cl)} روز" else "در جریان", RE - 175, ry, paint(reg, 10f, INK))
            val bw = RE - 255 - L - 60 + 60
            val tot = (cl ?: owner?.cycleLength ?: 28).toFloat()
            rrect(L, ry - 3, bw, 9f, 4.5f, fill(0xFFEFEFEF.toInt()))
            rrect(L + bw * (1 - p.length / tot), ry - 3, bw * p.length / tot, 9f, 4.5f, fill(PINK))
            if (cl != null) {
                val ov = cl - luteal
                val fs = (ov - 5) / tot; val fe = (ov + 1) / tot
                if (fe > fs) rrect(L + bw * (1 - fe), ry - 3, bw * (fe - fs), 9f, 4.5f, fill(YEL))
            }
        }
        val ly = hy - 22 - 4 * 24 - 6
        c.drawCircle(RE - 4, y(ly), 3f, fill(PINK)); text("پریود", RE - 12, ly - 3, paint(reg, 8f, MUTED))
        c.drawCircle(RE - 60, y(ly), 3f, fill(YEL)); text("پنجره‌ی باروری", RE - 68, ly - 3, paint(reg, 8f, MUTED))

        // ---------------- symptoms / moods
        yb = H - 436
        val half = (RE - L - 14) / 2
        fun bars(x0: Float, title: String, data: List<Pair<String, Int>>, col: Int) {
            card(x0, yb - 146, half, 152f)
            text(title, x0 + half - 14, yb - 18, paint(bold, 11.5f, INK))
            if (data.isEmpty()) { text("در این بازه چیزی ثبت نشده.", x0 + half - 14, yb - 44, paint(reg, 9.5f, MUTED)); return }
            val mx = data.maxOf { it.second }
            data.forEachIndexed { i, (k, v) ->
                val ry = yb - 40 - i * 19
                text(k, x0 + half - 14, ry, paint(reg, 9.5f, INK))
                val full = half - 130
                val bw = full * v / mx
                rrect(x0 + 14, ry - 3, full, 8f, 4f, fill(0xFFF1F1F1.toInt()))
                rrect(x0 + 14 + full - bw, ry - 3, bw, 8f, 4f, fill(col))
                text(Fa.n(v), x0 + 14 + full + 6, ry - 1, paint(med, 9f, MUTED, Paint.Align.LEFT))
            }
        }
        bars(RE - half, "علائم پرتکرار", symCounts.map { (Catalog.symptom(it.key)?.label ?: Fa.symptomName(it.key)) to it.value }, PINK)
        bars(L, "حال‌وهوا", moodCounts.map { (Catalog.mood(it.key)?.label ?: it.key) to it.value }, BLUE)

        // ---------------- health charts
        yb = H - 598
        text("سلامت در ${Fa.n(days)} روز اخیر", RE, yb, paint(bold, 13f, INK))
        c.drawLine(L, y(yb - 8), RE, y(yb - 8), fill(LINE, Paint.Style.STROKE, 1f))
        fun chart(x0: Float, title: String, vals: List<Double>, col: Int, last: String) {
            val w = half; val h = 100f
            card(x0, yb - 24 - h, w, h + 4)
            text(title, x0 + w - 14, yb - 40, paint(med, 10.5f, INK))
            if (vals.size < 2) { text("داده‌ی کافی ثبت نشده.", x0 + w - 14, yb - 70, paint(reg, 9.5f, MUTED)); return }
            text(last, x0 + 14, yb - 40, paint(bold, 12f, col, Paint.Align.LEFT))
            val gx0 = x0 + 14; val gx1 = x0 + w - 14; val gy0 = yb - 24 - h + 16; val gy1 = yb - 56
            val lo = vals.min() - 0.3; val hi = (vals.max() + 0.3).coerceAtLeast(lo + 0.5)
            for (k in 0..2) { val yy = gy0 + (gy1 - gy0) * k / 2; c.drawLine(gx0, y(yy), gx1, y(yy), fill(LINE, Paint.Style.STROKE, 0.5f)) }
            val pts = vals.mapIndexed { i, v -> (gx0 + (gx1 - gx0) * i / (vals.size - 1)) to (gy0 + (gy1 - gy0) * ((v - lo) / (hi - lo)).toFloat()) }
            val area = Path().apply { moveTo(pts.first().first, y(gy0)); pts.forEach { lineTo(it.first, y(it.second)) }; lineTo(pts.last().first, y(gy0)); close() }
            c.drawPath(area, fill((col and 0x00FFFFFF) or 0x24000000))
            val line = Path().apply { moveTo(pts.first().first, y(pts.first().second)); pts.drop(1).forEach { lineTo(it.first, y(it.second)) } }
            c.drawPath(line, fill(col, Paint.Style.STROKE, 1.6f))
            pts.filterIndexed { i, _ -> i % 3 == 0 }.forEach { c.drawCircle(it.first, y(it.second), 2.4f, fill(0xFFFFFFFF.toInt())); c.drawCircle(it.first, y(it.second), 1.5f, fill(col)) }
            text("ابتدای بازه", gx0 + 0f, gy0 - 11, paint(reg, 7.5f, MUTED, Paint.Align.LEFT)); text("امروز", gx1, gy0 - 11, paint(reg, 7.5f, MUTED))
        }
        chart(RE - half, "وزن، کیلوگرم", weights, GREEN, weights.lastOrNull()?.let { Fa.digits(fmt1(it)) }.orEmpty())
        chart(L, "دمای صبحگاهی، درجه‌ی سانتی‌گراد", temps, ORANGE, temps.lastOrNull()?.let { Fa.digits(fmt1(it)) }.orEmpty())

        // ---------------- contraception + notes
        yb = H - 744
        card(RE - half, yb - 52, half, 58f)
        text("پیشگیری و دارو", RE - 14, yb - 18, paint(bold, 11f, INK))
        text(meds.take(4).joinToString("، ") { "${Catalog.medicine(it.key)?.label ?: it.key} ${Fa.n(it.value)} بار" }.ifBlank { "در این بازه چیزی ثبت نشده." }, RE - 14, yb - 38, paint(reg, 9.5f, MUTED))
        card(L, yb - 52, half, 58f, SOFT, SOFT)
        text("نکات قابل توجه", L + half - 14, yb - 18, paint(bold, 11f, PINK))
        text(flags.firstOrNull()?.let { "${it.title}: ${it.text.substringBefore(". ").trim()}" }?.take(70) ?: "چرخه‌ها منظم‌ان؛ مورد هشداری ثبت نشده.", L + half - 14, yb - 38, paint(reg, 9.5f, INK))
        if (flags.size > 1) text("و ${Fa.n(flags.size - 1)} مورد دیگه", L + half - 14, yb - 50, paint(reg, 8.5f, MUTED))

        // ---------------- footer
        c.drawLine(L, y(40f), RE, y(40f), fill(LINE, Paint.Style.STROKE, 1f))
        text("این گزارش بر پایه‌ی ثبت‌های خود کاربر ساخته شده و تشخیص پزشکی نیست.", RE, 26f, paint(reg, 8f, MUTED))
        text("لونا", L, 26f, paint(reg, 8f, MUTED, Paint.Align.LEFT))

        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "luna-report-${today}.pdf")
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }

    fun share(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "گزارش چرخه‌ی قاعدگی")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری گزارش").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
