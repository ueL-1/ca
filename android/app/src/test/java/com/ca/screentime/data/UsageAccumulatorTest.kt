package com.ca.screentime.data

import com.ca.screentime.data.UsageAccumulator.Companion.ACTIVITY_PAUSED
import com.ca.screentime.data.UsageAccumulator.Companion.ACTIVITY_RESUMED
import com.ca.screentime.data.UsageAccumulator.Companion.KEYGUARD_HIDDEN
import com.ca.screentime.data.UsageAccumulator.Companion.KEYGUARD_SHOWN
import com.ca.screentime.data.UsageAccumulator.Companion.SCREEN_INTERACTIVE
import com.ca.screentime.data.UsageAccumulator.Companion.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAccumulatorTest {
    private val min = 60_000L
    private val dayStart = 1_000 * min
    private fun acc() = UsageAccumulator(dayStart) { it == "dialer" }

    @Test
    fun countsOnlyWhileScreenOnAndUnlocked() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart + 0)
        a.feed(KEYGUARD_HIDDEN, null, null, dayStart + 1 * min)
        a.feed(ACTIVITY_RESUMED, "youtube", "Main", dayStart + 1 * min)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 31 * min)
        assertEquals(30 * min, a.countedUntil(dayStart + 60 * min, liveActive = false))
    }

    @Test
    fun screenOffIsNotCountedEvenWhenAppKeepsRunning() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart)
        a.feed(ACTIVITY_RESUMED, "music", "Main", dayStart)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 10 * min)
        // 화면이 꺼진 뒤 음악 앱이 다시 RESUMED로 기록되더라도 화면이 꺼져 있으면 세지 않음
        a.feed(ACTIVITY_RESUMED, "music", "Main", dayStart + 20 * min)
        a.feed(ACTIVITY_PAUSED, "music", "Main", dayStart + 50 * min)
        assertEquals(10 * min, a.countedUntil(dayStart + 60 * min, liveActive = true))
    }

    @Test
    fun lockScreenIsNotCounted() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart)
        a.feed(ACTIVITY_RESUMED, "chat", "Main", dayStart)
        a.feed(KEYGUARD_SHOWN, null, null, dayStart + 5 * min)
        a.feed(KEYGUARD_HIDDEN, null, null, dayStart + 15 * min)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 20 * min)
        assertEquals(10 * min, a.countedUntil(dayStart + 30 * min, liveActive = false))
    }

    @Test
    fun phoneAppTimeIsExcluded() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart)
        a.feed(ACTIVITY_RESUMED, "launcher", "Home", dayStart)
        a.feed(ACTIVITY_PAUSED, "launcher", "Home", dayStart + 2 * min)
        a.feed(ACTIVITY_RESUMED, "dialer", "Call", dayStart + 2 * min)
        assertFalse(a.isCounting())
        a.feed(ACTIVITY_PAUSED, "dialer", "Call", dayStart + 12 * min)
        a.feed(ACTIVITY_RESUMED, "launcher", "Home", dayStart + 12 * min)
        assertTrue(a.isCounting())
        assertEquals(5 * min, a.countedUntil(dayStart + 15 * min, liveActive = true))
        assertEquals("launcher", a.foreground)
    }

    @Test
    fun timeBeforeMidnightIsNotCounted() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart - 30 * min)
        a.feed(ACTIVITY_RESUMED, "game", "Main", dayStart - 30 * min)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 10 * min)
        assertEquals(10 * min, a.countedUntil(dayStart + 20 * min, liveActive = false))
    }

    @Test
    fun countsAfterRebootWithoutScreenOnEvent() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 1 * min)
        a.feed(UsageAccumulator.DEVICE_SHUTDOWN, null, null, dayStart + 2 * min)
        a.feed(UsageAccumulator.DEVICE_STARTUP, null, null, dayStart + 5 * min)
        a.feed(ACTIVITY_RESUMED, "launcher", "Home", dayStart + 6 * min)
        a.feed(SCREEN_NON_INTERACTIVE, null, null, dayStart + 9 * min)
        assertEquals(3 * min, a.countedUntil(dayStart + 20 * min, liveActive = false))
    }

    @Test
    fun fallsBackToActivityStateWhenDeviceHasNoScreenEvents() {
        val a = acc()
        a.feed(ACTIVITY_RESUMED, "web", "Main", dayStart)
        a.feed(ACTIVITY_PAUSED, "web", "Main", dayStart + 4 * min)
        assertEquals(4 * min, a.countedUntil(dayStart + 10 * min, liveActive = true))
    }

    @Test
    fun liveTailOnlyWhenScreenIsReallyActive() {
        val a = acc()
        a.feed(SCREEN_INTERACTIVE, null, null, dayStart)
        a.feed(ACTIVITY_RESUMED, "web", "Main", dayStart)
        assertEquals(7 * min, a.countedUntil(dayStart + 7 * min, liveActive = true))
        assertEquals(0L, a.countedUntil(dayStart + 7 * min, liveActive = false))
    }
}
