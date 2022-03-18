package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction.DidReverseGeocode
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.util.Geocoder
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * UIComponent that observes and reverse geocodes Destination.
 */
internal class GeocodingComponent(
    private val destinationViewModel: DestinationViewModel
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val accessToken = mapboxNavigation.navigationOptions.accessToken
        if (accessToken == null) {
            logW(TAG, "GeocodingComponent disabled. Missing AccessToken in MapboxNavigation")
            return
        }

        val geocoder = Geocoder.create(accessToken)
        destinationViewModel.state
            .mapNotNull { it.destination?.point }
            .distinctUntilChanged()
            .observe { point: Point ->
                geocoder.findAddresses(point).onSuccess { features ->
                    destinationViewModel.invoke(DidReverseGeocode(point, features))
                }.onFailure { e ->
                    logW(TAG, "Failed to find address for point= $point; error=$e")
                }
            }
    }

    companion object {
        private val TAG = GeocodingComponent::class.java.simpleName
    }
}
