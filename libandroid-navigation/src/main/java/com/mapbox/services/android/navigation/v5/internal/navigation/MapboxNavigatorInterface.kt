package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Geometry
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.VoiceInstruction
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType
import java.util.Date

interface MapboxNavigatorInterface {
    fun updateRoute(route: DirectionsRoute, routeType: DirectionsRouteType)
    fun updateLegIndex(index: Int): NavigationStatus
    fun retrieveHistory(): String
    fun toggleHistory(isEnabled: Boolean)
    fun addHistoryEvent(eventType: String, eventJsonProperties: String)
    fun retrieveVoiceInstruction(index: Int): VoiceInstruction?
    fun setRoute(routeJson: String, routeIndex: Int, legIndex: Int): NavigationStatus
    fun updateAnnotations(legAnnotationJson: String, routeIndex: Int, legIndex: Int): Boolean
    fun retrieveStatus(date: Date, lagInMilliseconds: Long): NavigationStatus
    fun updateLocation(raw: Location)
    fun retrieveBannerInstruction(index: Int): BannerInstruction?
    fun retrieveRouteGeometryWithBuffer(): Geometry?
}
