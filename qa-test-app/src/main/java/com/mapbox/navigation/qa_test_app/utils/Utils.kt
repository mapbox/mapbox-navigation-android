package com.mapbox.navigation.qa_test_app.utils

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

object Utils {

    fun getMapboxAccessToken(context: Context): String {
        return context.getString(
            context.resources.getIdentifier(
                "mapbox_access_token",
                "string",
                context.packageName
            )
        )
    }

    fun readRawFileText(context: Context, res: Int): String =
        context.resources.openRawResource(res).bufferedReader().use { it.readText() }

    fun getRouteLineString(route: DirectionsRoute): LineString {
        val precision =
            if (route.routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
                Constants.PRECISION_5
            } else {
                Constants.PRECISION_6
            }

        return LineString.fromPolyline(
            route.geometry() ?: "",
            precision
        )
    }

    fun getRouteOriginPoint(route: DirectionsRoute): Point =
        getRouteLineString(route).coordinates().first()
}
