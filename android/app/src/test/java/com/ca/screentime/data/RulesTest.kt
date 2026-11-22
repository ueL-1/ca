package com.ca.screentime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val min = 60_000L

    @Test
    fun normalThenOver() {
        assertEquals(Mode.NORMAL, LimitRules.evaluate(179 * min, 180, null, false).mode)
        val over = LimitRules.evaluate(180 * min, 180, null, false)
        assertEquals(Mode.OVER, over.mode)
        assertTrue(over.exceeded)
    }

    @Test
    fun extraTenMinutesCountsRealUsageOnly() {
        val start = 181 * min
        assertEquals(Mode.EXTRA_ACTIVE, LimitRules.evaluate(start, 180, start, false).mode)
        val mid = LimitRules.evaluate(start + 4 * min, 180, start, false)
        assertEquals(Mode.EXTRA_ACTIVE, mid.mode)
        assertEquals(6 * min, mid.extraRemainingMs)
        assertEquals(Mode.OVER, LimitRules.evaluate(start + 10 * min, 180, start, false).mode)
    }

    @Test
    fun weekUnlockOverridesEverything() {
        assertEquals(Mode.WEEK_UNLOCKED, LimitRules.evaluate(500 * min, 180, null, true).mode)
    }

    @Test
    fun weekRunsMondayToSunday() {
        val wed = LocalDate.of(2026, 11, 25)
        val sun = LocalDate.of(2026, 11, 29)
        val mon = LocalDate.of(2026, 11, 30)
        assertEquals(LocalDate.of(2026, 11, 23), TimeRules.weekStart(wed))
        assertEquals(LocalDate.of(2026, 11, 23), TimeRules.weekStart(sun))
        assertEquals(mon, TimeRules.weekStart(mon))
        assertEquals(sun, TimeRules.weekLastDay(wed))
        assertEquals(mon, TimeRules.nextWeekStart(sun))
        assertFalse(TimeRules.weekStart(sun) == TimeRules.weekStart(mon))
    }

    @Test
    fun wordsAndTexts() {
        assertEquals("3시간", TimeRules.words(180))
        assertEquals("1시간 18분", TimeRules.words(78))
        assertEquals("45분", TimeRules.words(45))
        assertEquals("2:30", TimeRules.clock(150))
        assertEquals("11월 25일 수요일", TimeRules.longText(LocalDate.of(2026, 11, 25)))
        assertEquals("11월 29일(일)", TimeRules.shortText(LocalDate.of(2026, 11, 29)))
    }
}
