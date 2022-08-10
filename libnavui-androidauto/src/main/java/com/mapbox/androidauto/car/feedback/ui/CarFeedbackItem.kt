package com.mapbox.androidauto.car.feedback.ui

import androidx.annotation.Keep
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.result.SearchSuggestion

/**
 * This object is converted to json and sent the navigation history as a custom event.
 *
 * TODO add builder
 */
@Suppress("unused")
@SuppressWarnings("LongParameterList")
@Keep
class CarFeedbackItem(
    val carFeedbackTitle: String,
    val navigationFeedbackType: String? = null,
    val navigationFeedbackSubType: List<String>? = null,
    @com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
    val searchFeedbackReason: String? = null,
    val favoritesFeedbackReason: String? = null,
    val geoDeeplink: GeoDeeplink? = null,
    val geocodingResponse: GeocodingResponse? = null,
    val favoriteRecords: List<FavoriteRecord>? = null,
    val searchSuggestions: List<SearchSuggestion>? = null,
)
