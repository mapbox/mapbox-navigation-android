package com.mapbox.androidauto.car

import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.placeslistonmap.PlaceMarkerRenderer
import com.mapbox.androidauto.car.placeslistonmap.PlacesListItemMapper
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.androidauto.car.search.FavoritesApi
import com.mapbox.androidauto.car.search.PlaceSearchScreen
import com.mapbox.androidauto.car.search.SearchCarContext
import com.mapbox.androidauto.car.settings.CarSettingsScreen
import com.mapbox.androidauto.car.settings.SettingsCarContext
import com.mapbox.search.MapboxSearchSdk

class MainActionStrip(
    private val screen: Screen,
    private val mainCarContext: MainCarContext
) {
    private val carContext = mainCarContext.carContext
    private val screenManager = carContext.getCarService(ScreenManager::class.java)

    /**
     * Build the action strip
     */
    fun builder() = ActionStrip.Builder()
        .addAction(buildSettingsAction())
        .addAction(buildFreeDriveFeedbackAction())
        .addAction(buildSearchAction())
        .addAction(buildFavoritesAction())

    /**
     * Build the settings action only
     */
    fun buildSettings() = ActionStrip.Builder()
        .addAction(buildSettingsAction())

    private fun buildFreeDriveFeedbackAction() =
        CarFeedbackAction(
            mainCarContext.mapboxCarMap,
            CarFeedbackSender(),
            mainCarContext.feedbackPollProvider
                .getFreeDriveFeedbackPoll(mainCarContext.carContext),
        ).getAction(screen)

    private fun buildSettingsAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext, R.drawable.ic_settings
                )
            ).build()
        )
        .setOnClickListener {
            val settingsCarContext = SettingsCarContext(mainCarContext)
            carContext
                .getCarService(ScreenManager::class.java)
                .push(CarSettingsScreen(settingsCarContext))
        }
        .build()

    private fun buildSearchAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    R.drawable.ic_search_black36dp
                )
            ).build()
        )
        .setOnClickListener {
            screenManager.push(
                PlaceSearchScreen(SearchCarContext(mainCarContext))
            )
        }
        .build()

    private fun buildFavoritesAction() = Action.Builder()
        .setTitle(carContext.resources.getString(R.string.car_action_search_favorites))
        .setOnClickListener { screenManager.push(favoritesScreen()) }
        .build()

    private fun favoritesScreen(): PlacesListOnMapScreen {
        val placesProvider = FavoritesApi(MapboxSearchSdk.serviceProvider.favoritesDataProvider())
        val feedbackPoll = mainCarContext.feedbackPollProvider
            .getSearchFeedbackPoll(mainCarContext.carContext)
        return PlacesListOnMapScreen(
            mainCarContext,
            placesProvider,
            PlacesListItemMapper(
                PlaceMarkerRenderer(mainCarContext.carContext),
                mainCarContext
                    .mapboxNavigation
                    .navigationOptions
                    .distanceFormatterOptions
                    .unitType
            ),
            listOf(
                CarFeedbackAction(
                    mainCarContext.mapboxCarMap, CarFeedbackSender(), feedbackPoll, placesProvider,
                ),
            )
        )
    }
}
