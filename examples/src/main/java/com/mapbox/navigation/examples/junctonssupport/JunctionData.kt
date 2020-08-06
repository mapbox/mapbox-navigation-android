package com.mapbox.navigation.examples.junctonssupport

import androidx.annotation.RawRes
import com.mapbox.geojson.Point

data class JunctionData(
    val junctionLocationName: String,
    @RawRes val lineString: Int,
    val junctionEntry: Point
)