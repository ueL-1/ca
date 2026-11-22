package com.ca.screentime.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * 시스템 사용 기록을 읽어 오늘의 사용시간과 현재 앞에 떠 있는 앱을 구한다.
 * 매번 하루 전체를 읽지 않도록 새 기록만 이어서 읽고, 1분마다 전체를 다시 계산해 오차를 바로잡는다.
 */
class UsageTracker(context: Context) {
    private val appContext = context.applicationContext
    private val usm = appContext.getSystemService(UsageStatsManager::class.java)

    private var phoneApps = PhoneApps(appContext)
    private var phoneAppsAt = 0L
    private var acc: UsageAccumulator? = null
    private var accDay: LocalDate? = null
    private var accZone: ZoneId? = null
    private var queriedUntil = 0L
    private var lastFull = 0L

    data class Snapshot(
        val day: LocalDate,
        /** 오늘 00:00부터 지금까지 센 사용시간(ms). 초과 화면 표시 시간은 아직 빼지 않은 값. */
        val countedMs: Long,
        val foreground: String?,
        /** 지금 이 순간이 사용시간으로 세지고 있는지. */
        val countingNow: Boolean,
        val phoneInFront: Boolean,
    )

    @Synchronized
    fun snapshot(now: Long, liveActive: Boolean): Snapshot {
        val zone = ZoneId.systemDefault()
        val day = TimeRules.dateOf(now, zone)
        if (now - phoneAppsAt > PHONE_APPS_REFRESH_MS || now < phoneAppsAt) {
            phoneApps = PhoneApps(appContext)
            phoneAppsAt = now
            acc = null
        }
        var a = acc
        val needFull = a == null || day != accDay || zone != accZone ||
            now - lastFull > FULL_RECOMPUTE_MS || now < queriedUntil
        if (needFull) {
            val dayStart = TimeRules.startOfDayMillis(day, zone)
            val fresh = UsageAccumulator(dayStart, phoneApps::isPhone)
            feed(fresh, dayStart - LOOKBACK_MS, now)
            a = fresh
            acc = fresh
            accDay = day
            accZone = zone
            lastFull = now
        } else if (now > queriedUntil) {
            feed(a!!, queriedUntil, now)
        }
        queriedUntil = now
        val current = a!!
        return Snapshot(
            day = day,
            countedMs = current.countedUntil(now, liveActive),
            foreground = current.foreground,
            countingNow = liveActive && current.isCounting(),
            phoneInFront = phoneApps.isPhone(current.foreground),
        )
    }

    private fun feed(a: UsageAccumulator, begin: Long, end: Long) {
        if (end <= begin) return
        val events = runCatching { usm.queryEvents(begin, end) }.getOrNull() ?: return
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            if (!events.getNextEvent(e)) break
            a.feed(e.eventType, e.packageName, e.className, e.timeStamp)
        }
    }

    companion object {
        private const val FULL_RECOMPUTE_MS = 60_000L
        private const val LOOKBACK_MS = 12 * 60 * 60_000L
        private const val PHONE_APPS_REFRESH_MS = 10 * 60_000L
    }
}
