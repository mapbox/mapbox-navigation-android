package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * HistoryPoint.
 *
 * @property latitude coordinate of the search point
 * @property longitude coordinate of the search point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
data class HistoryPoint(
    val latitude: Double,
    val longitude: Double,
)
