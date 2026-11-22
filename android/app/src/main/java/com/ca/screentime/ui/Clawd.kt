package com.ca.screentime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 손을 흔들며 인사하는 Claude Code 픽셀 캐릭터.
 * 18 x 10 격자(가로 1칸, 세로 2칸이 한 픽셀) 위에 그리고, 오른손 두 프레임을 번갈아 보여준다.
 */
@Composable
fun ClawdWaving(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(380)
            frame = 1 - frame
        }
    }
    Canvas(
        modifier
            .offset(y = if (frame == 1) (-3).dp else 0.dp)
            .size(width = 216.dp, height = 120.dp)
            .semantics { contentDescription = "손을 흔들며 인사하는 Claude Code 캐릭터" },
    ) {
        val u = size.width / 18f
        val v = size.height / 10f
        fun px(x: Int, y: Int, w: Int, h: Int, color: Color) {
            drawRect(color, topLeft = Offset(x * u, y * v), size = Size(w * u, h * v))
        }
        val body = Palette.Clay
        px(3, 0, 12, 8, body) // 몸
        px(1, 4, 2, 2, body) // 왼팔
        px(15, 2, 1, 2, body) // 들어 올린 오른팔
        if (frame == 0) px(16, 0, 1, 2, body) else px(15, 0, 1, 2, body) // 흔드는 손
        px(4, 8, 1, 2, body) // 다리
        px(6, 8, 1, 2, body)
        px(11, 8, 1, 2, body)
        px(13, 8, 1, 2, body)
        px(5, 2, 1, 2, Palette.NightBg) // 눈
        px(12, 2, 1, 2, Palette.NightBg)
    }
}
