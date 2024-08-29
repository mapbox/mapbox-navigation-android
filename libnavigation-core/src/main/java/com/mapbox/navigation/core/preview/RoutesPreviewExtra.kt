package com.mapbox.navigation.core.preview

import androidx.annotation.StringDef

/**
 * All reasons which can cause routes preview update.
 * @see [RoutesPreviewObserver]
 */
object RoutesPreviewExtra {
    /**
     * Routes for preview were set by user.
     * [RoutesPreviewUpdate.routesPreview] always has value for this reason.
     * @see [RoutesPreviewObserver]
     */
    const val PREVIEW_NEW = "PREVIEW_NEW"

    /***
     * Routes preview were cleanup by user.
     * [RoutesPreviewUpdate.routesPreview] is always null for this reason.
     * @see [RoutesPreviewObserver]
     */
    const val PREVIEW_CLEAN_UP = "PREVIEW_CLEAN_UP"

    /**
     * Reason of Routes Preview update.
     * See [RoutesPreviewObserver]
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        PREVIEW_NEW,
        PREVIEW_CLEAN_UP,
    )
    annotation class RoutePreviewUpdateReason
}
