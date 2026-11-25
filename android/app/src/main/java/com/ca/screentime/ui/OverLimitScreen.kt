package com.ca.screentime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 초과 화면에 필요한 값과 버튼 동작. */
data class OverlayModel(
    val dateText: String,
    val limitLine: String,
    val limitWords: String,
    val tenUsed: Boolean,
    val weekAvailable: Boolean,
    val weekEndText: String,
    val nextWeekText: String,
    val onUseTen: () -> Unit,
    val onWeekUnlock: () -> Unit,
    val onCall: () -> Unit,
)

@Composable
fun OverLimitScreen(m: OverlayModel) {
    var sheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Palette.NightBg)) {
        Star(44.dp, 118.dp, 4.dp, Palette.StarBright)
        Star(92.dp, 196.dp, 3.dp, Palette.StarDim)
        Star(318.dp, 104.dp, 4.dp, Palette.StarDim)
        Star(340.dp, 236.dp, 5.dp, Palette.StarBright)
        Star(270.dp, 162.dp, 3.dp, Palette.StarBright)
        Star(28.dp, 286.dp, 3.dp, Palette.StarDim)

        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.dateText, color = Palette.NightMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(m.limitLine, color = Palette.NightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SpeechBubble(if (m.tenUsed) "오늘은 진짜 끝! 내일 또 만나요" else "안녕! 오늘은 여기까지예요")
                Spacer(Modifier.height(20.dp))
                ClawdWaving()
                Spacer(Modifier.height(30.dp))
                Text(
                    "오늘 한도 초과",
                    color = Palette.NightHead,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (m.tenUsed) "추가 10분까지 모두 사용했어요.\n내일 00:00에 다시 열려요."
                    else "오늘 쓸 수 있는 ${m.limitWords}을 모두 사용했어요.\n내일 00:00에 다시 열려요.",
                    color = Palette.NightMuted,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Surface(
                    onClick = m.onCall,
                    shape = RoundedCornerShape(999.dp),
                    color = Palette.NightSurface,
                    border = BorderStroke(1.dp, Palette.NightLine),
                ) {
                    Row(
                        Modifier.height(48.dp).padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PhoneIcon(Palette.Clay)
                        Text("전화 걸기", color = Palette.NightInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("전화·긴급전화는 제한 중에도 쓸 수 있어요", color = Palette.NightMuted, fontSize = 12.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(
                    title = "이번 주 제한 해제",
                    subtitle = if (m.weekAvailable) "이번 주 1회 사용 가능" else "다음 사용: ${m.nextWeekText}",
                    enabled = m.weekAvailable,
                    filled = false,
                    onClick = { sheet = true },
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    title = "딱 10분!",
                    subtitle = if (m.tenUsed) "오늘 이미 사용함" else "오늘 1회 사용 가능",
                    enabled = !m.tenUsed,
                    filled = true,
                    onClick = m.onUseTen,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (sheet) {
            WeekUnlockSheet(
                weekEndText = m.weekEndText,
                nextWeekText = m.nextWeekText,
                onCancel = { sheet = false },
                onConfirm = {
                    sheet = false
                    m.onWeekUnlock()
                },
            )
        }
    }
}

@Composable
private fun Star(x: Dp, y: Dp, size: Dp, color: Color) {
    Box(Modifier.offset(x = x, y = y).size(size).background(color))
}

@Composable
private fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .background(Palette.NightInk, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(text, color = Palette.NightBg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .offset(y = (-6).dp)
                .size(12.dp)
                .rotate(45f)
                .background(Palette.NightInk),
        )
    }
}

@Composable
private fun BigButton(
    title: String,
    subtitle: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when {
        filled && enabled -> Palette.Clay
        filled -> Palette.NightDisabled
        else -> Color.Transparent
    }
    val content = when {
        filled && enabled -> Palette.NightBg
        enabled -> Palette.NightInk
        else -> Palette.NightMuted
    }
    val border = if (filled) null else BorderStroke(1.5.dp, if (enabled) Palette.NightOutline else Palette.NightLine)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(Radius.BIG_BUTTON.dp),
        color = container,
        border = border,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title,
                color = content,
                fontSize = if (filled) 21.sp else 16.sp,
                fontWeight = if (filled) FontWeight.Black else FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                subtitle,
                color = if (filled) content else Palette.NightMuted,
                fontSize = 12.sp,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WeekUnlockSheet(
    weekEndText: String,
    nextWeekText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Palette.NightSurface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("이번 주 제한을 해제할까요?", color = Palette.NightInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "${weekEndText}까지 사용시간 제한 없이 쓸 수 있어요.\n" +
                    "해제는 일주일에 한 번만 쓸 수 있어서, 다음 사용은 ${nextWeekText}부터 가능해요.",
                color = Palette.NightSheetText,
                fontSize = 14.sp,
                lineHeight = 23.sp,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, Palette.NightOutline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("취소", color = Palette.NightInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Palette.NightInk,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("해제하기", color = Palette.NightBg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
