package com.ca.screentime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ca.screentime.R

/**
 * IBM Plex Sans KR (SIL Open Font License 1.1, 라이선스: assets/licenses/IBM-Plex-Sans-KR-OFL.txt).
 * 800·900 굵기를 요청하면 가장 가까운 Bold(700)가 쓰인다.
 */
val PlexSansKr = FontFamily(
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_kr_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
)

private val BaseText = TextStyle(
    fontFamily = PlexSansKr,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    lineHeight = TextUnit.Unspecified,
)

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.merge(BaseText),
        displayMedium = t.displayMedium.merge(BaseText),
        displaySmall = t.displaySmall.merge(BaseText),
        headlineLarge = t.headlineLarge.merge(BaseText),
        headlineMedium = t.headlineMedium.merge(BaseText),
        headlineSmall = t.headlineSmall.merge(BaseText),
        titleLarge = t.titleLarge.merge(BaseText),
        titleMedium = t.titleMedium.merge(BaseText),
        titleSmall = t.titleSmall.merge(BaseText),
        bodyLarge = t.bodyLarge.merge(BaseText),
        bodyMedium = t.bodyMedium.merge(BaseText),
        bodySmall = t.bodySmall.merge(BaseText),
        labelLarge = t.labelLarge.merge(BaseText),
        labelMedium = t.labelMedium.merge(BaseText),
        labelSmall = t.labelSmall.merge(BaseText),
    )
}

private val AppColors = darkColorScheme(
    primary = Palette.Clay,
    onPrimary = Palette.NightBg,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
)

/** 앱 화면과 사용시간 초과 화면에 같은 글꼴·색을 적용한다. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
