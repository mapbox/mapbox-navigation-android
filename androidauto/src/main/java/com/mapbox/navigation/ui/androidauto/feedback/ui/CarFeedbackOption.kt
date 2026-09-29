package com.mapbox.navigation.ui.androidauto.feedback.ui

/**
 * Represents one of the predefined categories users can select when providing feedback
 *
 * @param searchFeedbackReason one of the Search SDK feedback reasons, attached to the
 * selection. It is not sent to Search SDK analytics; it is only recorded in the local history
 * recorder, where integrators can read it back.
 * @param favoritesFeedbackReason free-form reason attached to the selection. It is not sent to
 * any analytics service; it is only recorded in the local history recorder, where integrators
 * can read it back. None of the default polls set it.
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
