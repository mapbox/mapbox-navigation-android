package com.mapbox.navigation.core.routerefresh

/**
 * Result of an immediate route refresh request.
 */
sealed class ImmediateRouteRefreshResult {
    /**
     * At least one route was refreshed successfully.
     */
    class Success internal constructor() : ImmediateRouteRefreshResult()

    /**
     * All route refresh attempts failed.
     */
    class Failure internal constructor() : ImmediateRouteRefreshResult()

    /**
     * Refresh was not started because there were no routes to refresh.
     */
    class NotStarted internal constructor() : ImmediateRouteRefreshResult()

    /**
     * Refresh was cancelled by a newer request.
     */
    class Cancelled internal constructor() : ImmediateRouteRefreshResult()
}
