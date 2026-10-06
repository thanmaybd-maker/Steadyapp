package com.thanu.steady.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun SteadyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD0F0DC), onPrimary = Color.Black,
            secondary = Color(0xFFD8DAFF), onSecondary = Color.Black,
            background = Color.Black, onBackground = Color.White,
            surface = Color(0xFF101010), onSurface = Color.White,
            onSurfaceVariant = Color(0xFFDDDDDD),
            error = Color(0xFFFFB4AB), onError = Color.Black,
            errorContainer = Color(0xFF361A18), onErrorContainer = Color.White,
            primaryContainer = Color(0xFF183322), onPrimaryContainer = Color.White
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
            labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp)
        ),
        content = content
    )
}
