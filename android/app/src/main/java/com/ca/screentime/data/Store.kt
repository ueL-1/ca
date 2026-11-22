package com.ca.screentime.data

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 기기 내부 저장소(SharedPreferences). 앱 종료·재부팅 후에도 유지된다.
 * 날짜·주 단위 값은 "어느 날짜/주의 값인지"를 함께 저장해서, 날짜나 주가 바뀌면 자동으로 초기화된 것으로 본다.
 */
class Store(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("screen_time", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_LIMIT_MINUTES = 180
        const val MAX_LIMIT_MINUTES = 23 * 60 + 55
    }

    // ---- 요일별 최대 사용시간 (계속 유지) ----
    fun limitMinutes(day: DayOfWeek): Int = p.getInt("limit_${day.value}", DEFAULT_LIMIT_MINUTES)

    fun setLimitMinutes(day: DayOfWeek, minutes: Int) {
        p.edit().putInt("limit_${day.value}", minutes.coerceIn(0, MAX_LIMIT_MINUTES)).commit()
    }

    // ---- 딱 10분! (하루 1회, 날짜가 바뀌면 초기화) ----
    fun extraStartUsage(date: LocalDate): Long? =
        if (p.getString("extra_date", null) == date.toString()) p.getLong("extra_start", 0L) else null

    fun useExtra(date: LocalDate, usageNowMs: Long): Boolean {
        if (extraStartUsage(date) != null) return false
        return p.edit().putString("extra_date", date.toString()).putLong("extra_start", usageNowMs).commit()
    }

    // ---- 이번 주 제한 해제 (주 1회, 월요일 00:00에 초기화) ----
    fun weekUnlocked(date: LocalDate): Boolean =
        p.getString("week_unlock", null) == TimeRules.weekStart(date).toString()

    fun useWeekUnlock(date: LocalDate): Boolean {
        if (weekUnlocked(date)) return false
        return p.edit().putString("week_unlock", TimeRules.weekStart(date).toString()).commit()
    }

    // ---- 초과 화면이 떠 있던 시간 (사용시간에서 제외) ----
    fun blockedMs(date: LocalDate): Long =
        if (p.getString("blocked_date", null) == date.toString()) p.getLong("blocked_ms", 0L) else 0L

    fun addBlockedMs(date: LocalDate, deltaMs: Long) {
        if (deltaMs <= 0) return
        p.edit().putString("blocked_date", date.toString()).putLong("blocked_ms", blockedMs(date) + deltaMs).apply()
    }

    // ---- 오늘 한도 도달 여부 (도달한 날에는 그 날 한도를 늘릴 수 없음) ----
    fun exceededOn(date: LocalDate): Boolean = p.getString("exceeded_date", null) == date.toString()

    fun markExceeded(date: LocalDate) {
        if (!exceededOn(date)) p.edit().putString("exceeded_date", date.toString()).commit()
    }
}
