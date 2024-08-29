package com.mapbox.navigation.ui.androidauto.feedback.ui

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * Add an action button that represents feedback, when pressed the screen will be changed to
 * [carFeedbackScreen]
 *
 * @param carFeedbackScreen [MapboxScreen.Key] used when the action is selected.
 */
class CarFeedbackAction(
    @MapboxScreen.Key
    private val carFeedbackScreen: String,
) {

    /**
     * Build the [Action].
     */
    fun getAction(screen: Screen): Action {
        return buildSnapshotAction(screen)
    }

    private fun buildSnapshotAction(screen: Screen) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, R.drawable.mapbox_car_ic_feedback),
            ).build(),
        )
        .setOnClickListener {
            MapboxScreenManager.push(carFeedbackScreen)
        }
        .build()
}
