package com.echoos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF818CF8),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
)

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
