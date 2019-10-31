package com.mapbox.navigation.base.model.banner

data class BannerComponent(
    val type: String,
    val text: String,
    val abbr: String?,
    val abbrPriority: String?,
    val imageBaseUrl: String?,
    val active: Boolean?,
    val directions: ArrayList<String>?
)
