package com.mapbox.navigation.core.reroute.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.reroute.NativeMapboxRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import kotlinx.coroutines.flow.StateFlow

/**
 * Returns a [StateFlow] of [NativeRerouteControllerState] if this controller is a
 * [NativeMapboxRerouteController], or `null` for other implementations.
 *
 * This flow exposes the fine-grained internal state of the native reroute controller,
 * including the distinction between [NativeRerouteControllerState.WaitingForResponse]
 * and [NativeRerouteControllerState.RouteObjectsParsing] that is not visible through
 * the public [com.mapbox.navigation.core.reroute.RerouteStateV2] API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun RerouteController.nativeRerouteControllerStateFlow(): StateFlow<NativeRerouteControllerState>? {
    return (this as? NativeMapboxRerouteController)?.nativeControllerStateFlow
}
