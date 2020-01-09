package com.mapbox.navigation.base.banner

data class BannerInstruction(private val primary: BannerSection) {

    fun primary(): BannerSection = primary
}
