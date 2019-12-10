package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.BannerInstructionsNavigation

internal class BannerInstructionsNavigationDto(
    val distanceAlongGeometry: Double,
    val primary: BannerTextNavigationDto?,
    val secondary: BannerTextNavigationDto?,
    val sub: BannerTextNavigationDto?
)

internal fun BannerInstructionsNavigationDto.mapToModel() = BannerInstructionsNavigation(
    distanceAlongGeometry = distanceAlongGeometry,
    primary = primary?.mapToModel(),
    secondary = secondary?.mapToModel(),
    sub = sub?.mapToModel()
)
