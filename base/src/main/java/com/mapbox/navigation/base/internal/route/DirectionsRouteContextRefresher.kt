package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.directions.route.DirectionsRouteContext

/**
 * Wraps the native [DirectionsRouteContext.refreshRoute] call, the same way
 * [com.mapbox.navigation.base.internal.SDKRouteParser] wraps native parsing, so that it can be
 * swapped out in unit tests where no native peer is available.
 *
 * A mock of [DirectionsRouteContext] is not an alternative: every one of its members is `external`,
 * and merely recording a stub for one invokes the native implementation, which fails with
 * `UnsatisfiedLinkError` when the library isn't loaded. Substituting this wrapper is the only seam.
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface DirectionsRouteContextRefresher {
    fun refresh(
        context: DirectionsRouteContext,
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
    ): Expected<String, DirectionsRouteContext>

    companion object {
        val default: DirectionsRouteContextRefresher = NativeDirectionsRouteContextRefresher()
    }
}

private class NativeDirectionsRouteContextRefresher : DirectionsRouteContextRefresher {
    override fun refresh(
        context: DirectionsRouteContext,
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
    ): Expected<String, DirectionsRouteContext> =
        context.refreshRoute(refreshResponse, legIndex, legGeometryIndex)
}
