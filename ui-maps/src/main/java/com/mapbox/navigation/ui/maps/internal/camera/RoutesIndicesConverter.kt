package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints

internal class RoutesIndicesConverter {

    // key - route id, each element in a value list is a lambda that converts shapeIndex
    // (leg geometry index) to step geometry index for a set leg index and step index
    // Example: indicesConverter["routeId2"][1][4](15) means:
    // "for alternative with id 'routeId2', for leg #1 for step#15
    // convert legGeometryIndex=15, result is current step geometry index"
    private var lambdasMap: MutableMap<String, List<List<(Int) -> Int?>>> = hashMapOf()

    fun onRoutesChanged(routes: List<NavigationRoute>) {
        lambdasMap = hashMapOf()
        routes.forEachIndexed { index, route ->
            lambdasMap[routes[index].id] = processIndices(
                route.directionsRoute,
            )
        }
    }

    fun convert(id: String, legIndex: Int, stepIndex: Int, legGeometryIndex: Int): Int? {
        return lambdasMap[id]
            ?.getOrNull(legIndex)
            ?.getOrNull(stepIndex)
            ?.invoke(legGeometryIndex)
    }

    private fun processIndices(directionsRoute: DirectionsRoute): List<List<(Int) -> Int?>> {
        return directionsRoute.legs()?.map { leg ->
            var prevPointsCount = 0
            leg.steps()?.map { step ->
                val prevPointsCountSnapshot = prevPointsCount
                val stepSize = directionsRoute.stepGeometryToPoints(step).size
                val lambda = { shapeIndex: Int ->
                    val result = shapeIndex - prevPointsCountSnapshot
                    if (result in 0 until stepSize) result else null
                }
                prevPointsCount += stepSize - 1
                lambda
            }.orEmpty()
        }.orEmpty()
    }
}
