package com.mapbox.navigation.ui.maps.building.model

import com.mapbox.maps.QueriedRenderedFeature

/**
 * The state is returned when buildings have been queried.
 * @property buildings The list containing buildings. It is empty when there are no buildings found.
 */
class BuildingValue internal constructor(
    val buildings: List<QueriedRenderedFeature>,
)
