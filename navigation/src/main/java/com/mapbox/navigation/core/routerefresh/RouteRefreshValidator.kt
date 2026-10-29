@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object RouteRefreshValidator {

    private const val OFFLINE_ROUTES_NOT_REFRESHABLE = "Offline routes can't be refreshed"

    private const val OFFLINE_EV_ONLY =
        "This offline route can't be refreshed. Only an offline EV route that " +
            "requests state_of_charge is refreshed on the device."

    fun joinValidationErrorMessages(
        validations: List<Pair<RouteValidationResult, NavigationRoute>>,
    ): String = validations.mapNotNull { pair ->
        (pair.first as? RouteValidationResult.Invalid)?.let { invalidResult ->
            "${pair.second.id} ${invalidResult.reason}"
        }
    }.joinToString(separator = ". ")

    /**
     * @param onDeviceRefreshSupported asked only for an offline route, see
     * [com.mapbox.navigation.core.directions.session.RouteRefresh.supportsOnDeviceRefresh]
     */
    fun validateRoute(
        route: NavigationRoute,
        onDeviceRefreshSupported: () -> Boolean,
    ): RouteValidationResult = when {
        route.origin == RouterOrigin.CUSTOM_EXTERNAL ->
            RouteValidationResult.Invalid(
                "Custom routes can't be refreshed",
            )
        route.origin == RouterOrigin.OFFLINE && !onDeviceRefreshSupported() ->
            RouteValidationResult.Invalid(OFFLINE_ROUTES_NOT_REFRESHABLE)
        route.origin == RouterOrigin.OFFLINE && !route.requestsStateOfCharge() ->
            RouteValidationResult.Invalid(OFFLINE_EV_ONLY)
        route.routeOptions.enableRefresh() != true ->
            RouteValidationResult.Invalid("RouteOptions#enableRefresh is false")
        route.directionsRoute.requestUuid()?.isNotBlank() != true ->
            RouteValidationResult.Invalid(
                "DirectionsRoute#requestUuid is blank. " +
                    "A route is refreshed by the uuid of the request that built it.",
            )
        route.responseOriginAPI == ResponseOriginAPI.MAP_MATCHING_API ->
            RouteValidationResult.Invalid(
                "Map matched routes can't be refreshed",
            )
        else -> RouteValidationResult.Valid
    }

    private fun NavigationRoute.requestsStateOfCharge(): Boolean =
        routeOptions.annotationsList()?.contains("state_of_charge") == true

    sealed class RouteValidationResult {
        object Valid : RouteValidationResult()
        data class Invalid(val reason: String) : RouteValidationResult()
    }
}
