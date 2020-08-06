package com.mapbox.navigation.examples.junctonssupport

import com.mapbox.geojson.Point
import com.mapbox.navigation.examples.R

internal class JunctionsProvider {
    private val birminghamJunction = JunctionData(
        junctionLocationName = "Birmingham",
        lineString = R.raw.birmingham_junction,
        junctionEntry = Point.fromLngLat(-1.859601,52.508297)
    )

    val listOfJunctions = listOf(birminghamJunction)
}