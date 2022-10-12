package com.mapbox.androidauto.car.feedback.ui

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.action.MapboxActionProvider
import com.mapbox.androidauto.screenmanager.MapboxScreenManager

class CarFeedbackAction(
    private val carFeedbackScreen: String,
) : MapboxActionProvider.ScreenActionProvider {

    override fun getAction(screen: Screen): Action {
        return buildSnapshotAction(screen)
    }

    private fun buildSnapshotAction(screen: Screen) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, R.drawable.mapbox_car_ic_feedback)
            ).build()
        )
        .setOnClickListener {
            MapboxScreenManager.push(carFeedbackScreen)
        }
        .build()
}
