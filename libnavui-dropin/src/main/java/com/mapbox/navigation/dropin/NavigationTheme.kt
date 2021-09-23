package com.mapbox.navigation.dropin

sealed class DropInTheme {
    open val colors: Colors? = null
    open val typography: Typography? = null

    class LightTheme(
        override val colors: Colors,
        override val typography: Typography,
    ) : DropInTheme() {
        val tripProgressText = colors.onPrimary
        val tripProgressDivider = colors.secondary
        val tripProgressBackground = colors.primary
        // Define values for all the other views here
    }

    class DarkTheme(
        override val colors: Colors,
        override val typography: Typography,
    ) : DropInTheme() {
        val tripProgressText = colors.onPrimary
        val tripProgressDivider = colors.secondary
        val tripProgressBackground = colors.primary
        // Define values for all the other views here
    }
}

class Colors(
    val light: Int = 0,
    val warmth: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val primary: Int = light + warmth + contrast + saturation, // whatever this math is
    val secondary: Int = light + warmth + contrast + saturation, // whatever this math is
    val background: Int = light + warmth + contrast + saturation, // whatever this math is
    val onPrimary: Int = light + warmth + contrast + saturation, // whatever this math is
    val onSecondary: Int = light + warmth + contrast + saturation // whatever this math is
)

class Typography
