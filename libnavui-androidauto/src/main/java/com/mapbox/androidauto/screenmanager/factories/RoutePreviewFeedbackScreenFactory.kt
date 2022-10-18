package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.core.CarFeedbackScreenFactory
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackPoll
import com.mapbox.androidauto.screenmanager.MapboxScreen

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW_FEEDBACK].
 */
class RoutePreviewFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : CarFeedbackScreenFactory(mapboxCarContext) {
    override fun getSourceName(): String = MapboxScreen.ROUTE_PREVIEW

    override fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        return mapboxCarContext.options.feedbackPollProvider
            .getRoutePreviewFeedbackPoll(carContext)
    }
}
