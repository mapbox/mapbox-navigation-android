package com.mapbox.navigation.ui.androidauto.feedback.ui

import androidx.annotation.Keep

/**
 * This object is converted to JSON and sent the navigation history as a custom event.
 *
 * TODO add builder
 */
@Suppress("unused")
@SuppressWarnings("LongParameterList")
@Keep
internal class CarFeedbackItem(
    val carFeedbackTitle: String,
    val navigationFeedbackType: String? = null,
    val navigationFeedbackSubType: List<String>? = null,
    @param:com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
    val searchFeedbackReason: String? = null,
    val favoritesFeedbackReason: String? = null,
)
