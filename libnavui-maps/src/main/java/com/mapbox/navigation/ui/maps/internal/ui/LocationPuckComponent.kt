package com.mapbox.navigation.ui.maps.internal.ui

import android.animation.ValueAnimator
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@ExperimentalPreviewMapboxNavigationAPI
class LocationPuckComponent(
    private val map: MapboxMap,
    private val locationComponentPlugin: LocationComponentPlugin,
    private val locationPuck: LocationPuck,
    private val locationProvider: NavigationLocationProvider
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            locationProvider.waitForFirstLocation()
            map.getStyle {
                locationComponentPlugin.apply {
                    setLocationProvider(locationProvider)
                    locationPuck = this@LocationPuckComponent.locationPuck
                    enabled = true
                }
            }
        }
    }

    private suspend fun NavigationLocationProvider.waitForFirstLocation() =
        suspendCancellableCoroutine<Unit> { cont ->
            if (lastLocation != null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            val consumer = object : LocationConsumer {
                override fun onBearingUpdated(
                    vararg bearing: Double,
                    options: (ValueAnimator.() -> Unit)?
                ) = Unit

                override fun onLocationUpdated(
                    vararg location: Point,
                    options: (ValueAnimator.() -> Unit)?
                ) {
                    cont.resume(Unit)
                    unRegisterLocationConsumer(this)
                }

                override fun onPuckBearingAnimatorDefaultOptionsUpdated(
                    options: ValueAnimator.() -> Unit
                ) = Unit

                override fun onPuckLocationAnimatorDefaultOptionsUpdated(
                    options: ValueAnimator.() -> Unit
                ) =
                    Unit
            }
            registerLocationConsumer(consumer)
            cont.invokeOnCancellation { unRegisterLocationConsumer(consumer) }
        }
}
