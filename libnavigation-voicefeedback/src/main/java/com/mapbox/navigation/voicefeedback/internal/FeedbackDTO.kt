package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val FEEDBACK_ACTION_TYPE = "feedback"

@ExperimentalPreviewMapboxNavigationAPI
@Serializable
internal data class FeedbackDTO(
    @SerialName("feedbackType")
    val feedbackType: String,
    @SerialName("feedbackDescription")
    val feedbackDescription: String,
)
