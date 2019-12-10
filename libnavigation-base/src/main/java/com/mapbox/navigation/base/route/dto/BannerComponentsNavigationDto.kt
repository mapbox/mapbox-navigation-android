package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.model.BannerComponentsNavigation

class BannerComponentsNavigationDto(
    val text: String?,
    val type: String?,
    @SerializedName("abbr")
    val abbreviation: String?,
    @SerializedName("abbr_priority")
    val abbreviationPriority: Int?,
    @SerializedName("imageBaseURL")
    val imageBaseUrl: String?,
    val directions: List<String>?,
    val active: Boolean?
)

internal fun BannerComponentsNavigationDto.mapToModel() = BannerComponentsNavigation(
    text = text,
    type = type,
    abbreviation = abbreviation,
    abbreviationPriority = abbreviationPriority,
    imageBaseUrl = imageBaseUrl,
    directions = directions,
    active = active
)
