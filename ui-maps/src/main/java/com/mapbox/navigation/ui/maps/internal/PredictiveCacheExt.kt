package com.mapbox.navigation.ui.maps.internal

import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.ui.maps.PredictiveCacheController

/**
 * Create Maps cache controllers for a map instance for styles with options
 * Call when a new map instance is available.
 *
 * @param map an instance of [MapboxMap]
 * or not. Current map style is cached by default and shouldn't be added to the list of styles.
 * @param styles a list of Mapbox style URIs to cache.
 * If no styles are passed current map's style will be cached.
 * Only styles hosted on Mapbox Services are supported.
 * Only non-volatile styles will be cached.
 * @param predictiveCacheMapOptions list of map predictive cache options to apply only to styles
 * in this function call
 */
fun PredictiveCacheController.createStyleMapControllers(
    map: MapboxMap,
    styles: List<String>,
    predictiveCacheMapOptions: List<PredictiveCacheMapsOptions>,
) {
    this.createStyleMapControllers(map, styles, predictiveCacheMapOptions)
}
