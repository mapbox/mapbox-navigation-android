package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Controls if the Nav SDK behaves in regular or optimised way.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class LongRoutesOptimisationOptions {

    /**
     * The options identify criteria when an optimised behavior of Nav SDK is applied. The optimised
     * behaviour let the Nav SDK handle heavy routes without OOM exception. The optimised behavior
     * is different from regular in the following ways:
     *
     * 1. The Nav SDK drops alternatives routes before parsing a new response. Make sure that
     * your app doesn't keep references to dropped alternatives so that GC could release memory
     * which is needed for the new routes.
     *
     * 2. `NavigationRoute.directionsResponse.routes` always returns an empty list so that
     * dropped alternatives aren't kept in memory when they aren't needed.
     *
     * 3. Long routes are parsed in a queue internally. New parsing doesn't start until previous
     * is finished. You can get longer response time as a consequence.
     *
     * 4. [NavigationRouteAlternativesRequestCallback] returns an empty list of alternatives in case
     * of success. You will be able to receive new alternatives in [NavigationRouteAlternativesObserver].
     *
     * @param responseToParseSizeBytes - minimum size of incoming response to apply optimisations
     */
    data class OptimiseNavigationForLongRoutes(
        val responseToParseSizeBytes: Int,
    ) : LongRoutesOptimisationOptions()

    /**
     * Default navigation behavior.
     */
    object NoOptimisations : LongRoutesOptimisationOptions()
}
