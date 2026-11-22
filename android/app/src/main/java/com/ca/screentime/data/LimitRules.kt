package com.ca.screentime.data

enum class Mode {
    /** 한도 이내. */
    NORMAL,

    /** 한도를 넘었지만 "딱 10분!" 추가 시간 사용 중. */
    EXTRA_ACTIVE,

    /** 한도 초과 → 다른 앱 제한. */
    OVER,

    /** "이번 주 제한 해제" 사용 중 → 이번 주 일요일 24:00까지 제한 없음. */
    WEEK_UNLOCKED,
}

data class Evaluation(
    val mode: Mode,
    val usageMs: Long,
    val limitMinutes: Int,
    /** 오늘 한도까지 남은 시간(ms). */
    val remainingMs: Long,
    val extraUsedToday: Boolean,
    /** 추가 10분 중 남은 시간(ms). */
    val extraRemainingMs: Long,
    val weekUnlocked: Boolean,
    /** 오늘 사용시간이 한도에 도달했는지 (추가 시간·주간 해제와 무관). */
    val exceeded: Boolean,
)

object LimitRules {
    const val EXTRA_MS = 10 * 60_000L

    /**
     * @param extraStartUsageMs "딱 10분!"을 누른 순간의 사용시간. 오늘 쓰지 않았으면 null.
     *   추가 10분은 그 이후 "실제 사용시간"으로 10분이 지날 때까지다 (화면이 꺼진 시간은 세지 않음).
     */
    fun evaluate(
        usageMs: Long,
        limitMinutes: Int,
        extraStartUsageMs: Long?,
        weekUnlocked: Boolean,
    ): Evaluation {
        val limitMs = limitMinutes * 60_000L
        val exceeded = usageMs >= limitMs
        val extraEnd = extraStartUsageMs?.let { maxOf(it, limitMs) + EXTRA_MS }
        val extraRemaining = if (extraEnd != null) maxOf(0L, extraEnd - usageMs) else 0L
        val mode = when {
            weekUnlocked -> Mode.WEEK_UNLOCKED
            !exceeded -> Mode.NORMAL
            extraEnd != null && usageMs < extraEnd -> Mode.EXTRA_ACTIVE
            else -> Mode.OVER
        }
        return Evaluation(
            mode = mode,
            usageMs = usageMs,
            limitMinutes = limitMinutes,
            remainingMs = maxOf(0L, limitMs - usageMs),
            extraUsedToday = extraStartUsageMs != null,
            extraRemainingMs = extraRemaining,
            weekUnlocked = weekUnlocked,
            exceeded = exceeded,
        )
    }
}
