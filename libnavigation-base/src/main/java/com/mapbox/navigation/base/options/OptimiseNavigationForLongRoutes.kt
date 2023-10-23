package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI


@ExperimentalPreviewMapboxNavigationAPI
sealed class LongRoutesOptimisationOptions {

    /**
     * Changes behavior of navigation to be able to handle heavy routes avoiding OOM exception.
     * The options identify criteria when new behavior is applied.
     * @param responseToParseSizeBytes - minimum size of incoming response to apply optimisations
     */
    data class OptimiseNavigationForLongRoutes(
        val responseToParseSizeBytes: Int,
    ): LongRoutesOptimisationOptions()

    object NoOptimisations: LongRoutesOptimisationOptions()
}