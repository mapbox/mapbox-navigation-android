package com.mapbox.androidauto.car.feedback.ui

import androidx.car.app.model.CarIcon

/**
 * Represents one of the predefined categories users can select when providing feedback
 */
data class CarFeedbackOption(
    val title: String,
    val icon: CarIcon,
    val type: String? = null,
    val subType: List<String>? = null,
    @com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
    val searchFeedbackReason: String? = null,
    val favoritesFeedbackReason: String? = null,
    val nextPoll: CarFeedbackPoll? = null,
)
