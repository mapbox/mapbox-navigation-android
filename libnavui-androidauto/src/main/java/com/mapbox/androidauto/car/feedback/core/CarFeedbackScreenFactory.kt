package com.mapbox.androidauto.car.feedback.core

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackPoll
import com.mapbox.androidauto.car.feedback.ui.CarGridFeedbackScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper

abstract class CarFeedbackScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val bitmapEncodeOptions = mapboxCarContext.carFeedbackOptions.bitmapEncodeOptions
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
