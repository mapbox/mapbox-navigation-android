package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * HistorySearchResult.
 *
 * @property id unique identifier of the search point
 * @property name of the search point
 * @property address of the search point
 * @property coordinates of the search point, null in case of error
 * @property routablePoint [HistoryRoutablePoint]s details, null in case of error or if there is no related routable point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
data class HistorySearchResult(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: HistoryPoint?,
    val routablePoint: List<HistoryRoutablePoint>?,
)
