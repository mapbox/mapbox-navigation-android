package com.mapbox.navigation.base.internal

object CurrentIndicesFactory {

    fun createIndices(
        legIndex: Int,
        routeGeometryIndex: Int,
        legGeometryIndex: Int?,
    ): CurrentIndices = CurrentIndices(
        legIndex, routeGeometryIndex, legGeometryIndex
    )
}
