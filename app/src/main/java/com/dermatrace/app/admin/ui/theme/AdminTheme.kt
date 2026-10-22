package com.dermatrace.app.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AdminBg = Color(0xFF000000)
val AdminCardBg = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)
val AdminCardBorder = Color(0x24FFFFFF) // rgba(255,255,255,0.14)
val AdminCardBorderHover = Color(0x4DFFFFFF)
val AdminTextPrimary = Color(0xFFE2E8F0)
val AdminTextMuted = Color(0xFF94A3B8)
val AdminWhite = Color(0xFFFFFFFF)
val AdminAmber = Color(0xFFFBBF24)
val AdminAmberBg = Color(0x33FBBF24)
val AdminRed = Color(0xFFF87171)

// Translucent White replacing Cyan/Teal (matching patient app design)
val AdminTranslucentWhite = Color(0x29FFFFFF) // rgba(255,255,255,0.16)
val AdminTranslucentBorder = Color(0x40FFFFFF) // rgba(255,255,255,0.25)
val AdminTeal = Color(0x33FFFFFF) // patient-side translucent button background
val AdminCyan = Color(0xFFF8FAFC) // crisp elegant text/accent replacing cyan
val AdminInputBg = Color(0x47000000) // rgba(0,0,0,0.28)
val AdminBarMuted = Color(0x99E2E8F0) // rgba(226,232,240,0.6)

private val DarkColorScheme = darkColorScheme(
    primary = AdminWhite,
    onPrimary = Color.Black,
    secondary = AdminAmber,
    onSecondary = Color.Black,
    background = AdminBg,
    onBackground = AdminTextPrimary,
    surface = AdminCardBg,
    onSurface = AdminTextPrimary
)

@Composable
fun DermaAdminTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
