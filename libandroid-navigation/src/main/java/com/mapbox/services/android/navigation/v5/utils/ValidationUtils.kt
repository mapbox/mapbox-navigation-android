package com.mapbox.services.android.navigation.v5.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import java.util.MissingFormatArgumentException

class ValidationUtils {
    companion object {
        @JvmStatic
        fun validDirectionsRoute(
            directionsRoute: DirectionsRoute,
            defaultMilestonesEnabled: Boolean
        ) {
            if (defaultMilestonesEnabled) {
                val routeOptions = directionsRoute.routeOptions()
                checkNullRouteOptions(routeOptions)
                checkInvalidVoiceInstructions(routeOptions!!)
                checkInvalidBannerInstructions(routeOptions)
            }
        }

        private fun checkNullRouteOptions(routeOptions: RouteOptions?) {
            if (routeOptions == null) {
                throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to include the route options object.")
            }
        }

        private fun checkInvalidVoiceInstructions(routeOptions: RouteOptions) {
            val instructions = routeOptions.voiceInstructions()
            val invalidVoiceInstructions = instructions == null || !instructions
            check(!invalidVoiceInstructions) {
                throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with voice instructions enabled.")
            }
        }

        private fun checkInvalidBannerInstructions(routeOptions: RouteOptions) {
            val instructions = routeOptions.bannerInstructions()
            val invalidBannerInstructions = instructions == null || !instructions
            check(!invalidBannerInstructions) {
                throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with banner instructions enabled.")
            }
        }
    }
}
