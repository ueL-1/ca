package com.ca.screentime.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** 날짜·주 계산. 항상 실제 시스템 시간과 현재 시간대를 기준으로 한다. 한 주는 월요일 00:00 ~ 일요일 24:00. */
object TimeRules {
    fun dateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 이번 주 마지막 날(일요일). 제한 해제는 이 날 24:00까지 유지된다. */
    fun weekLastDay(date: LocalDate): LocalDate = weekStart(date).plusDays(6)

    /** 다음 주 시작(월요일 00:00) = 주간 제한 해제를 다시 쓸 수 있는 날. */
    fun nextWeekStart(date: LocalDate): LocalDate = weekStart(date).plusWeeks(1)

    private val longDate = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
    private val shortDate = DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN)

    /** 예: 11월 25일 수요일 */
    fun longText(date: LocalDate): String = date.format(longDate)

    /** 예: 11월 29일(일) */
    fun shortText(date: LocalDate): String = date.format(shortDate)

    fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

    fun dayLetter(day: DayOfWeek): String = dayName(day).substring(0, 1)

    /** 분 → "3시간", "1시간 18분", "45분" */
    fun words(minutes: Long): String {
        val m = if (minutes < 0) 0 else minutes
        val h = m / 60
        val r = m % 60
        return when {
            h > 0 && r > 0 -> "${h}시간 ${r}분"
            h > 0 -> "${h}시간"
            else -> "${r}분"
        }
    }

    /** 분 → "3:00" */
    fun clock(minutes: Int): String = "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
}
