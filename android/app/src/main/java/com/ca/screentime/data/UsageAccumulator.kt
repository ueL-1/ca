package com.ca.screentime.data

/**
 * Android 시스템의 사용 기록(UsageEvents)을 시간 순서대로 받아 "실제 휴대폰 사용시간"을 계산한다.
 *
 * 사용시간으로 세는 조건 (모두 만족해야 함):
 *  1. 화면이 켜져 있음 (SCREEN_INTERACTIVE ~ SCREEN_NON_INTERACTIVE 사이).
 *     화면이 꺼져 있으면 다른 앱이 실행 중이어도(음악, 내비, 다운로드 등) 세지 않는다.
 *  2. 잠금화면이 아님 (KEYGUARD_SHOWN ~ KEYGUARD_HIDDEN 사이는 제외).
 *  3. 화면 앞에 실제로 앱 화면이 떠 있음 (ACTIVITY_RESUMED 상태인 화면이 하나 이상).
 *  4. 그 화면이 전화 관련 앱(전화, 통화 화면, 긴급전화)만은 아님.
 *
 * 시스템이 저장한 기록을 다시 읽어 계산하므로 앱 종료·강제 종료·재부팅 후에도 기록이 유지된다.
 * android.* 클래스에 의존하지 않아 JVM 단위 테스트가 가능하다.
 */
class UsageAccumulator(
    /** 이 시각(오늘 00:00) 이전 구간은 계산에 넣지 않는다. 그 이전 기록은 상태 파악용으로만 쓴다. */
    private val countFrom: Long,
    private val isPhoneApp: (String) -> Boolean,
) {
    companion object {
        // android.app.usage.UsageEvents.Event 상수값
        const val ACTIVITY_RESUMED = 1
        const val ACTIVITY_PAUSED = 2
        const val SCREEN_INTERACTIVE = 15
        const val SCREEN_NON_INTERACTIVE = 16
        const val KEYGUARD_SHOWN = 17
        const val KEYGUARD_HIDDEN = 18
        const val ACTIVITY_STOPPED = 23
        const val DEVICE_SHUTDOWN = 26
        const val DEVICE_STARTUP = 27
    }

    private var screenOn = false
    private var locked = false

    /** 화면 켜짐/꺼짐 기록을 남기지 않는 기기에서는 "앱 화면이 앞에 떠 있는지"만으로 판단한다. */
    private var sawScreenEvents = false

    /** 현재 RESUMED 상태인 화면들 (key = 패키지/클래스, value = 패키지). 삽입 순서 = 최근 순서. */
    private val resumed = LinkedHashMap<String, String>()

    private var lastTs = Long.MIN_VALUE
    private var counted = 0L

    /** 가장 최근에 앞으로 나온 앱 패키지. */
    var foreground: String? = null
        private set

    fun isCounting(): Boolean =
        (screenOn || !sawScreenEvents) && !locked && resumed.values.any { !isPhoneApp(it) }

    fun feed(type: Int, pkg: String?, cls: String?, ts: Long) {
        advance(ts)
        when (type) {
            SCREEN_INTERACTIVE -> {
                screenOn = true
                sawScreenEvents = true
            }
            SCREEN_NON_INTERACTIVE -> {
                screenOn = false
                sawScreenEvents = true
                resumed.clear()
                foreground = null
            }
            KEYGUARD_SHOWN -> locked = true
            KEYGUARD_HIDDEN -> locked = false
            ACTIVITY_RESUMED -> if (pkg != null) {
                val key = "$pkg/$cls"
                resumed.remove(key)
                resumed[key] = pkg
                foreground = pkg
            }
            ACTIVITY_PAUSED, ACTIVITY_STOPPED -> if (pkg != null) {
                resumed.remove("$pkg/$cls")
                foreground = resumed.values.lastOrNull()
            }
            DEVICE_SHUTDOWN -> {
                screenOn = false
                locked = false
                resumed.clear()
                foreground = null
            }
            // 부팅 직후에는 화면이 이미 켜져 있어 "화면 켜짐" 기록이 없을 수 있다.
            // 잠금화면 동안은 앱 화면이 RESUMED가 아니므로, 잠금 해제 후 앱이 뜨는 순간부터 세게 된다.
            DEVICE_STARTUP -> {
                screenOn = true
                locked = false
                resumed.clear()
                foreground = null
            }
        }
    }

    private fun advance(ts: Long) {
        if (lastTs != Long.MIN_VALUE && ts > lastTs && isCounting()) {
            val from = maxOf(lastTs, countFrom)
            if (ts > from) counted += ts - from
        }
        if (ts > lastTs) lastTs = ts
    }

    /**
     * [now]까지의 누적 사용시간(ms). 마지막 기록 이후 구간은 [liveActive]
     * (지금 실제로 화면이 켜져 있고 잠금이 풀려 있는지)가 true일 때만 더한다.
     * 상태를 바꾸지 않으므로 이후 기록을 계속 이어서 넣을 수 있다.
     */
    fun countedUntil(now: Long, liveActive: Boolean): Long {
        var c = counted
        if (liveActive && lastTs != Long.MIN_VALUE && now > lastTs && isCounting()) {
            val from = maxOf(lastTs, countFrom)
            if (now > from) c += now - from
        }
        return c
    }
}
