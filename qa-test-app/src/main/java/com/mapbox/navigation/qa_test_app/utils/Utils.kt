package com.mapbox.navigation.qa_test_app.utils

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints

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

    fun getMapboxSapaAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token_sapa", "string", context.packageName)
        return if (tokenResId != 0) {
            context.getString(tokenResId)
        } else {
            getMapboxAccessToken(context)
        }
    }

    fun readRawFileText(context: Context, res: Int): String =
        context.resources.openRawResource(res).bufferedReader().use { it.readText() }

    fun getRouteOriginPoint(route: DirectionsRoute): Point =
        route.completeGeometryToPoints().first()
}
