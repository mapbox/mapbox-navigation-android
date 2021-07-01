package com.mapbox.navigation.base.route

import androidx.annotation.StringDef

/**
 * Route variants like consts.
 */
object RouteVariants {

    /**
     * Route(s) is fetched from **local tiles**.
     */
    const val ON_BOARD = "onBoard"

    /**
     * Route(s) is fetched from **Directions API**.
     */
    const val OFF_BOARD = "offBoard"

    /**
     * Router origin. Where route was fetched from: **Directions API** or **local tiles**
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        ON_BOARD,
        OFF_BOARD,
    )
    annotation class RouterOrigin
}
