package com.mapbox.navigation.base.model.banner

data class BannerSection(
    val text: String,
    val type: String?,
    val modifier: String?,
    val degrees: Int?,
    val drivingSide: String?,
    val components: ArrayList<BannerComponent>?
)
