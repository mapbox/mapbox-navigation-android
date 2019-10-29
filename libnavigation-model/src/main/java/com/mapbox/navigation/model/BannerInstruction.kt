package com.mapbox.navigation.model

data class BannerInstruction(
    val primary: BannerSection,
    val secondary: BannerSection?,
    val sub: BannerSection?,
    val remainingStepDistance: Float,
    val index: Int
)
