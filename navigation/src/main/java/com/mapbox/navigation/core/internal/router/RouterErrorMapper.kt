package com.mapbox.navigation.core.internal.router

import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigator.RouterErrorType

/**
 * @throws [IllegalStateException] if [RouterErrorType] is [RouterErrorType.REQUEST_CANCELLED]
 */
internal fun RouterErrorType.mapToSdkRouterFailureType(): String {
    return when (this) {
        RouterErrorType.UNKNOWN -> RouterFailureType.UNKNOWN_ERROR
        RouterErrorType.THROTTLING_ERROR -> RouterFailureType.THROTTLING_ERROR
        RouterErrorType.INPUT_ERROR -> RouterFailureType.INPUT_ERROR
        RouterErrorType.NETWORK_ERROR -> RouterFailureType.NETWORK_ERROR
        RouterErrorType.AUTHENTICATION_ERROR -> RouterFailureType.AUTHENTICATION_ERROR
        RouterErrorType.ROUTE_CREATION_ERROR -> RouterFailureType.ROUTE_CREATION_ERROR
        RouterErrorType.REQUEST_CANCELLED -> error("Should have been processed separately")
        RouterErrorType.MAP_MATCHING_CREATION_ERROR -> RouterFailureType.ROUTE_CREATION_ERROR
        RouterErrorType.MISSING_TILES_ERROR -> RouterFailureType.MISSING_TILES_ERROR
    }
}
