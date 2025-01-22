package com.mapbox.navigation.core.directions.session

import androidx.annotation.StringDef

/**
 * Extra data of a route
 */
object RoutesExtra {

    /**
     * Routes are **cleaned up**. Also, default reason, when routes haven't been set.
     * @see [RoutesObserver]
     */
    const val ROUTES_UPDATE_REASON_CLEAN_UP = "ROUTES_UPDATE_REASON_CLEAN_UP"

    /**
     * Routes update reason is **new routes**.
     * @see [RoutesObserver]
     */
    const val ROUTES_UPDATE_REASON_NEW = "ROUTES_UPDATE_REASON_NEW"

    /**
     * Routes update reason is **alternative routes**.
     * @see [RoutesObserver]
     */
    const val ROUTES_UPDATE_REASON_ALTERNATIVE =
        "ROUTES_UPDATE_REASON_ALTERNATIVE"

    /**
     * Routes update reason is **reroute**.
     * @see [RoutesObserver]
     */
    const val ROUTES_UPDATE_REASON_REROUTE = "ROUTES_UPDATE_REASON_REROUTE"

    /**
     * Routes update reason is **refresh routes**.
     * @see [RoutesObserver]
     */
    const val ROUTES_UPDATE_REASON_REFRESH = "ROUTES_UPDATE_REASON_REFRESH"

    /**
     * Reason of Routes update. See [RoutesObserver]
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        ROUTES_UPDATE_REASON_CLEAN_UP,
        ROUTES_UPDATE_REASON_NEW,
        ROUTES_UPDATE_REASON_ALTERNATIVE,
        ROUTES_UPDATE_REASON_REROUTE,
        ROUTES_UPDATE_REASON_REFRESH,
    )
    annotation class RoutesUpdateReason
}
