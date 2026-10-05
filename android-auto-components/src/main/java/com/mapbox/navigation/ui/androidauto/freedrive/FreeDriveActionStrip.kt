package com.mapbox.navigation.ui.androidauto.freedrive

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackAction
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

class FreeDriveActionStrip(
    private val screen: Screen,
) {
    /**
     * Build an action strip for free drive
     */
    fun builder() = ActionStrip.Builder()
        .addAction(buildSettingsAction())
        .addAction(buildFeedbackAction())
        .addAction(buildSearchAction())
        .addAction(buildFavoritesAction())

    /**
     * Action to open settings screen.
     */
    fun buildSettingsAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_settings,
                ),
            ).build(),
        )
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.SETTINGS)
        }
        .build()

    /**
     * Action to provide feedback for the free drive state.
     */
    fun buildFeedbackAction(): Action = CarFeedbackAction(MapboxScreen.FREE_DRIVE_FEEDBACK)
        .getAction(screen)

    /**
     * Action to open search screen.
     */
    fun buildSearchAction(): Action = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_search_black36dp,
                ),
            ).build(),
        )
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.SEARCH)
        }
        .build()

    /**
     * Action to open favorite places screen.
     */
    fun buildFavoritesAction(): Action = Action.Builder()
        .setTitle(screen.carContext.resources.getString(R.string.car_action_search_favorites))
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.FAVORITES)
        }
        .build()
}
