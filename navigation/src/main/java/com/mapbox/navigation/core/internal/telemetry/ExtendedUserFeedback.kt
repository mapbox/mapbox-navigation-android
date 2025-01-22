package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.UserFeedback

/**
 * Class that contains user feedback passed to [MapboxNavigation.postUserFeedback]
 * and additional properties: feedback id and location at that moment.
 *
 * @property feedback Feedback passed to [MapboxNavigation.postUserFeedback]
 * @property feedbackId this feedback's unique identifier
 * @property location user location when the feedback event was posted
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
data class ExtendedUserFeedback internal constructor(
    val feedback: UserFeedback,
    val feedbackId: String,
    val location: Point,
)
