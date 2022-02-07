package com.mapbox.navigation.qa_test_app.lifecycle.topbanner

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.dropin.lifecycle.DropInViewCoordinator
import com.mapbox.navigation.dropin.lifecycle.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.flowTripSessionState
import com.mapbox.navigation.dropin.statebinder.EmptyViewBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TopBannerCoordinator(viewGroup: ViewGroup) : DropInViewCoordinator(viewGroup) {

    override fun MapboxNavigation.flowViewBinders(): Flow<DropInViewBinder> =
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
                    EmptyViewBinder()
                }
            }
        }
}
