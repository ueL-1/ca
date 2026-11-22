package com.ca.screentime.ui

import android.content.Context
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ca.screentime.data.Engine
import com.ca.screentime.data.LimitRules
import com.ca.screentime.data.Store
import com.ca.screentime.data.UsageTracker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

/**
 * 메인 화면에서 요일을 바꾼 뒤 시간을 바꾸면, 화면에 선택된 그 요일의 한도만 바뀌어야 한다.
 * (버그: 화요일을 선택하고 바꿔도 처음 선택돼 있던 오늘 요일(월요일)만 바뀌던 문제)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-port-xxhdpi")
class DayLimitEditTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var engine: Engine
    private val monday = LocalDate.of(2026, 11, 23)

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("screen_time", Context.MODE_PRIVATE).edit().clear().commit()
        engine = Engine(app)
    }

    private fun showMainScreenOn(day: LocalDate) {
        val eval = LimitRules.evaluate(30 * 60_000L, engine.store.limitMinutes(day.dayOfWeek), null, false)
        val status = Engine.Status(day, eval, UsageTracker.Snapshot(day, 30 * 60_000L, "app", true, false), true)
        compose.setContent {
            MainScreen(status, PermissionState(true, true, true, true, false), engine, {})
        }
    }

    private fun selectDay(name: String) {
        compose.onNode(hasContentDescription(name, substring = true)).performScrollTo().performClick()
        compose.onNodeWithText("$name 하루 최대 사용시간").assertExists()
    }

    private fun press(button: String) {
        compose.onNodeWithContentDescription(button).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun limit(day: DayOfWeek) = engine.store.limitMinutes(day)

    @Test
    fun changingTuesdayChangesOnlyTuesday() {
        showMainScreenOn(monday) // 오늘 = 월요일, 처음 선택 = 월요일
        selectDay("화요일")
        press("1시간 늘리기")
        compose.waitForIdle()

        assertEquals("화요일이 바뀌어야 함", Store.DEFAULT_LIMIT_MINUTES + 60, limit(TUESDAY))
        assertEquals("월요일은 그대로여야 함", Store.DEFAULT_LIMIT_MINUTES, limit(MONDAY))
        compose.onNodeWithText("화요일 한도를 4시간으로 저장했어요").assertExists()
    }

    @Test
    fun switchingDaysRepeatedlyAlwaysEditsTheSelectedDay() {
        showMainScreenOn(monday)
        selectDay("화요일")
        press("5분 늘리기")
        selectDay("수요일")
        press("1시간 줄이기")
        selectDay("월요일")
        press("1시간 늘리기")
        compose.waitForIdle()

        assertEquals(Store.DEFAULT_LIMIT_MINUTES + 5, limit(TUESDAY))
        assertEquals(Store.DEFAULT_LIMIT_MINUTES - 60, limit(WEDNESDAY))
        assertEquals(Store.DEFAULT_LIMIT_MINUTES + 60, limit(MONDAY))
        // 화면의 요일 버튼에도 각 요일 값이 맞게 표시되는지 확인
        compose.onNode(hasContentDescription("화요일 최대 3시간 5분")).assertExists()
        compose.onNode(hasContentDescription("수요일 최대 2시간")).assertExists()
        compose.onNode(hasContentDescription("월요일 최대 4시간, 오늘")).assertExists()
        DayOfWeek.entries.filter { it !in setOf(MONDAY, TUESDAY, WEDNESDAY) }.forEach {
            assertEquals("$it 은 그대로여야 함", Store.DEFAULT_LIMIT_MINUTES, limit(it))
        }
    }
}
