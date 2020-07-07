package com.mapbox.navigation.core.fasterroute

import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute

internal class LegacyRouteComparator {
    fun compareRoutes(currentRoute: DirectionsRoute, alternativeRoute: DirectionsRoute): Int {
        val chosenRouteLegDescription = obtainRouteLegDescriptionFrom(currentRoute)
        val routeLegDescription = obtainRouteLegDescriptionFrom(alternativeRoute)
        Log.i("faster_route_debug","faster_route_debug current route $chosenRouteLegDescription")
        Log.i("faster_route_debug","faster_route_debug alternative route $routeLegDescription")
        return DamerauLevenshteinAlgorithm.execute(chosenRouteLegDescription, routeLegDescription)
    }

    private fun obtainRouteLegDescriptionFrom(route: DirectionsRoute): String {
        val routeLegs = route.legs()
        val routeLegDescription = StringBuilder()
        for (leg in routeLegs!!) {
            routeLegDescription.append(leg.summary())
        }
        return routeLegDescription.toString()
    }
}