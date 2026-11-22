package com.ca.screentime.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ca.screentime.R
import com.ca.screentime.data.Evaluation
import com.ca.screentime.data.LimitRules
import com.ca.screentime.data.Mode
import com.ca.screentime.data.TimeRules
import com.ca.screentime.ui.MainActivity
import java.time.LocalDate

/** 상단 알림에 보여줄 내용. [progressMax]가 0이면 막대 없이 글자만 보여준다. */
data class UsageNotificationContent(
    val title: String,
    val text: String,
    val progressMax: Int = 0,
    val progress: Int = 0,
) {
    val hasProgress: Boolean get() = progressMax > 0
}

object UsageNotification {
    const val CHANNEL_ID = "monitor"
    const val ID = 1
    private const val BAR_COLOR = 0xFFD97757.toInt()

    /** 상태별 알림 내용. 막대는 분 단위로 움직여서 알림이 1분에 한 번 정도만 갱신된다. */
    fun content(day: LocalDate, eval: Evaluation): UsageNotificationContent {
        val usedMin = eval.usageMs / 60_000L
        val limit = eval.limitMinutes
        val limitWords = TimeRules.words(limit.toLong())
        val barMax = maxOf(limit, 1)
        return when (eval.mode) {
            Mode.NORMAL -> UsageNotificationContent(
                title = "오늘 ${TimeRules.words(usedMin)} 사용",
                text = "한도 $limitWords · 남은 시간 ${TimeRules.words(limit - usedMin)}",
                progressMax = barMax,
                progress = usedMin.toInt().coerceIn(0, barMax),
            )
            Mode.EXTRA_ACTIVE -> {
                val totalMin = (LimitRules.EXTRA_MS / 60_000L).toInt()
                val leftMin = ((eval.extraRemainingMs + 59_999L) / 60_000L).toInt().coerceIn(0, totalMin)
                UsageNotificationContent(
                    title = "딱 10분! 사용 중",
                    text = "남은 시간 ${TimeRules.words(leftMin.toLong())} · 끝나면 다시 제한돼요",
                    progressMax = totalMin,
                    progress = totalMin - leftMin,
                )
            }
            Mode.OVER -> UsageNotificationContent(
                title = "오늘 한도 초과",
                text = "오늘 ${TimeRules.words(usedMin)} 사용 · 내일 00:00에 다시 열려요",
                progressMax = barMax,
                progress = barMax,
            )
            Mode.WEEK_UNLOCKED -> UsageNotificationContent(
                title = "이번 주 제한 해제 중",
                text = "오늘 ${TimeRules.words(usedMin)} 사용 · ${TimeRules.shortText(TimeRules.weekLastDay(day))} 24:00까지",
                progressMax = barMax,
                progress = usedMin.toInt().coerceIn(0, barMax),
            )
        }
    }

    /** 사용시간을 아직 알 수 없을 때 (시작 직후, 권한 없음). */
    fun waiting(context: Context, text: String) =
        UsageNotificationContent(title = context.getString(R.string.app_name), text = text)

    fun build(context: Context, c: UsageNotificationContent): Notification {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clawd)
            .setColor(BAR_COLOR)
            .setContentTitle(c.title)
            .setContentText(c.text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .apply {
                if (c.hasProgress) {
                    // 기본 알림 막대(setProgress)는 접혀 있을 때 안 보이고 색도 시스템 테마를 따라가서,
                    // 앱 레이아웃으로 접힌/펼친 상태 모두 앱 색(클레이) 막대 하나만 보여준다.
                    // setProgress를 같이 쓰면 시스템 막대가 하나 더 그려지므로 쓰지 않는다.
                    setStyle(Notification.DecoratedCustomViewStyle())
                    setCustomContentView(views(context, R.layout.notification_usage_small, c))
                    setCustomBigContentView(views(context, R.layout.notification_usage_big, c))
                }
            }
            .build()
    }

    private fun views(context: Context, layout: Int, c: UsageNotificationContent) =
        RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.title, c.title)
            if (layout == R.layout.notification_usage_big) setTextViewText(R.id.text, c.text)
            setProgressBar(R.id.bar, c.progressMax, c.progress, false)
        }
}
