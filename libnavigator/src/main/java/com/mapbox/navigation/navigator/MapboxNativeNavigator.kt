package com.mapbox.navigation.navigator

import android.hardware.SensorEvent
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.HttpInterface
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.VoiceInstruction
import java.util.Date

interface MapboxNativeNavigator {

    companion object {
        private const val INDEX_FIRST_ROUTE = 0
        private const val INDEX_FIRST_LEG = 0
        private const val GRID_SIZE = 0.0025f
        private const val BUFFER_DILATION: Short = 1
    }

    // Route following

    fun updateLocation(rawLocation: Location): Boolean
    fun updateSensorEvent(sensorEvent: SensorEvent): Boolean
    fun getStatus(date: Date): TripStatus

    // Routing

    fun setRoute(
        route: DirectionsRoute,
        routeIndex: Int = INDEX_FIRST_ROUTE,
        legIndex: Int = INDEX_FIRST_LEG
    ): NavigationStatus

    fun updateAnnotations(legAnnotationJson: String, routeIndex: Int, legIndex: Int): Boolean
    fun getBannerInstruction(index: Int): BannerInstruction?
    fun getRouteGeometryWithBuffer(
        gridSize: Float = GRID_SIZE,
        bufferDilation: Short = BUFFER_DILATION
    ): String?

    fun updateLegIndex(routeIndex: Int, legIndex: Int): NavigationStatus

    // Free Drive

    fun getElectronicHorizon(request: String): RouterResult

    // Offline

    fun cacheLastRoute()

    fun configureRouter(routerParams: RouterParams, httpClient: HttpInterface?): Long
    fun getRoute(url: String): RouterResult
    fun unpackTiles(tarPath: String, destinationPath: String): Long
    fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long

    // History traces

    fun getHistory(): String
    fun toggleHistory(isEnabled: Boolean)
    fun addHistoryEvent(eventType: String, eventJsonProperties: String)

    // Configuration

    fun getConfig(): NavigatorConfig
    fun setConfig(config: NavigatorConfig?)

    // Other

    fun getVoiceInstruction(index: Int): VoiceInstruction?
}
