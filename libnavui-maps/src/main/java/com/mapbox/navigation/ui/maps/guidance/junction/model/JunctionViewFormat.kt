package com.mapbox.navigation.ui.maps.guidance.junction.model

import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Annotation class representing the format options for a junction view.
 * Available values are:
 * - [JunctionViewFormat.PNG]
 * - [JunctionViewFormat.SVG]
 */
@ExperimentalPreviewMapboxNavigationAPI
@Retention(AnnotationRetention.BINARY)
@StringDef(
    JunctionViewFormat.PNG,
    JunctionViewFormat.SVG,
)
annotation class JunctionViewFormat {
    companion object {

        /**
         * Specifies the PNG format for the junction view.
         */
        const val PNG = "png"

        /**
         * Specifies the SVG format for the junction view.
         */
        const val SVG = "svg"
    }
}
