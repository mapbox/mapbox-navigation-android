package com.mapbox.navigation.ui.base

sealed class UIMode {
    class LightMode: UIMode()
    class DarkMode: UIMode()
}
