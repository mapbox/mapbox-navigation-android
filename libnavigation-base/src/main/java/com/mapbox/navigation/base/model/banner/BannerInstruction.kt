package com.mapbox.navigation.base.model.banner

data class BannerInstruction(
    val primary: BannerSection,
    val secondary: BannerSection?,
    val sub: BannerSection?,
    val remainingStepDistance: Float,
    val index: Int
)
