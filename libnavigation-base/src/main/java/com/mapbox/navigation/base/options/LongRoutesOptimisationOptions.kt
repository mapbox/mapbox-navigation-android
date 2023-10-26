package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Controls if the Nav SDK behaves in a regular or an optimised way.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class LongRoutesOptimisationOptions {

    /**
     * This option enables optimised behaviour of the Navigation SDK. The optimised
     * behaviour let the Nav SDK handle heavy routes without [OutOfMemoryError]. The optimised behavior
     * is different from regular in the following ways:
     *
     * 1. The Nav SDK drops alternatives routes before parsing a new response. The drop happens only
     * if the new routes response size is bigger than [responseToParseSizeBytes].
     * Make sure that your app doesn't keep references to dropped alternatives so that GC could
     * release memory which is needed for the new routes.
     *
     * 2. `NavigationRoute.directionsResponse.routes` always returns an empty list so that
     * dropped alternatives aren't kept in memory when they aren't needed. This optimisation is
     * applied for all [NavigationRoute] regardless of their size.
     *
     * 3. Long routes are parsed in a queue internally. New parsing doesn't start until previous
     * is finished. You can get longer response time as a consequence. Parsing happens in queue only
     * if response size is bigger than [responseToParseSizeBytes].
     *
     * 4. [NavigationRouteAlternativesRequestCallback] returns an empty list of alternatives in case
     * of success. You will be able to receive new alternatives in [NavigationRouteAlternativesObserver].
     * This optimisation is applied regardless of alternative size.
     *
     * @param responseToParseSizeBytes - minimum size of incoming response to drop current
     * alternative routes and parse responses in queue.
     */
    data class OptimiseNavigationForLongRoutes(
        val responseToParseSizeBytes: Int,
    ) : LongRoutesOptimisationOptions()

    /**
     * This option keeps default behavior.
     * Use this option if your app doesn't fail with [OutOfMemoryError] because of heavy routes
     * models.
     */
    object NoOptimisations : LongRoutesOptimisationOptions()
}
