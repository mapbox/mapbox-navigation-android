package com.mapbox.navigation.dropin.component.roadlabel

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameLabelComponent(
    private val store: Store,
    private val roadNameView: MapboxRoadNameView,
    private val mapStyle: Style,
    @StyleRes private val textAppearance: Int,
    @DrawableRes private val roadNameBackground: Int,
    private val routeShieldApi: MapboxRouteShieldApi = MapboxRouteShieldApi()
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        roadNameView.background = ContextCompat.getDrawable(
            roadNameView.context,
            roadNameBackground
        )
        // setTextAppearance is not deprecated in AppCompatTextView
        roadNameView.setTextAppearance(roadNameView.context, textAppearance)
        store.select { it.location }.observe { matcherResult ->
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
