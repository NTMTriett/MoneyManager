package com.example.moneymanager.ui.theme

import androidx.compose.ui.graphics.Color

// --- Vibrant Modern Theme (Blue, Purple, Green) ---

// Blue (Primary)
val MainBlue = Color(0xFF1A73E8)
val LightBlue = Color(0xFFD2E3FC)
val DarkBlue = Color(0xFF174EA6)

// Purple (Secondary)
val MainPurple = Color(0xFF8E24AA)
val LightPurple = Color(0xFFF3E5F5)
val DarkPurple = Color(0xFF4A148C)

// Green (Tertiary/Success)
val MainGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFFE8F5E9)
val DarkGreen = Color(0xFF1B5E20)

// Functional Colors
val ErrorRed = Color(0xFFD32F2F)
val LightRed = Color(0xFFFFEBEE)
val WarningOrange = Color(0xFFF57C00)

// Neutral Colors
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)
val BackgroundGray = Color(0xFFF8F9FA)
val SurfaceGray = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF202124)
val TextSecondary = Color(0xFF5F6368)
val DividerGray = Color(0xFFDADCE0)

// Dark Mode Specific
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkTextPrimary = Color(0xFFE8EAED)
val DarkTextSecondary = Color(0xFFBDC1C6)

// Keep some legacy names if they are used elsewhere to avoid immediate breakages
val Primary = MainBlue
val MediumGreen = MainGreen
val TextGray = TextSecondary
val Background = BackgroundGray
val OnPrimary = Color.White
val CardBackground = Color.White
val Success = MainGreen
val Error = ErrorRed
