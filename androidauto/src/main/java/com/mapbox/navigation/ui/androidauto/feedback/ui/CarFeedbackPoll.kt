package com.mapbox.navigation.ui.androidauto.feedback.ui

/**
 * Represents a step in a feedback flow, where users select one of the predefined categories
 *
 * @param title human readable title describing the feedback items
 * @param options selectable feedback options
 */
data class CarFeedbackPoll(
    val title: String,
    val options: List<CarFeedbackOption>,
)
