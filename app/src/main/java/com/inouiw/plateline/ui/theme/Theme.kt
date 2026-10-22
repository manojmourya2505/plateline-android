package com.inouiw.plateline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inouiw.plateline.R

val Yellow = Color(0xFFFFC93C)
val YellowLight = Color(0xFFFFD459)
val YellowDeep = Color(0xFFF2B518)
val Ink = Color(0xFF111317)
val Paper = Color(0xFFEFF0F3)
val White = Color(0xFFFFFFFF)
val Slate = Color(0xFF5F6572)
val PlateBlue = Color(0xFF1D3FA8)
val Amber = Color(0xFFE3A800)
val Line = Color(0xFFE2E4EA)
val Good = Color(0xFF1E9E5A)

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

val Sora = variable(R.font.sora)
val DmSans = variable(R.font.dm_sans)
val Barlow = FontFamily(Font(R.font.barlow_condensed_bold, FontWeight.Bold))

fun sora(size: Int, weight: Int = 700, color: Color = Ink) = TextStyle(
    fontFamily = Sora, fontSize = size.sp, fontWeight = FontWeight(weight),
    letterSpacing = (-0.03f * size).sp, color = color, lineHeight = (size * 1.15f).sp
)

fun dm(size: Int, weight: Int = 400, color: Color = Ink) = TextStyle(
    fontFamily = DmSans, fontSize = size.sp, fontWeight = FontWeight(weight),
    color = color, lineHeight = (size * 1.4f).sp
)

@Composable
fun PlatelineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Ink, secondary = Yellow, background = Paper, surface = White),
        content = content
    )
}
