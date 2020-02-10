package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.Navigator
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType
import java.util.Date

// TODO Put navigator internal modifier back when MapboxNavigation is converted to Kotlin
internal class MapboxNavigator(val navigator: Navigator) {

    companion object {
        private const val INDEX_FIRST_ROUTE = 0
        private const val GRID_SIZE = 0.0025f
        private const val BUFFER_DILATION: Short = 1
    }

    private val routeHandler: RouteHandler = RouteHandler(this)

    fun updateRoute(route: DirectionsRoute, routeType: DirectionsRouteType) {
        routeHandler.updateRoute(route, routeType)
    }

    // @Synchronized
    // fun updateLegIndex(index: Int): NavigationStatus {
    //     return navigator.changeRouteLeg(INDEX_FIRST_ROUTE, index)
    // }

    /**
     * Gets the history of state changing calls to the navigator this can be used to
     * replay a sequence of events for the purpose of bug fixing.
     *
     * @return a json representing the series of events that happened since the last time
     * history was toggled on
     */
    @Synchronized
    fun retrieveHistory(): String {
        return navigator.history
    }

    /**
     * Toggles the recording of history on or off.
     *
     * @param isEnabled set this to true to turn on history recording and false to turn it off
     * toggling will reset all history call getHistory first before toggling
     * to retain a copy
     */
    @Synchronized
    fun toggleHistory(isEnabled: Boolean) {
        navigator.toggleHistory(isEnabled)
    }

    @Synchronized
    fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        navigator.pushHistory(eventType, eventJsonProperties)
    }

    @Synchronized
    fun retrieveVoiceInstruction(index: Int) {
        navigator.getVoiceInstruction(index) {}
    }

    @Synchronized
    fun setRoute(routeJson: String, routeIndex: Int, legIndex: Int) {
        navigator.setRoute(routeJson, routeIndex, legIndex) {}
    }

    @Synchronized
    fun updateAnnotations(legAnnotationJson: String, routeIndex: Int, legIndex: Int) {
        navigator.updateAnnotations(legAnnotationJson, routeIndex, legIndex)
    }

    @Synchronized
    fun retrieveStatus(date: Date, lagInMilliseconds: Long) {
        // We ask for a point slightly in the future to account for lag in location services
        if (lagInMilliseconds > 0) {
            date.time = date.time + lagInMilliseconds
        }
        navigator.getStatus(date) {}
    }

    fun updateLocation(raw: Location) {
        val fixedLocation = buildFixLocationFromLocation(raw)
        synchronized(this) {
            navigator.updateLocation(fixedLocation)
        }
    }

    @Synchronized
    fun retrieveBannerInstruction(index: Int) {
        navigator.getBannerInstruction(index) {}
    }

    @Synchronized
    fun retrieveRouteGeometryWithBuffer() {
        navigator.getRouteBufferGeoJson(GRID_SIZE, BUFFER_DILATION) {}
    }

    @Synchronized
    fun retrieveElectronicHorizon(request: String) {
        navigator.getElectronicHorizon(request) {}
    }

    private fun buildFixLocationFromLocation(location: Location): FixLocation {
        val time = Date()
        val rawPoint = Point.fromLngLat(location.longitude, location.latitude)
        val speed = checkFor(location.speed)
        val bearing = checkFor(location.bearing)
        val altitude = checkFor(location.altitude.toFloat())
        val horizontalAccuracy = checkFor(location.accuracy)
        val provider = location.provider

        return FixLocation(
                rawPoint,
                time,
                speed,
                bearing,
                altitude,
                horizontalAccuracy,
                provider
        )
    }

    private fun checkFor(value: Float?): Float? {
        return if (value == 0f) {
            null
        } else value
    }
}
