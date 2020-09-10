package com.mapbox.navigation.ui.feedback

import androidx.annotation.StringRes
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent.Description

internal class FeedbackSubTypeItem(
    @field:Description @get:Description
    @param:Description val feedbackDescription: String,
    @field:StringRes @get:StringRes
    @param:StringRes val feedbackDescriptionResourceId: Int
) {
    var isChecked = false
}
