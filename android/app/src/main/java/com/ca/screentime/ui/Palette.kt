package com.ca.screentime.ui

import androidx.compose.ui.graphics.Color

/** 테마 B · 다크 모드 (승인된 테마 샘플의 색상). */
object Palette {
    // 메인 화면
    val Bg = Color(0xFF17181C)
    val Surface = Color(0xFF22242A)
    val Ink = Color(0xFFF2F3F5)
    val Muted = Color(0xFF9EA3AE)
    val Line = Color(0xFF2F323A)
    val Track = Color(0xFF33363F)
    val Clay = Color(0xFFD97757)
    val ClayText = Color(0xFFF0A285)
    val Green = Color(0xFF6FD49B)
    val GreenDot = Color(0xFF3DBE7A)
    val BadgeBg = Color(0xFF3A2A24)
    val BadgeFg = Color(0xFFF0A285)

    /** 선택된 요일 버튼: 밝은 바탕(Ink) 위의 글자색 */
    val OnSelected = Color(0xFF17181C)
    val SelectedSub = Color(0xFF5C616D)
    val BarMuted = Color(0xFF4A4E58)

    // 사용시간 초과 화면
    val NightBg = Color(0xFF0B0B0C)
    val NightSurface = Color(0xFF1C1C1F)
    val NightInk = Color(0xFFFFFFFF)
    val NightMuted = Color(0xFFA0A0A8)
    val NightLine = Color(0xFF2C2C31)
    val NightOutline = Color(0xFF5A5A63)
    val NightDisabled = Color(0xFF1C1C1F)
    val NightSheetText = Color(0xFFC8C8D0)
    val NightHead = Color(0xFFF0A285)
    val StarBright = Color(0xFF3A3A40)
    val StarDim = Color(0xFF26262B)
}

/** 모서리 둥글기 (테마 B). */
object Radius {
    const val CARD = 18
    const val TILE = 16
    const val DAY_PILL = 12
    const val BIG_BUTTON = 16
}
