package com.mapbox.androidauto.car.feedback.ui

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.mapbox.examples.androidauto.R
import com.mapbox.androidauto.car.action.MapboxActionProvider
import com.mapbox.androidauto.car.feedback.core.CarFeedbackItemProvider
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import kotlinx.coroutines.launch

@OptIn(MapboxExperimental::class)
class CarFeedbackAction(
    private val mapboxCarMap: MapboxCarMap,
    private val carFeedBackSender: CarFeedbackSender,
    private val carFeedbackItemProvider: CarFeedbackItemProvider
) : MapboxActionProvider.ScreenActionProvider {

    override fun getAction(screen: Screen): Action {
        return buildSnapshotAction(
            screen,
            carFeedBackSender,
            carFeedbackItemProvider.feedbackItems()
        )
    }

    private fun buildSnapshotAction(
        screen: Screen,
        feedbackSender: CarFeedbackSender,
        feedbackItems: List<CarFeedbackItem>
    ) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, R.drawable.mapbox_car_ic_feedback)
            ).build()
        )
        .setOnClickListener {
            screen.lifecycleScope.launch {
                val mapSurface = mapboxCarMap.carMapSurface?.mapSurface
                val encodedSnapshot = mapSurface?.snapshot()?.let { bitmap ->
                    FeedbackHelper.encodeScreenshot(bitmap, bitmapEncodeOptions)
                }
                screen.screenManager.push(
                    CarGridFeedbackScreen(
                        screen.carContext,
                        screen.javaClass.simpleName,
                        feedbackSender,
                        feedbackItems,
                        encodedSnapshot
                    ) { screen.screenManager.pop() }
                )
            }
        }
        .build()

    private companion object {
        private const val BITMAP_COMPRESS_QUALITY = 50
        private const val BITMAP_WIDTH = 800
        private val bitmapEncodeOptions = BitmapEncodeOptions.Builder()
            .compressQuality(BITMAP_COMPRESS_QUALITY)
            .width(BITMAP_WIDTH)
            .build()
    }
}
