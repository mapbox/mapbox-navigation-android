package com.mapbox.navigation.ui.androidauto.feedback.ui

/**
 * Represents one of the predefined categories users can select when providing feedback
 */
data class CarFeedbackOption(
    val title: String,
    val icon: CarFeedbackIcon,
    val type: String? = null,
    val subType: List<String>? = null,
    val searchFeedbackReason: String? = null,
    val favoritesFeedbackReason: String? = null,
    val nextPoll: CarFeedbackPoll? = null,
)
