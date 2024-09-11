package com.mapbox.navigation.ui.androidauto.feedback.core

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarGridFeedbackScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

abstract class CarFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val bitmapEncodeOptions = mapboxCarContext.options.carFeedbackOptions.bitmapEncodeOptions
        val mapSurface = mapboxCarContext.mapboxCarMap.carMapSurface?.mapSurface
        val encodedSnapshot = mapSurface?.snapshot()?.let { bitmap ->
            FeedbackHelper.encodeScreenshot(bitmap, bitmapEncodeOptions)
        }

        return object : CarGridFeedbackScreen(
            mapboxCarContext,
            getSourceName(),
            CarFeedbackSender(),
            getCarFeedbackPoll(mapboxCarContext.carContext),
            encodedSnapshot,
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
