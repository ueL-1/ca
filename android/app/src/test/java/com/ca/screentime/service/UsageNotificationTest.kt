package com.ca.screentime.service

import android.app.Notification
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.ca.screentime.R
import com.ca.screentime.data.LimitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/** 상단 알림의 사용시간 막대(진행 표시줄)와 문구 확인. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UsageNotificationTest {
    private val min = 60_000L
    private val wed = LocalDate.of(2026, 11, 25)

    @Test
    fun normalShowsUsedOverLimitBar() {
        val c = UsageNotification.content(wed, LimitRules.evaluate(102 * min, 180, null, false))
        assertEquals("오늘 1시간 42분 사용", c.title)
        assertEquals("한도 3시간 · 남은 시간 1시간 18분", c.text)
        assertEquals(180, c.progressMax)
        assertEquals(102, c.progress)
    }

    @Test
    fun overLimitShowsFullBar() {
        val c = UsageNotification.content(wed, LimitRules.evaluate(181 * min, 180, null, false))
        assertEquals("오늘 한도 초과", c.title)
        assertEquals(c.progressMax, c.progress)
    }

    @Test
    fun extraTenMinutesShowsItsOwnBar() {
        val start = 180 * min
        val c = UsageNotification.content(wed, LimitRules.evaluate(start + 3 * min, 180, start, false))
        assertEquals("딱 10분! 사용 중", c.title)
        assertEquals("남은 시간 7분 · 끝나면 다시 제한돼요", c.text)
        assertEquals(10, c.progressMax)
        assertEquals(3, c.progress)
    }

    @Test
    fun weekUnlockedShowsUsageClampedToLimit() {
        val c = UsageNotification.content(wed, LimitRules.evaluate(205 * min, 180, null, true))
        assertEquals("이번 주 제한 해제 중", c.title)
        assertEquals("오늘 3시간 25분 사용 · 11월 29일(일) 24:00까지", c.text)
        assertEquals(180, c.progressMax)
        assertEquals(180, c.progress)
    }

    @Test
    fun zeroLimitStillHasValidBar() {
        val c = UsageNotification.content(wed, LimitRules.evaluate(0L, 0, null, false))
        assertTrue(c.progressMax > 0)
        assertEquals(c.progressMax, c.progress)
    }

    @Test
    fun builtNotificationUsesOnlyTheAppBar() {
        val context = RuntimeEnvironment.getApplication()
        val c = UsageNotification.content(wed, LimitRules.evaluate(102 * min, 180, null, false))
        val n = UsageNotification.build(context, c)
        // 시스템 기본 막대는 쓰지 않는다 (앱 막대와 두 개가 겹쳐 보이므로)
        assertEquals(0, n.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertFalse(n.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertEquals("오늘 1시간 42분 사용", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(0xFFD97757.toInt(), n.color)
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
    }

    @Test
    fun barIsVisibleInBothCollapsedAndExpandedNotification() {
        val context = RuntimeEnvironment.getApplication()
        val c = UsageNotification.content(wed, LimitRules.evaluate(102 * min, 180, null, false))
        val builder = Notification.Builder.recoverBuilder(context, UsageNotification.build(context, c))
        for (remote in listOf(builder.createContentView(), builder.createBigContentView())) {
            val view = remote.apply(context, FrameLayout(context))
            val bar = view.findViewById<ProgressBar>(R.id.bar)
            assertEquals(180, bar.max)
            assertEquals(102, bar.progress)
            assertEquals("오늘 1시간 42분 사용", view.findViewById<TextView>(R.id.title).text.toString())
        }
        val big = builder.createBigContentView().apply(context, FrameLayout(context))
        assertEquals("한도 3시간 · 남은 시간 1시간 18분", big.findViewById<TextView>(R.id.text).text.toString())
    }

    @Test
    fun waitingNotificationHasNoBar() {
        val context = RuntimeEnvironment.getApplication()
        val n = UsageNotification.build(context, UsageNotification.waiting(context, "사용정보 접근 권한이 필요해요"))
        assertEquals(0, n.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertEquals("나 사실 시간관리 고수", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }
}
