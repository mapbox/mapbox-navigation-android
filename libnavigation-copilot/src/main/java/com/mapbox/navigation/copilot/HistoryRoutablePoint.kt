package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * HistoryRoutablePoint.
 *
 * @property coordinates of the search point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
data class HistoryRoutablePoint(
    val coordinates: HistoryPoint,
)
