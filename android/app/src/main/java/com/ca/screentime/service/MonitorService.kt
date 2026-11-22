package com.ca.screentime.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.ca.screentime.R
import com.ca.screentime.data.Engine
import com.ca.screentime.data.Evaluation
import com.ca.screentime.data.Mode
import com.ca.screentime.data.Permissions
import com.ca.screentime.data.TimeRules
import com.ca.screentime.ui.MainActivity
import com.ca.screentime.ui.OverlayModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * 상시 실행되는 감시 서비스.
 * 1초마다 사용시간을 확인하고, 한도를 넘었으면 전화 앱을 제외한 모든 앱 위에 초과 화면을 띄운다.
 * 화면이 꺼져 있거나 잠겨 있을 때는 초과 화면을 띄우지 않는다(잠금화면의 긴급전화는 항상 사용 가능).
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private lateinit var engine: Engine
    private lateinit var overlay: OverlayController

    private var lastTickElapsed = 0L
    private var pendingBlockedMs = 0L
    private var pendingBlockedDay: LocalDate? = null
    private var lastUsageMs = 0L
    private var lastDay: LocalDate? = null
    private var lastNotificationText = ""
    private var dialerGraceUntil = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wake.trySend(Unit)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine = Engine(this)
        overlay = OverlayController(this)
        ensureChannel(this)
        goForeground(buildNotification("사용시간을 확인하고 있어요"))

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch {
            while (isActive) {
                val next = try {
                    tick()
                } catch (t: Throwable) {
                    Log.e(TAG, "tick failed", t)
                    2_000L
                }
                withTimeoutOrNull(next) { wake.receive() }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(buildNotification(lastNotificationText.ifEmpty { "사용시간을 확인하고 있어요" }))
        wake.trySend(Unit)
        return START_STICKY
    }

    override fun onDestroy() {
        flushBlocked()
        overlay.hide()
        runCatching { unregisterReceiver(receiver) }
        scope.cancel()
        super.onDestroy()
    }

    /** @return 다음 확인까지 기다릴 시간(ms) */
    private suspend fun tick(): Long {
        if (!Permissions.hasUsageAccess(this)) {
            overlay.hide()
            updateNotification("사용정보 접근 권한이 필요해요")
            return 5_000L
        }

        val unsaved = unsavedBlocked(LocalDate.now())
        val status = withContext(Dispatchers.Default) { engine.compute(unsaved) }
        val day = status.day
        val eval = status.eval
        if (lastDay != null && lastDay != day) flushBlocked()
        lastDay = day
        lastUsageMs = eval.usageMs
        if (eval.exceeded) engine.store.markExceeded(day)

        // 초과 화면이 떠 있던 시간은 사용시간에서 뺀다.
        val elapsed = SystemClock.elapsedRealtime()
        if (overlay.isShowing && status.screenActive && status.snapshot.countingNow && lastTickElapsed > 0) {
            addBlocked(day, (elapsed - lastTickElapsed).coerceIn(0L, 3_000L))
        }
        lastTickElapsed = elapsed

        val inCall = Permissions.hasPhoneState(this) && runCatching {
            getSystemService(TelecomManager::class.java)?.isInCall == true
        }.getOrDefault(false)

        val block = eval.mode == Mode.OVER &&
            status.screenActive &&
            elapsed >= dialerGraceUntil &&
            !status.snapshot.phoneInFront &&
            !inCall &&
            Permissions.hasOverlay(this)

        if (block) {
            overlay.show(overlayModel(day, eval))
        } else if (overlay.isShowing) {
            overlay.hide()
            flushBlocked()
        }
        if (pendingBlockedMs >= 10_000L) flushBlocked()

        updateNotification(notificationText(day, eval))
        return if (status.screenActive) 1_000L else 5_000L
    }

    private fun overlayModel(day: LocalDate, eval: Evaluation): OverlayModel {
        val limitWords = TimeRules.words(eval.limitMinutes.toLong())
        return OverlayModel(
            dateText = TimeRules.longText(day),
            limitLine = if (eval.extraUsedToday) "한도 $limitWords + 추가 10분" else "한도 $limitWords",
            limitWords = limitWords,
            tenUsed = eval.extraUsedToday,
            weekAvailable = !eval.weekUnlocked,
            weekEndText = TimeRules.shortText(TimeRules.weekLastDay(day)) + " 24:00",
            nextWeekText = TimeRules.shortText(TimeRules.nextWeekStart(day)) + " 00:00",
            onUseTen = {
                if (engine.store.useExtra(day, lastUsageMs)) wake.trySend(Unit)
            },
            onWeekUnlock = {
                if (engine.store.useWeekUnlock(day)) wake.trySend(Unit)
            },
            onCall = { openDialer() },
        )
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure { Log.w(TAG, "dialer open failed", it) }
        // 전화 앱이 뜰 때까지 잠깐(3초) 초과 화면을 다시 띄우지 않는다. 이후에는 전화 앱이 앞에 있는 동안 계속 숨겨진다.
        dialerGraceUntil = SystemClock.elapsedRealtime() + 3_000L
        overlay.hide()
        flushBlocked()
        lastTickElapsed = 0L
    }

    private fun unsavedBlocked(day: LocalDate): Long = if (pendingBlockedDay == day) pendingBlockedMs else 0L

    private fun addBlocked(day: LocalDate, delta: Long) {
        if (pendingBlockedDay != day) {
            flushBlocked()
            pendingBlockedDay = day
        }
        pendingBlockedMs += delta
    }

    private fun flushBlocked() {
        val day = pendingBlockedDay
        if (day != null && pendingBlockedMs > 0) engine.store.addBlockedMs(day, pendingBlockedMs)
        pendingBlockedMs = 0L
    }

    private fun notificationText(day: LocalDate, eval: Evaluation): String {
        val usedMin = eval.usageMs / 60_000L
        return when (eval.mode) {
            Mode.NORMAL -> "오늘 ${TimeRules.words(usedMin)} 사용 · 남은 시간 ${TimeRules.words(eval.limitMinutes - usedMin)}"
            Mode.EXTRA_ACTIVE -> "딱 10분! 사용 중 · 남은 시간 ${TimeRules.words((eval.extraRemainingMs + 59_999L) / 60_000L)}"
            Mode.OVER -> "오늘 한도 초과 · 내일 00:00에 다시 열려요"
            Mode.WEEK_UNLOCKED -> "이번 주 제한 해제 중 · ${TimeRules.shortText(TimeRules.weekLastDay(day))} 24:00까지"
        }
    }

    private fun updateNotification(text: String) {
        if (text == lastNotificationText) return
        lastNotificationText = text
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clawd)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun goForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            // 시스템이 백그라운드 시작을 막은 경우: 다음에 앱을 열거나 재부팅하면 다시 시작된다.
            Log.w(TAG, "startForeground not allowed", t)
        }
    }

    companion object {
        private const val TAG = "MonitorService"
        private const val CHANNEL_ID = "monitor"
        private const val NOTIFICATION_ID = 1

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "사용시간 확인", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "사용시간을 확인하는 동안 표시되는 알림"
                        setShowBadge(false)
                    },
                )
            }
        }

        /** 필수 권한이 있으면 서비스를 시작(또는 즉시 다시 확인)한다. */
        fun startIfReady(context: Context) {
            if (!Permissions.coreReady(context)) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
            }.onFailure { Log.w(TAG, "start failed", it) }
        }
    }
}
