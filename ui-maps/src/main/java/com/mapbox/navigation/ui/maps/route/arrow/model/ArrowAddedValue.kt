package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.FeatureCollection

/**
 * Represents the data for rendering the adding of route arrow(s)
 *
 * @param arrowShaftFeatureCollection a [FeatureCollection] for the arrow shafts source
 * @param arrowHeadFeatureCollection a [FeatureCollection] for the arrow head source
 */
class ArrowAddedValue internal constructor(
    val arrowShaftFeatureCollection: FeatureCollection,
    val arrowHeadFeatureCollection: FeatureCollection,
)
