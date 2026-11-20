package dev.imhamid.period.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun `newer versions are detected numerically`() {
        assertTrue(Updater.isNewer("1.2.0", "1.1.9"))
        assertTrue(Updater.isNewer("v1.0.10", "1.0.9")) // not a text comparison
        assertTrue(Updater.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun `same or older versions are not updates`() {
        assertFalse(Updater.isNewer("1.0.0", "1.0.0"))
        assertFalse(Updater.isNewer("1.0", "1.0.0"))
        assertFalse(Updater.isNewer("0.9.9", "1.0.0"))
        assertFalse(Updater.isNewer("0.0.0-dev", "1.0.0"))
    }
}
