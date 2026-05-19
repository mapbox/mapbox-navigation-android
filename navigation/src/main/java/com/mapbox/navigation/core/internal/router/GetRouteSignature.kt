package com.mapbox.navigation.core.internal.router

import com.mapbox.navigator.GetRouteOrigin
import com.mapbox.navigator.GetRouteReason
import com.mapbox.navigator.GetRouteSignature as NativeGetRouteSignature

internal data class GetRouteSignature(
    val reason: Reason,
    val origin: Origin,
) {

    fun toNativeSignature(): NativeGetRouteSignature {
        return NativeGetRouteSignature(
            reason.toNativeReason(),
            origin.toNativeOrigin(),
            "",
            null,
        )
    }

    enum class Reason {
        NEW_ROUTE, REROUTE_BY_DEVIATION, REROUTE_OTHER;

        fun toNativeReason(): GetRouteReason {
            return when (this) {
                NEW_ROUTE -> GetRouteReason.NEW_ROUTE
                REROUTE_BY_DEVIATION -> GetRouteReason.REROUTE_BY_DEVIATION
                REROUTE_OTHER -> GetRouteReason.REROUTE_OTHER
            }
        }
    }

    enum class Origin {
        SDK, APP;

        fun toNativeOrigin(): GetRouteOrigin {
            return when (this) {
                SDK -> GetRouteOrigin.PLATFORM_SDK
                APP -> GetRouteOrigin.CUSTOMER
            }
        }
    }
}
