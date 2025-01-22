package com.mapbox.navigation.ui.maps.building.model

/**
 * The state is returned if there is an error highlighting buildings.
 * @param errorMessage an error message
 */
class BuildingError internal constructor(
    val errorMessage: String?,
) : Error(errorMessage)
