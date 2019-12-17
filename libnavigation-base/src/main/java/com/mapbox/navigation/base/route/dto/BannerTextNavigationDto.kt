package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.model.BannerTextNavigation

class BannerTextNavigationDto(
    val text: String?,
    val components: List<BannerComponentsNavigationDto>?,
    val type: String?,
    val modifier: String?,
    val degrees: Double?,
    @SerializedName("driving_side")
    val drivingSide: String?
)

 fun BannerTextNavigationDto.mapToModel() = BannerTextNavigation(
    text = text,
    components = components?.map { it.mapToModel() },
    type = type,
    modifier = modifier,
    degrees = degrees,
    drivingSide = drivingSide
)
