package com.mapbox.navigation.core.reroute.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.isRetryable
import com.mapbox.navigation.core.reroute.PreRouterFailure

/**
 * Internal state for [com.mapbox.navigation.core.reroute.NativeMapboxRerouteController].
 *
 * Compared to [com.mapbox.navigation.core.reroute.RerouteStateV2], splits the fetching stage into:
 * - [WaitingForResponse] — request sent, waiting for the network response
 * - [RouteObjectsParsing] — response received, route objects are being parsed
 *
 * Both stages map to [com.mapbox.navigation.core.reroute.RerouteStateV2.FetchingRoute] in the
 * public V2 state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class NativeRerouteControllerState {

    class Idle : NativeRerouteControllerState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int = javaClass.hashCode()

        override fun toString(): String = "Idle"
    }

    class Interrupted : NativeRerouteControllerState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int = javaClass.hashCode()

        override fun toString(): String = "Interrupted"
    }

    class Failed internal constructor(
        val message: String,
        val throwable: Throwable? = null,
        val reasons: List<RouterFailure>? = null,
        internal val preRouterReasons: List<PreRouterFailure> = emptyList(),
    ) : NativeRerouteControllerState() {

        val isRetryable get() = reasons.isRetryable || preRouterReasons.any { it.isRetryable }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Failed
            if (message != other.message) return false
            if (throwable != other.throwable) return false
            if (preRouterReasons != other.preRouterReasons) return false
            return reasons == other.reasons
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (throwable?.hashCode() ?: 0)
            result = 31 * result + (reasons?.hashCode() ?: 0)
            result = 31 * result + preRouterReasons.hashCode()
            return result
        }

        override fun toString(): String =
            "Failed(" +
                "message='$message', " +
                "throwable=$throwable, " +
                "reasons=$reasons, " +
                "preRouterReasons=$preRouterReasons" +
                ")"
    }

    /**
     * A route request has been sent and the SDK is waiting for the server response.
     */
    class WaitingForResponse : NativeRerouteControllerState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int = javaClass.hashCode()

        override fun toString(): String = "WaitingForResponse"
    }

    /**
     * A response has been received and route objects are being parsed.
     */
    class RouteObjectsParsing : NativeRerouteControllerState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int = javaClass.hashCode()

        override fun toString(): String = "RouteObjectsParsing"
    }

    class RouteFetched internal constructor(
        @RouterOrigin val routerOrigin: String,
    ) : NativeRerouteControllerState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RouteFetched
            return routerOrigin == other.routerOrigin
        }

        override fun hashCode(): Int = routerOrigin.hashCode()

        override fun toString(): String = "RouteFetched(routerOrigin='$routerOrigin')"
    }

    sealed class Deviation : NativeRerouteControllerState() {

        class ApplyingRoute : Deviation() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            override fun hashCode(): Int = javaClass.hashCode()

            override fun toString(): String = "Deviation.ApplyingRoute"
        }

        class RouteIgnored : Deviation() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            override fun hashCode(): Int = javaClass.hashCode()

            override fun toString(): String = "Deviation.RouteIgnored"
        }
    }
}
