package com.ishawahsib.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF020617)
val SlateSurface = Color(0xFF0F172A)
val SlateSurfaceVariant = Color(0xFF1E293B)
val DawnAmber = Color(0xFFF59E0B)
val DawnAmberDark = Color(0xFFD97706)
val SoftText = Color(0xFFF8FAFC)
val MutedText = Color(0xFF94A3B8)
val DangerRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)

private val IshaDarkColors = darkColorScheme(
    primary = DawnAmber,
    onPrimary = Color(0xFF111827),
    primaryContainer = Color(0xFF451A03),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    background = ObsidianBackground,
    onBackground = SoftText,
    surface = SlateSurface,
    onSurface = SoftText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = MutedText,
    error = DangerRed,
    outline = Color(0xFF334155)
)

@Composable
fun IshaWahsibTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IshaDarkColors,
        content = content
    )
}
