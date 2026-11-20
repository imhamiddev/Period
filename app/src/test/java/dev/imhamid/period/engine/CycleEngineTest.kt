package dev.imhamid.period.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    /** Every day of a 5-day period starting on each of [starts]. */
    private fun bleed(vararg starts: LocalDate) = starts.flatMap { s -> (0 until 5).map { s.plusDays(it.toLong()) } }

    /** Three regular 28-day cycles: Jan 1, Jan 29 and Feb 26. */
    private val regular = bleed(d(1, 1), d(1, 29), d(2, 26))

    private fun engine(today: LocalDate, hormonal: Boolean = false, log: List<LocalDate> = regular) =
        CycleEngine(today, log, hormonal = hormonal)

    // ---------------------------------------------------------------- periods and blending
    @Test
    fun `bleeding days with a gap of up to two days form one period`() {
        val p = CycleEngine.derivePeriods(listOf(d(1, 1), d(1, 2), d(1, 3), d(1, 6), d(1, 10)))
        assertEquals(2, p.size)
        assertEquals(d(1, 1), p[0].start)
        assertEquals(d(1, 6), p[0].end)
        assertEquals(6, p[0].length)
        assertEquals(d(1, 10), p[1].start)
    }

    @Test
    fun `blend falls back to the configured value without data`() {
        assertEquals(28, CycleEngine.blend(emptyList(), 28))
    }

    @Test
    fun `blend leans on the default while fewer than three cycles are known`() {
        assertEquals(29, CycleEngine.blend(listOf(30), 28))
        assertEquals(28, CycleEngine.blend(listOf(26, 30), 28))
    }

    @Test
    fun `blend weights recent cycles more and only uses the last six`() {
        assertEquals(30, CycleEngine.blend(listOf(30, 30, 30), 28))
        assertEquals(33, CycleEngine.blend(listOf(20, 30, 40), 28)) // plain mean would be 30
        assertEquals(28, CycleEngine.blend(listOf(100) + List(6) { 28 }, 28))
    }

    @Test
    fun `gaps longer than 90 days are not counted as a cycle`() {
        val log = bleed(d(1, 1), d(1, 29), LocalDate.of(2026, 5, 9)) // 28 days, then 100 days
        val e = engine(LocalDate.of(2026, 5, 12), log = log)
        assertEquals(listOf(28), e.cycleLengths)
        assertEquals(28, e.predictedCycle)
    }

    // ---------------------------------------------------------------- overview
    @Test
    fun `no data means no overview`() {
        val o = CycleEngine(d(3, 10), emptyList()).overview()
        assertFalse(o.hasData)
        assertNull(o.nextPeriod)
    }

    @Test
    fun `regular cycles predict the next period and the fertile window`() {
        val e = engine(d(3, 10))
        assertEquals(28, e.predictedCycle)
        val o = e.overview()
        assertEquals(Phase.FERTILE, o.phase)
        assertEquals(13, o.cycleDay)
        assertEquals(d(3, 26), o.nextPeriod)
        assertEquals(16, o.daysToPeriod)
        assertEquals(d(3, 12), o.ovulation)
        assertEquals(2, o.daysToOvulation)
        assertEquals(11, o.daysToPms)
        assertFalse(o.inPms)
    }

    @Test
    fun `hormonal contraception hides ovulation and the fertile window`() {
        val o = engine(d(3, 10), hormonal = true).overview()
        assertNull(o.ovulation)
        assertNull(o.daysToOvulation)
        assertEquals(Phase.FOLLICULAR, o.phase)
    }

    // ---------------------------------------------------------------- notification events
    @Test
    fun `ovulation day fires the ovulation event`() {
        assertTrue(EventType.OVULATION in engine(d(3, 12)).dueEvents())
    }

    @Test
    fun `pms events fire one day before and on the first pms day`() {
        assertEquals(listOf(EventType.PMS_TOMORROW), engine(d(3, 20)).dueEvents())
        assertEquals(listOf(EventType.PMS_STARTS), engine(d(3, 21)).dueEvents())
    }

    @Test
    fun `period soon fires two days ahead and period today on the expected day`() {
        assertEquals(listOf(EventType.PERIOD_SOON), engine(d(3, 24)).dueEvents())
        assertEquals(emptyList<EventType>(), engine(d(3, 25)).dueEvents())
        assertEquals(listOf(EventType.PERIOD_TODAY), engine(d(3, 26)).dueEvents())
    }

    @Test
    fun `late events fire after two and after five days`() {
        assertEquals(listOf(EventType.PERIOD_LATE), engine(d(3, 28)).dueEvents())
        assertEquals(emptyList<EventType>(), engine(d(3, 29)).dueEvents())
        assertEquals(listOf(EventType.PERIOD_LATE), engine(d(3, 31)).dueEvents())
    }

    @Test
    fun `no events without data`() {
        assertTrue(CycleEngine(d(3, 10), emptyList()).dueEvents().isEmpty())
    }
}
