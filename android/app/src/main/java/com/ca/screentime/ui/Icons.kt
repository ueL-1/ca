package com.ca.screentime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 24x24 좌표계로 그리는 단순한 선 아이콘들 (샘플과 같은 모양). */
@Composable
private fun LineIcon(color: Color, size: Dp, strokeWidth: Float, draw: DrawScope.(scale: Float, stroke: Stroke) -> Unit) {
    Canvas(Modifier.size(size)) {
        val scale = this.size.width / 24f
        draw(scale, Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun path(scale: Float, vararg points: Pair<Float, Float>): Path = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * scale, y * scale) else lineTo(x * scale, y * scale) }
}

@Composable
fun ChevronUpIcon(color: Color) = LineIcon(color, 26.dp, 2.4f) { s, st ->
    drawPath(path(s, 6f to 15f, 12f to 9f, 18f to 15f), color, style = st)
}

@Composable
fun ChevronDownIcon(color: Color) = LineIcon(color, 26.dp, 2.4f) { s, st ->
    drawPath(path(s, 6f to 9f, 12f to 15f, 18f to 9f), color, style = st)
}

@Composable
fun CheckIcon(color: Color) = LineIcon(color, 15.dp, 2.8f) { s, st ->
    drawPath(path(s, 5f to 12f, 10f to 17f, 20f to 7f), color, style = st)
}

@Composable
fun HomeIcon(color: Color) = LineIcon(color, 20.dp, 2f) { s, st ->
    drawPath(path(s, 3f to 10.5f, 12f to 3f, 21f to 10.5f), color, style = st)
    drawPath(path(s, 5f to 9.5f, 5f to 20f, 19f to 20f, 19f to 9.5f), color, style = st)
}

@Composable
fun ShieldIcon(color: Color) = LineIcon(color, 20.dp, 2f) { s, st ->
    val shield = Path().apply {
        moveTo(12f * s, 3f * s)
        lineTo(20f * s, 6f * s)
        lineTo(20f * s, 12f * s)
        cubicTo(20f * s, 16.5f * s, 16.6f * s, 20.3f * s, 12f * s, 21f * s)
        cubicTo(7.4f * s, 20.3f * s, 4f * s, 16.5f * s, 4f * s, 12f * s)
        lineTo(4f * s, 6f * s)
        close()
    }
    drawPath(shield, color, style = st)
    drawPath(path(s, 9f to 12f, 11f to 14f, 15f to 10f), color, style = st)
}

@Composable
fun PhoneIcon(color: Color) = LineIcon(color, 18.dp, 2.2f) { s, st ->
    val p = Path().apply {
        moveTo(5f * s, 4f * s)
        lineTo(9f * s, 4f * s)
        lineTo(11f * s, 9f * s)
        lineTo(8.5f * s, 10.5f * s)
        cubicTo(9.6f * s, 12.8f * s, 11.2f * s, 14.4f * s, 13.5f * s, 15.5f * s)
        lineTo(15f * s, 13f * s)
        lineTo(20f * s, 15f * s)
        lineTo(20f * s, 19f * s)
        cubicTo(20f * s, 20.1f * s, 19.1f * s, 21f * s, 18f * s, 21f * s)
        cubicTo(10f * s, 20.5f * s, 3.5f * s, 14f * s, 3f * s, 6f * s)
        cubicTo(3f * s, 4.9f * s, 3.9f * s, 4f * s, 5f * s, 4f * s)
        close()
    }
    drawPath(p, color, style = st)
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Canvas(Modifier.size(size)) { drawCircle(color, radius = this.size.minDimension / 2f, center = Offset(this.size.width / 2f, this.size.height / 2f)) }
}
