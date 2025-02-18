package com.mapbox.navigation.magpt.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

sealed class MapGPTColor(val light: Color, val dark: Color) {
    object TextPrimary : MapGPTColor(
        light = Color(0xFF05070A),
        dark = Color(0xFFEDEFF2),
    )
    object TextSecondary : MapGPTColor(
        light = Color(0xFF536275),
        dark = Color(0xFFA6B2C6),
    )

    object BackgroundPrimary : MapGPTColor(
        light = Color(0xFFFFFFFF),
        dark = Color(0xFF181B20),
    )
}

sealed class MapGPTTypography(
    val phoneSize: TextUnit,
    val tabletSize: TextUnit,
    val weight: FontWeight,
) {
    object Title3 : MapGPTTypography(
        // phoneSize should be 36, but we use 48 instead because the HVAC card design
        // is not consistent and uses title2 for phones, but title3 for tablets
        phoneSize = 48.sp,
        tabletSize = 68.sp,
        weight = FontWeight.W600,
    )
    object Title5 : MapGPTTypography(
        phoneSize = 24.sp,
        tabletSize = 40.sp,
        weight = FontWeight.W600,
    )
    object Title7 : MapGPTTypography(
        phoneSize = 18.sp,
        tabletSize = 28.sp,
        weight = FontWeight.W600,
    )

    object Body5 : MapGPTTypography(
        phoneSize = 18.sp,
        tabletSize = 28.sp,
        weight = FontWeight.W400,
    )

    fun toTextStyle(isTablet: Boolean): TextStyle {
        val fontSize = if (isTablet) tabletSize else phoneSize
        return TextStyle(
            fontSize = fontSize,
            fontWeight = weight,
        )
    }
}
