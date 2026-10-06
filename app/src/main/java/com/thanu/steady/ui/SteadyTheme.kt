package com.thanu.steady.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import com.thanu.steady.data.ExpandedProfile

@Composable
fun SteadyTheme(profile: ExpandedProfile = ExpandedProfile(), content: @Composable () -> Unit) {
    val dark = profile.theme == "DARK" || profile.theme == "SYSTEM" && isSystemInDarkTheme()
    val daybook = profile.palette == "DAYBOOK"
    val density = LocalDensity.current
    val colours = if (dark) darkColorScheme(
        primary = Color(0xFF82DAB3), onPrimary = Color(0xFF002114),
        background = if (profile.highContrast) Color.Black else Color(0xFF111A18), onBackground = Color(0xFFF2F5F3),
        surface = if (profile.highContrast) Color.Black else Color(0xFF1B2823), onSurface = Color(0xFFF2F5F3),
        onSurfaceVariant = Color(0xFFD4E0D9), outline = Color(0xFF9AADA1),
        primaryContainer = Color(0xFF173C2B), onPrimaryContainer = Color(0xFFF2F5F3),
        error = Color(0xFFFFB4AB), errorContainer = Color(0xFF381614), onErrorContainer = Color.White
    ) else lightColorScheme(
        primary = if (daybook) Color(0xFF25483D) else Color(0xFF006C49), onPrimary = Color.White,
        background = if (daybook) Color(0xFFF4F0E6) else Color(0xFFFAF8FF),
        surface = if (daybook) Color(0xFFFFFCF6) else Color.White,
        onBackground = if (daybook) Color(0xFF233A33) else Color(0xFF131B2E),
        onSurface = if (daybook) Color(0xFF233A33) else Color(0xFF131B2E),
        onSurfaceVariant = if (daybook) Color(0xFF34463D) else Color(0xFF344155),
        outline = Color(0xFF586A61), primaryContainer = Color(0xFFDDEFE4), onPrimaryContainer = Color(0xFF163627),
        error = Color(0xFF9B1C1C), errorContainer = Color(0xFFFFE6E1), onErrorContainer = Color(0xFF651010)
    )
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * profile.textScale)) {
    MaterialTheme(
        colorScheme = colours,
        shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp)),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
            bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
            bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
            titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
            headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp),
            labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
            labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        ),
        content = content
    )
    }
}
