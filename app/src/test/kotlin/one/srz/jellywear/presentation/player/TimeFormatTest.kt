package one.srz.jellywear.presentation.player

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for the player's "m:ss" position/duration formatting.
 *
 * The production code formats with the default locale (matching what the
 * watch shows in the user's language); pin it to a known value here so the
 * expected digit strings are stable regardless of the CI machine's locale.
 */
class TimeFormatTest {

    private lateinit var originalLocale: Locale

    @Before
    fun pinLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `zero formats as 0-00`() {
        assertEquals("0:00", formatMillis(0))
    }

    @Test
    fun `sub-second remainder is floored, not rounded`() {
        assertEquals("0:00", formatMillis(999))
        assertEquals("0:01", formatMillis(1_999))
    }

    @Test
    fun `seconds are always two digits`() {
        assertEquals("0:01", formatMillis(1_000))
        assertEquals("0:09", formatMillis(9_000))
        assertEquals("0:59", formatMillis(59_999))
    }

    @Test
    fun `minute rollover at exactly 60 seconds`() {
        assertEquals("1:00", formatMillis(60_000))
        assertEquals("1:01", formatMillis(61_500))
    }

    @Test
    fun `double-digit minutes`() {
        assertEquals("10:05", formatMillis(605_000))
    }

    @Test
    fun `minutes keep counting past an hour -- current no-hours behavior`() {
        // Documents the status quo (issue #26 tracks adding an hours
        // segment): a 2-hour movie reads "120:00", not "2:00:00".
        assertEquals("61:01", formatMillis(3_661_000))
        assertEquals("120:00", formatMillis(7_200_000))
    }
}
