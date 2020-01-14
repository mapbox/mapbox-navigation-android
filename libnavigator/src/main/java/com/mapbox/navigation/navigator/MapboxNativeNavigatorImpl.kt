package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.HttpInterface
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.VoiceInstruction
import java.util.Date

object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    private val navigator: Navigator = Navigator()

    init {
        System.loadLibrary("navigator-android")
    }

    // Route following

    override fun updateLocation(rawLocation: Location): Boolean =
        navigator.updateLocation(rawLocation.toFixLocation())

    override fun getStatus(date: Date): TripStatus {
        val status = navigator.getStatus(date)
        return TripStatus(
            status.location.toLocation(),
            status.getRouteProgress()
        )
    }

    // Routing

    override fun setRoute(routeJson: String, routeIndex: Int, legIndex: Int): NavigationStatus =
        navigator.setRoute(routeJson, routeIndex, legIndex)

    override fun updateAnnotations(
        legAnnotationJson: String,
        routeIndex: Int,
        legIndex: Int
    ): Boolean = navigator.updateAnnotations(legAnnotationJson, routeIndex, legIndex)

    override fun getBannerInstruction(index: Int): BannerInstruction? =
        navigator.getBannerInstruction(index)

    override fun getRouteGeometryWithBuffer(gridSize: Float, bufferDilation: Short): String? =
        navigator.getRouteBufferGeoJson(gridSize, bufferDilation)

    override fun updateLegIndex(routeIndex: Int, legIndex: Int): NavigationStatus =
        navigator.changeRouteLeg(routeIndex, legIndex)

    // Free Drive

    override fun getElectronicHorizon(request: String): RouterResult =
        navigator.getElectronicHorizon(request)

    // Offline

    override fun configureRouter(routerParams: RouterParams, httpClient: HttpInterface): Long =
        navigator.configureRouter(routerParams, httpClient)

    override fun getRoute(url: String): RouterResult = navigator.getRoute(url)

    override fun unpackTiles(tarPath: String, destinationPath: String): Long =
        navigator.unpackTiles(tarPath, destinationPath)

    override fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long =
        navigator.removeTiles(tilePath, southwest, northeast)

    // History traces

    override fun getHistory(): String = navigator.history

    override fun toggleHistory(isEnabled: Boolean) {
        navigator.toggleHistory(isEnabled)
    }

    override fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        navigator.pushHistory(eventType, eventJsonProperties)
    }

    // Configuration

    override fun getConfig(): NavigatorConfig = navigator.config

    override fun setConfig(config: NavigatorConfig?) {
        navigator.setConfig(config)
    }

    // Other

    override fun getVoiceInstruction(index: Int): VoiceInstruction? =
        navigator.getVoiceInstruction(index)

    private fun Location.toFixLocation() = FixLocation(
        Point.fromLngLat(this.longitude, this.latitude),
        Date(this.time),
        this.speed,
        this.bearing,
        this.altitude.toFloat(),
        this.accuracy,
        this.provider
    )

    private fun FixLocation.toLocation(): Location = Location(this.provider).also {
        it.latitude = this.coordinate.latitude()
        it.longitude = this.coordinate.longitude()
        it.time = this.time.time
        it.speed = this.speed ?: 0f
        it.bearing = this.bearing ?: 0f
        it.altitude = this.altitude?.toDouble() ?: 0.0
        it.accuracy = this.accuracyHorizontal ?: 0f
    }

    private fun NavigationStatus.getRouteProgress(): RouteProgress {
        return RouteProgress("")
    }
}
