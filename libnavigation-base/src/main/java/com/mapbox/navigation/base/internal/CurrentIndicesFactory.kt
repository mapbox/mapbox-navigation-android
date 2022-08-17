package com.mapbox.navigation.base.internal

import com.mapbox.navigation.base.CurrentIndices

object CurrentIndicesFactory {

    fun createIndices(
        legIndex: Int,
        routeGeometryIndex: Int,
        legGeometryIndex: Int?,
    ): CurrentIndices = CurrentIndices(
        legIndex, routeGeometryIndex, legGeometryIndex
    )
}
