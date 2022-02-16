package com.mapbox.navigation.qa_test_app.lifecycle.topbanner

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.extensions.flowTripSessionState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TopBannerCoordinator(viewGroup: ViewGroup) : UICoordinator<ViewGroup>(viewGroup) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> =
        combine(
            flowTripSessionState(), flowRoutesUpdated()
        ) { tripSessionState, routesUpdatedResult ->
            when {
                tripSessionState == TripSessionState.STARTED &&
                    routesUpdatedResult.routes.isNotEmpty() -> {
                    ManeuverViewViewBinder()
                }
                // TODO add new view builders for different states
                else -> {
                    EmptyBinder()
                }
            }
        }
}
