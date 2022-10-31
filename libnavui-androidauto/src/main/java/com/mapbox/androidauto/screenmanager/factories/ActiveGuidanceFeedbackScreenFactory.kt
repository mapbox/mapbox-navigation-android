package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.feedback.core.CarFeedbackScreenFactory
import com.mapbox.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.androidauto.screenmanager.MapboxScreen

/**
 * Default screen for [MapboxScreen.ACTIVE_GUIDANCE_FEEDBACK].
 */
class ActiveGuidanceFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.ACTIVE_GUIDANCE

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.options.feedbackPollProvider
            .getActiveGuidanceFeedbackPoll(carContext)
    }
}
