@file:OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.route.MapMatchingMatch

@RestrictTo(RestrictTo.Scope.LIBRARY)
class MapMatchingMatchParsingSuccessfulResult internal constructor(
    val matches: List<MapMatchingMatch>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface MapMatchingMatchParser {
    suspend fun parseMapMatchedResponse(
        response: ResponseToParse,
    ): Result<MapMatchingMatchParsingSuccessfulResult>
}
