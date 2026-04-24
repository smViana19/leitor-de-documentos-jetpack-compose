package com.example.leitordocumento_compose.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.R

// Set of Material typography styles to start with


val FamiliaInter = FontFamily(
    Font(R.font.inter, FontWeight.Light),
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

val FamiliaManrope = FontFamily(
    Font(R.font.manrope_light, FontWeight.Light),
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val AppTipografia = Typography(

    // -------------------------------------------------------------------------
    // DISPLAY — Voz Editorial (Manrope)
    // Usado em estados vazios, telas hero, momentos de impacto
    // Design system: display-lg = 3.5rem
    // -------------------------------------------------------------------------
    displayLarge = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,   // ~3.5rem
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    // -------------------------------------------------------------------------
    // HEADLINE — Títulos de seção (Manrope)
    // Design system: headline-sm = 1.5rem → títulos de pastas/documentos
    // -------------------------------------------------------------------------
    headlineLarge = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FamiliaManrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,   // ~1.5rem — títulos de pastas
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
// -------------------------------------------------------------------------
    // TITLE — Voz Funcional (Inter)
    // Design system: title-sm = 1rem → labels de formulários
    // -------------------------------------------------------------------------
    titleLarge = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,   // 1rem — labels de formulários
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // -------------------------------------------------------------------------
    // BODY — Corpo de texto (Inter)
    // Design system: body-md = 0.875rem → metadados de documentos
    // Contraste tonal: onSurface para títulos, onSurfaceVariant para corpo
    // -------------------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,   // ~0.875rem — metadados de documentos
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // -------------------------------------------------------------------------
    // LABEL — Rótulos e chips (Inter)
    // Design system: label-md → labels de campos de input
    // -------------------------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,   // label-md — campos de input
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FamiliaInter,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)