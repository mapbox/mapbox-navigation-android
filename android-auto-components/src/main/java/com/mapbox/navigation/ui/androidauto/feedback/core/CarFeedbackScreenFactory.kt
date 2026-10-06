package com.mapbox.navigation.ui.androidauto.feedback.core

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarGridFeedbackScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

abstract class CarFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val feedbackOptions = mapboxCarContext.options.carFeedbackOptions
        // The screen requests the snapshot while it is constructed, before it is pushed and its
        // template replaces the map. Capture and encoding run off the main thread.
        val screenshot: (suspend () -> String?)? = if (feedbackOptions.attachScreenshot) {
            mapboxCarContext.mapboxCarMap.carMapSurface?.mapSurface?.let { mapSurface ->
                { mapSurface.captureEncodedScreenshot(feedbackOptions.bitmapEncodeOptions) }
            }
        } else {
            null
        }

        return object : CarGridFeedbackScreen(
            mapboxCarContext,
            getSourceName(),
            CarFeedbackSender(),
            getCarFeedbackPoll(mapboxCarContext.carContext),
            screenshot,
        ) {
            override fun onFinish() {
                this@CarFeedbackScreenFactory.onFinish()
            }
        }
    }

    abstract fun getSourceName(): String

    abstract fun getCarFeedbackPoll(carContext: CarContext): CarFeedbackPoll

    open fun onFinish() {
        mapboxCarContext.mapboxScreenManager.goBack()
    }
}
