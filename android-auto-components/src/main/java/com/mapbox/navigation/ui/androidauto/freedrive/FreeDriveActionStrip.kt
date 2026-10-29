package com.mapbox.navigation.ui.androidauto.freedrive

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.action.buildSettingsAction
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackAction
import com.mapbox.navigation.ui.androidauto.navigation.MapboxNavigationScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

class FreeDriveActionStrip(
    private val screen: Screen,
) {
    /**
     * Build an action strip for free drive
     */
    fun builder(): ActionStrip.Builder = ActionStrip.Builder()
        .apply {
            if (screen !is MapboxNavigationScreen || !screen.isShowingSettings) {
                addAction(buildSettingsAction())
            }
        }
        .addAction(buildFeedbackAction())
        .addAction(buildSearchAction())
        .addAction(buildFavoritesAction())

    /**
     * Action to show settings inline on the unified navigation screen, or open the settings
     * screen on legacy screens.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun buildSettingsAction(): Action = buildSettingsAction(screen)

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
                    R.drawable.ic_search_24px,
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
