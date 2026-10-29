package com.mapbox.navigation.ui.androidauto.action

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.navigation.MapboxNavigationScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * Builds a settings action for any navigation mode.
 *
 * Shows settings inline on [MapboxScreen.NAVIGATION], or opens the dedicated settings screen
 * on legacy screens. The title is subject to the host's action-strip text limits.
 *
 * @param screen the screen that owns the action.
 * @param icon the settings action icon, defaulting to the built-in settings icon.
 * @param title the settings action title, or null for an icon-only action.
 */
@ExperimentalPreviewMapboxNavigationAPI
@JvmOverloads
@Suppress("DEPRECATION")
fun buildSettingsAction(
    screen: Screen,
    icon: CarIcon = defaultSettingsIcon(screen),
    title: CharSequence? = null,
): Action = Action.Builder()
    .setIcon(icon)
    .apply { title?.let(::setTitle) }
    .setOnClickListener {
        if (screen is MapboxNavigationScreen) {
            screen.showSettings()
        } else {
            MapboxScreenManager.push(MapboxScreen.SETTINGS)
        }
    }
    .build()

private fun defaultSettingsIcon(screen: Screen): CarIcon = CarIcon.Builder(
    IconCompat.createWithResource(
        screen.carContext,
        R.drawable.ic_settings_24px,
    ),
).build()
