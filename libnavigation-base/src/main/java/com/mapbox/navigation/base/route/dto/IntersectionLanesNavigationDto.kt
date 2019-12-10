package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.IntersectionLanesNavigation

class IntersectionLanesNavigationDto(
    val valid: Boolean?,
    val indications: List<String>?
)

fun IntersectionLanesNavigationDto.mapToModel() = IntersectionLanesNavigation(
    valid = valid,
    indications = indications
)
