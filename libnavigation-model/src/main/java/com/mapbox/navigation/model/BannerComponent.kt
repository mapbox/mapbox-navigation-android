package com.mapbox.navigation.model

data class BannerComponent(
    val type: String,
    val text: String,
    val abbr: String?,
    val abbrPriority: String?,
    val imageBaseUrl: String?,
    val active: Boolean?,
    val directions: ArrayList<String>?
)
