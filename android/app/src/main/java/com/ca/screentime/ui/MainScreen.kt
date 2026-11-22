package com.ca.screentime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ca.screentime.data.Engine
import com.ca.screentime.data.Mode
import com.ca.screentime.data.Store
import com.ca.screentime.data.TimeRules
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun MainScreen(
    status: Engine.Status?,
    perms: PermissionState,
    engine: Engine,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = engine.store
    val today = status?.day ?: LocalDate.now()
    val days = DayOfWeek.entries
    val limits = remember { mutableStateListOf(*days.map { store.limitMinutes(it) }.toTypedArray()) }
    var selected by rememberSaveable { mutableStateOf(today.dayOfWeek.value) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val selDay = DayOfWeek.of(selected)

    // 주의: 여기서 로컬 함수 참조(::함수)를 넘기면 안 된다.
    // 함수 참조의 equals()는 캡처한 값(선택한 요일)을 비교하지 않아서, Compose가 요일이 바뀐 뒤에도
    // "같은 함수"로 보고 처음 만든 버튼 핸들러(= 처음 선택돼 있던 오늘 요일)를 계속 쓴다.
    // 그래서 람다로 넘기고, 바꿀 요일은 버튼을 누른 순간의 선택 상태에서 읽는다.
    val onChange: (Int, Int) -> Unit = { hours, minuteSteps ->
        val day = DayOfWeek.of(selected)
        val idx = day.value - 1
        val cur = limits[idx]
        val h = (cur / 60 + hours).coerceIn(0, 23)
        val m = ((cur % 60 + minuteSteps * 5) % 60 + 60) % 60
        val next = (h * 60 + m).coerceIn(0, Store.MAX_LIMIT_MINUTES)
        val exceededToday = store.exceededOn(today) || status?.eval?.exceeded == true
        when {
            next == cur -> Unit
            day == today.dayOfWeek && next > cur && exceededToday ->
                message = false to "오늘은 이미 한도를 넘어서 오늘 한도는 늘릴 수 없어요"
            else -> {
                store.setLimitMinutes(day, next)
                limits[idx] = next
                message = true to "${TimeRules.dayName(day)} 한도를 ${TimeRules.words(next.toLong())}으로 저장했어요"
            }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(today, status, perms, onOpenPermissions)
        SummaryCard(status, perms, limits[today.dayOfWeek.value - 1])
        DaySelector(days, limits, selected, today.dayOfWeek) {
            selected = it
            message = null
        }
        PickerCard(
            day = selDay,
            isToday = selDay == today.dayOfWeek,
            minutes = limits[selDay.value - 1],
            message = message,
            onChange = onChange,
        )
        StatusTiles(today, status, store)
    }
}

@Composable
private fun Header(today: LocalDate, status: Engine.Status?, perms: PermissionState, onOpenPermissions: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(TimeRules.longText(today), color = Palette.Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "오늘의 사용시간",
                color = Palette.Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
        }
        val weekUnlocked = status?.eval?.mode == Mode.WEEK_UNLOCKED
        val (dot, label) = when {
            !perms.core -> Palette.ClayText to "권한 필요"
            weekUnlocked -> Palette.Clay to "제한 해제 중"
            else -> Palette.GreenDot to "제한 작동 중"
        }
        Surface(
            onClick = onOpenPermissions,
            enabled = !perms.core,
            modifier = Modifier.padding(bottom = 4.dp),
            shape = RoundedCornerShape(999.dp),
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Line),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Dot(dot)
                Text(label, color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryCard(status: Engine.Status?, perms: PermissionState, todayLimit: Int) {
    val eval = status?.eval
    val usedMin = (eval?.usageMs ?: 0L) / 60_000L
    val mode = eval?.mode
    val over = eval != null && usedMin >= todayLimit
    val pct = if (todayLimit > 0) ((usedMin * 100) / todayLimit).coerceIn(0L, 100L).toInt() else 100

    val rightLabel: String
    val rightValue: String
    val rightColor: Color
    val barFraction: Float
    val barColor: Color
    val caption: String
    when {
        eval == null || !perms.usage -> {
            rightLabel = "오늘 한도"; rightValue = TimeRules.words(todayLimit.toLong()); rightColor = Palette.Muted
            barFraction = 0f; barColor = Palette.Clay
            caption = "사용정보 접근 권한을 허용하면 사용시간이 표시돼요"
        }
        mode == Mode.WEEK_UNLOCKED -> {
            rightLabel = "오늘 한도"; rightValue = TimeRules.words(todayLimit.toLong()); rightColor = Palette.Muted
            barFraction = 1f; barColor = Palette.BarMuted
            caption = "이번 주 제한 해제 중이라 한도를 넘어도 계속 쓸 수 있어요"
        }
        mode == Mode.EXTRA_ACTIVE -> {
            rightLabel = "추가 시간 남음"
            rightValue = TimeRules.words((eval.extraRemainingMs + 59_999L) / 60_000L); rightColor = Palette.ClayText
            barFraction = 1f; barColor = Palette.ClayText
            caption = "딱 10분! 사용 중 · 끝나면 다시 제한돼요"
        }
        over -> {
            rightLabel = "남은 시간"; rightValue = "0분"; rightColor = Palette.ClayText
            barFraction = 1f; barColor = Palette.ClayText
            caption = "오늘 한도를 모두 썼어요 · 사용시간 초과 화면이 표시돼요"
        }
        else -> {
            rightLabel = "남은 시간"; rightValue = TimeRules.words(todayLimit - usedMin); rightColor = Palette.ClayText
            barFraction = pct / 100f; barColor = Palette.Clay
            caption = "한도 ${TimeRules.words(todayLimit.toLong())} 중 $pct% 사용"
        }
    }

    Card {
        Column(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("오늘 사용", color = Palette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (eval == null) {
                        Text("–", color = Palette.Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    } else {
                        val unit = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            buildAnnotatedString {
                                append("${usedMin / 60}")
                                withStyle(unit) { append("시간 ") }
                                append("${usedMin % 60}")
                                withStyle(unit) { append("분") }
                            },
                            color = Palette.Ink,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(rightLabel, color = Palette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(rightValue, color = rightColor, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Palette.Track)
                    .semantics { contentDescription = "오늘 한도 대비 $pct% 사용" },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(barFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor),
                )
            }
            Text(caption, color = Palette.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DaySelector(
    days: List<DayOfWeek>,
    limits: List<Int>,
    selected: Int,
    today: DayOfWeek,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("요일별 최대 사용시간", color = Palette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("요일을 눌러 설정", color = Palette.Muted, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day ->
                val on = day.value == selected
                val isToday = day == today
                Surface(
                    onClick = { onSelect(day.value) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .semantics {
                            this.selected = on
                            contentDescription = TimeRules.dayName(day) + " 최대 " +
                                TimeRules.words(limits[day.value - 1].toLong()) + if (isToday) ", 오늘" else ""
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (on) Palette.Ink else Palette.Surface,
                    border = BorderStroke(1.dp, if (on) Palette.Ink else Palette.Line),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            TimeRules.dayLetter(day),
                            color = if (on) Color.White else Palette.Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            TimeRules.clock(limits[day.value - 1]),
                            color = if (on) Palette.SelectedSub else Palette.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Dot(if (isToday) Palette.Clay else Color.Transparent, 5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCard(
    day: DayOfWeek,
    isToday: Boolean,
    minutes: Int,
    message: Pair<Boolean, String>?,
    onChange: (hours: Int, minuteSteps: Int) -> Unit,
) {
    Card(radius = 24) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${TimeRules.dayName(day)} 하루 최대 사용시간",
                    color = Palette.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isToday) {
                    Text(
                        "오늘",
                        modifier = Modifier
                            .background(Palette.BadgeBg, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Palette.BadgeFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stepper(
                    value = "${minutes / 60}",
                    unit = "시간",
                    upLabel = "1시간 늘리기",
                    downLabel = "1시간 줄이기",
                    onUp = { onChange(1, 0) },
                    onDown = { onChange(-1, 0) },
                )
                Stepper(
                    value = (minutes % 60).toString().padStart(2, '0'),
                    unit = "분",
                    upLabel = "5분 늘리기",
                    downLabel = "5분 줄이기",
                    onUp = { onChange(0, 1) },
                    onDown = { onChange(0, -1) },
                )
            }
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    message == null -> Text("변경하면 바로 저장돼요", color = Palette.Muted, fontSize = 13.sp)
                    message.first -> {
                        CheckIcon(Palette.Green)
                        Text(message.second, color = Palette.Muted, fontSize = 13.sp)
                    }
                    else -> Text(message.second, color = Palette.ClayText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    value: String,
    unit: String,
    upLabel: String,
    downLabel: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton(upLabel, onUp) { ChevronUpIcon(Palette.Ink) }
        Box(
            Modifier
                .size(width = 124.dp, height = 80.dp)
                .background(Palette.Bg, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, color = Palette.Ink, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                Text(
                    unit,
                    modifier = Modifier.padding(bottom = 10.dp),
                    color = Palette.Muted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        StepButton(downLabel, onDown) { ChevronDownIcon(Palette.Ink) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .semantics { contentDescription = label; role = Role.Button },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun StatusTiles(today: LocalDate, status: Engine.Status?, store: Store) {
    val eval = status?.eval
    val extraUsed = eval?.extraUsedToday ?: (store.extraStartUsage(today) != null)
    val weekUnlocked = eval?.weekUnlocked ?: store.weekUnlocked(today)

    val (tenValue, tenColor, tenSub) = when {
        eval?.mode == Mode.EXTRA_ACTIVE -> Triple(
            "사용 중", Palette.ClayText,
            "남은 시간 ${TimeRules.words((eval.extraRemainingMs + 59_999L) / 60_000L)}",
        )
        extraUsed -> Triple("오늘 사용함", Palette.Ink, "내일 다시 쓸 수 있어요")
        else -> Triple("오늘 사용 가능", Palette.Green, "하루 1회 · 자정에 초기화")
    }
    val (weekValue, weekColor, weekSub) = if (weekUnlocked) {
        Triple(
            "해제 중", Palette.ClayText,
            "${TimeRules.shortText(TimeRules.weekLastDay(today))} 24:00까지\n" +
                "다음 사용: ${TimeRules.shortText(TimeRules.nextWeekStart(today))}",
        )
    } else {
        Triple("사용 가능", Palette.Green, "주 1회 · 매주 월요일 초기화")
    }

    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tile("딱 10분!", tenValue, tenColor, tenSub, Modifier.weight(1f).fillMaxHeight())
        Tile("이번 주 제한 해제", weekValue, weekColor, weekSub, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun Tile(title: String, value: String, valueColor: Color, sub: String, modifier: Modifier) {
    Card(modifier = modifier, radius = 18) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, color = Palette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = Palette.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, radius: Int = 20, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(radius.dp)),
    ) { content() }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Palette.Surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp),
        ) {
            NavItem("메인", selected == 0, Modifier.weight(1f), { onSelect(0) }) { HomeIcon(it) }
            NavItem("권한·설정", selected == 1, Modifier.weight(1f), { onSelect(1) }) { ShieldIcon(it) }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    on: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val color = if (on) Palette.Ink else Palette.Muted
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp).semantics { selected = on },
        color = Color.Transparent,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 28.dp)
                    .background(if (on) Palette.Track else Color.Transparent, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) { icon(color) }
            Text(label, color = color, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold)
        }
    }
}
