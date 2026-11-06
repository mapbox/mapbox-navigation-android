package com.mapbox.navigation.ui.androidauto.action

import androidx.car.app.Screen
import androidx.car.app.model.Action
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.navigation.MapboxNavigationScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * Shows default favorites inline on [MapboxScreen.NAVIGATION]. Opens the dedicated favorites screen
 * on legacy screens or when an app registers a custom favorites screen factory.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Suppress("DEPRECATION")
fun buildFavoritesAction(screen: Screen): Action = Action.Builder()
    .setTitle(screen.carContext.getString(R.string.car_action_search_favorites))
    .setOnClickListener {
        if (screen is MapboxNavigationScreen) {
            screen.showFavorites()
        } else {
            MapboxScreenManager.push(MapboxScreen.FAVORITES)
        }
    }
    .build()
