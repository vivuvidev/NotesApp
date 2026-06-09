package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.vivuvi.lightnotes.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)

private val tightSpacing = TextUnit(-0.02f, TextUnitType.Em)
private val wideSpacing = TextUnit(0.08f, TextUnitType.Em)

private val baseline = Typography()

val AppTypography = Typography(
    // Note editor title field — 30sp Bold
    displayLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = tightSpacing,
    ),
    displayMedium = baseline.displayMedium.copy(fontFamily = interFontFamily, letterSpacing = tightSpacing),
    displaySmall = baseline.displaySmall.copy(fontFamily = interFontFamily, letterSpacing = tightSpacing),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = interFontFamily, letterSpacing = tightSpacing),
    // Notes Home app bar title — 24sp SemiBold
    headlineMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = tightSpacing,
    ),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = interFontFamily, letterSpacing = tightSpacing),
    // Edit Note app bar title — 20sp Medium
    titleLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    // Note card title — 18sp Bold
    titleMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleSmall = baseline.titleSmall.copy(fontFamily = interFontFamily),
    // Note editor body textarea — 18sp Normal
    bodyLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    // Note card body preview — 14sp Normal
    bodyMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = baseline.bodySmall.copy(fontFamily = interFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = interFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = interFontFamily),
    // Date chips, bottom nav labels — 12sp Medium wide-spaced
    labelSmall = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = wideSpacing,
    ),
)
