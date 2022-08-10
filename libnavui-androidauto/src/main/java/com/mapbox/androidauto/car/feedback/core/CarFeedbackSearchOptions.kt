package com.mapbox.androidauto.car.feedback.core

import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.result.SearchSuggestion

data class CarFeedbackSearchOptions(
    val favoriteRecords: List<FavoriteRecord>? = null,
    val searchSuggestions: List<SearchSuggestion>? = null,
    val geoDeeplink: GeoDeeplink? = null,
    val geocodingResponse: GeocodingResponse? = null,
)
