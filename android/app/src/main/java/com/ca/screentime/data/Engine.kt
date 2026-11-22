package com.ca.screentime.data

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import java.time.LocalDate

/** 사용시간 기록 + 저장된 설정 → 지금 상태(정상/추가 10분/초과/주간 해제)를 계산한다. */
class Engine(context: Context) {
    private val appContext = context.applicationContext
    val store = Store(appContext)
    private val tracker = UsageTracker(appContext)
    private val power = appContext.getSystemService(PowerManager::class.java)
    private val keyguard = appContext.getSystemService(KeyguardManager::class.java)

    data class Status(
        val day: LocalDate,
        val eval: Evaluation,
        val snapshot: UsageTracker.Snapshot,
        /** 지금 화면이 켜져 있고 잠금이 풀려 있는지. */
        val screenActive: Boolean,
    )

    /** @param unsavedBlockedMs 아직 저장하지 않은 "초과 화면 표시 시간" (서비스 내부 값). */
    fun compute(unsavedBlockedMs: Long = 0L): Status {
        val now = System.currentTimeMillis()
        val active = (power?.isInteractive ?: false) && !(keyguard?.isKeyguardLocked ?: false)
        val snap = tracker.snapshot(now, active)
        val day = snap.day
        val usage = maxOf(0L, snap.countedMs - store.blockedMs(day) - unsavedBlockedMs)
        val eval = LimitRules.evaluate(
            usageMs = usage,
            limitMinutes = store.limitMinutes(day.dayOfWeek),
            extraStartUsageMs = store.extraStartUsage(day),
            weekUnlocked = store.weekUnlocked(day),
        )
        return Status(day, eval, snap, active)
    }
}
