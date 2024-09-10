package com.mapbox.navigation.core.reroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.isRetryable
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Provides API to trigger reroute and observe reroute state.
 *
 * @see [MapboxNavigation.getRerouteController] and [MapboxNavigation.setRerouteEnabled]
 */
@UiThread
abstract class RerouteController internal constructor() {

    /**
     * Invoked whenever re-route is needed. For instance when a driver is off-route. Called just after
     * an off-route event.
     *
     * @see [OffRouteObserver]
     */
    abstract fun reroute(callback: RoutesCallback)

    /**
     * Reroute state
     */
    abstract val state: RerouteState

    /**
     * Add a RerouteStateObserver to collection and immediately invoke [rerouteStateObserver] with current
     * re-route state.
     *
     * @return `true` if the element has been added, `false` if the element is already present in the collection.
     */
    abstract fun registerRerouteStateObserver(rerouteStateObserver: RerouteStateObserver): Boolean

    /**
     * Remove [rerouteStateObserver] from collection of observers.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    abstract fun unregisterRerouteStateObserver(rerouteStateObserver: RerouteStateObserver): Boolean

    /**
     * Route Callback is useful to set new route(s) on reroute event. Doing the same as
     * [MapboxNavigation.setNavigationRoutes].
     */
    @UiThread
    fun interface RoutesCallback {
        /**
         * Called whenever new route(s) has been fetched.
         * @see [MapboxNavigation.setNavigationRoutes]
         */
        fun onNewRoutes(routes: List<NavigationRoute>, @RouterOrigin routerOrigin: String)
    }

    /**
     * [RerouteState] observer
     */
    fun interface RerouteStateObserver {

        /**
         * Invoked whenever re-route state has changed.
         */
        fun onRerouteStateChanged(rerouteState: RerouteState)
    }
}

/**
 * Reroute state
 */
sealed class RerouteState {

    /**
     * [RerouteController] is idle.
     */
    object Idle : RerouteState()

    /**
     * Reroute has been interrupted.
     *
     * Might be triggered when:
     * - [MapboxNavigation.setNavigationRoutes] called;
     * - [MapboxNavigation.requestRoutes] called;
     * - another reroute call [RerouteController.reroute];
     * - when reroute has been disabled by [MapboxNavigation.setRerouteEnabled];
     * - user is back to route, see [OffRouteObserver];
     * - from the SDK internally if another route request has been requested (only when using the default
     * implementation [MapboxRerouteController]).
     */
    object Interrupted : RerouteState()

    /**
     * Re-route request has failed.
     *
     * You can call [RerouteController.reroute] to retry the request.
     *
     * @param message describes error
     * @param throwable optional throwable
     * @param reasons optional reasons for the failure
     */
    class Failed internal constructor(
        val message: String,
        val throwable: Throwable?,
        val reasons: List<RouterFailure>?,
        internal val preRouterReasons: List<PreRouterFailure>,
    ) : RerouteState() {

        @JvmOverloads constructor(
            message: String,
            throwable: Throwable? = null,
            reasons: List<RouterFailure>? = null,
        ) : this(message, throwable, reasons, emptyList())

        /**
         * Indicates if it makes sense to retry for this type of failures.
         * If false, it doesn't make sense to retry route request
         */
        val isRetryable get() = reasons.isRetryable || preRouterReasons.any { it.isRetryable }

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Failed

            if (message != other.message) return false
            if (throwable != other.throwable) return false
            if (preRouterReasons != other.preRouterReasons) return false
            return reasons == other.reasons
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (throwable?.hashCode() ?: 0)
            result = 31 * result + (reasons?.hashCode() ?: 0)
            result = 31 * result + preRouterReasons.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Failed(" +
                "message='$message', " +
                "throwable=$throwable, " +
                "reasons=$reasons, " +
                "preRouterReasons=$preRouterReasons" +
                ")"
        }
    }

    /**
     * Route fetching is in progress.
     */
    object FetchingRoute : RerouteState()

    /**
     * Route has been fetched.
     *
     * @param routerOrigin which router was used to fetch the route
     */
    class RouteFetched(@RouterOrigin val routerOrigin: String) : RerouteState() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RouteFetched

            return routerOrigin == other.routerOrigin
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return routerOrigin.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "RouteFetched(routerOrigin='$routerOrigin')"
        }
    }
}
