package com.mapbox.services.android.navigation.v5.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import java.util.MissingFormatArgumentException

internal class ValidationUtils {
    companion object {
        @JvmStatic
        fun validDirectionsRoute(
            directionsRoute: DirectionsRoute,
            defaultMilestonesEnabled: Boolean
        ) {
            if (defaultMilestonesEnabled) {
                val routeOptions = directionsRoute.routeOptions()
                checkNullRouteOptions(routeOptions)
                checkInvalidVoiceInstructions(routeOptions)
                checkInvalidBannerInstructions(routeOptions)
            }
        }

        private fun checkNullRouteOptions(routeOptions: RouteOptions?) {
            if (routeOptions == null) {
                throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to include the route options object.")
            }
        }

        private fun checkInvalidVoiceInstructions(routeOptions: RouteOptions?) {
            routeOptions?.let { options ->
                val instructions = options.voiceInstructions()
                val isValidVoiceInstructions = instructions != null && instructions
                check(isValidVoiceInstructions) {
                    throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with voice instructions enabled.")
                }
            }
                    ?: throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with voice instructions enabled.")
        }

        private fun checkInvalidBannerInstructions(routeOptions: RouteOptions?) {
            routeOptions?.let { options ->
                val instructions = options.bannerInstructions()
                val isValidBannerInstructions = instructions != null && instructions
                check(isValidBannerInstructions) {
                    throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with banner instructions enabled.")
                }
            }
                    ?: throw MissingFormatArgumentException("Using the default milestones requires the " + "directions route to be requested with banner instructions enabled.")
        }
    }
}
