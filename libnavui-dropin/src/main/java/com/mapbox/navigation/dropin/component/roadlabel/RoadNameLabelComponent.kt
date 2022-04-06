package com.mapbox.navigation.dropin.component.roadlabel

import androidx.core.view.isVisible
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.maps.Style
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.internal.extensions.getStyleId
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class RoadNameLabelComponent(
    private val roadNameView: MapboxRoadNameView,
    private val locationViewModel: LocationViewModel,
    private val mapStyle: Style,
    private val routeShieldApi: MapboxRouteShieldApi = MapboxRouteShieldApi()
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        locationViewModel.state.observe { matcherResult ->
            if (matcherResult != null && matcherResult.isRoadNameAvailable()) {
                roadNameView.isVisible = true

                roadNameView.renderRoadName(matcherResult.road)

                val result = routeShieldApi.getRouteShields(
                    matcherResult.road,
                    DirectionsCriteria.PROFILE_DEFAULT_USER,
                    mapStyle.getStyleId(),
                    mapboxNavigation.navigationOptions.accessToken,
                )
                roadNameView.renderRoadNameWith(result)
            } else {
                roadNameView.isVisible = false
            }
        }
    }

    private fun LocationMatcherResult.isRoadNameAvailable() =
        road.components.isNotEmpty()

    private suspend fun MapboxRouteShieldApi.getRouteShields(
        road: Road,
        userId: String?,
        styleId: String?,
        accessToken: String?
    ): List<Expected<RouteShieldError, RouteShieldResult>> =
        suspendCancellableCoroutine { cont ->
            getRouteShields(road, userId, styleId, accessToken) { result ->
                cont.resume(result)
            }
            cont.invokeOnCancellation {
                // this cancel call will cancel any job invoked through other APIs
                cancel()
            }
        }
}
