package com.mapbox.navigation.core.reroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
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
     * Reroute state.
     *
     * Note that there is also a [stateV2], considering using it if you need more granular notifications.
     */
    abstract val state: RerouteState

    /**
     * Add a RerouteStateObserver to collection and immediately invoke [rerouteStateObserver] with current
     * re-route state.
     *
     * Note that there is also a [registerRerouteStateV2Observer] method, considering using it if you need more granular notifications.
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
     * Reroute state with additional substates, see [RerouteStateV2].
     */
    @ExperimentalMapboxNavigationAPI
    abstract val stateV2: RerouteStateV2

    /**
     * Add a RerouteStateV2Observer to collection and immediately invoke [rerouteStateObserver] with current
     * re-route state.
     *
     * Note no need to call [registerRerouteStateObserver] if you use the V2 observer, as it covers all of the supported states there and more.
     *
     * @return `true` if the element has been added, `false` if the element is already present in the collection.
     */
    @ExperimentalMapboxNavigationAPI
    abstract fun registerRerouteStateV2Observer(
        rerouteStateObserver: RerouteStateV2Observer,
    ): Boolean

    /**
     * Remove [rerouteStateObserver] from collection of observers.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    @ExperimentalMapboxNavigationAPI
    abstract fun unregisterRerouteStateV2Observer(
        rerouteStateObserver: RerouteStateV2Observer,
    ): Boolean

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

    /**
     * [RerouteStateV2] observer
     */
    @ExperimentalMapboxNavigationAPI
    fun interface RerouteStateV2Observer {

        /**
         * Invoked whenever re-route state has changed.
         */
        fun onRerouteStateChanged(rerouteState: RerouteStateV2)
    }
}

/**
 * Reroute state.
 * Note that there is also [RerouteStateV2], considering using it if you need more granular notifications.
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

/**
 * Reroute state V2. Compared to [RerouteState], it has additional states: [Deviation.ApplyingRoute] and [Deviation.RouteIgnored].
 */
@ExperimentalMapboxNavigationAPI
abstract class RerouteStateV2 internal constructor() {

    /**
     * [RerouteController] is idle.
     */
    class Idle internal constructor() : RerouteStateV2() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    /**
     * Reroute has been interrupted.
     *
     * Might be triggered when:
     * - [MapboxNavigation.setNavigationRoutes] called;
     * - [MapboxNavigation.requestRoutes] called;
     * - another reroute call [RerouteController.reroute];
     * - when reroute has been disabled by [MapboxNavigation.setRerouteEnabled];
     * - from the SDK internally if another route request has been requested (only when using the default
     * implementation [MapboxRerouteController]).
     */
    class Interrupted internal constructor() : RerouteStateV2() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

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
        val throwable: Throwable? = null,
        val reasons: List<RouterFailure>? = null,
        internal val preRouterReasons: List<PreRouterFailure> = emptyList(),
    ) : RerouteStateV2() {

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
    class FetchingRoute internal constructor() : RerouteStateV2() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    /**
     * Route has been fetched.
     *
     * @param routerOrigin which router was used to fetch the route
     */
    class RouteFetched internal constructor(
        @RouterOrigin val routerOrigin: String,
    ) : RerouteStateV2() {

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

    /**
     * A group of [RerouteStateV2]s that are relevant for when reroute was requested internally due to deviation (off-route).
     */
    abstract class Deviation internal constructor() : RerouteStateV2() {

        /**
         * New route was fetched and is now being set to the navigator.
         * You'll receive this state after [RouteFetched].
         */
        class ApplyingRoute internal constructor() : Deviation() {

            /**
             * Indicates whether some other object is "equal to" this one.
             */
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            /**
             * Returns a hash code value for the object.
             */
            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }

        /**
         * New route was fetched but was ignored, because the user gor back on the original route.
         * You'll receive this state after [RouteFetched].
         */
        class RouteIgnored internal constructor() : Deviation() {

            /**
             * Indicates whether some other object is "equal to" this one.
             */
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            /**
             * Returns a hash code value for the object.
             */
            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }
    }
}
