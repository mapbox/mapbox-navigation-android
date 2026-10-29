package com.mapbox.navigation.ui.androidauto.settings

import androidx.car.app.Screen
import androidx.car.app.model.ListTemplate
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen

/**
 * Supplies the inline settings content of [MapboxScreen.NAVIGATION] on Car API 7 or newer.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface CarSettingsTemplateProvider {
    /**
     * Called whenever the navigation screen requests settings content.
     *
     * @param screen the owning screen. Call [Screen.invalidate] when settings content changes.
     * @param mode the current navigation mode, independent of legacy screen keys.
     * @return the settings list, with [androidx.car.app.model.Action.BACK] as its header action.
     * The navigation screen handles Back and retains ownership of map and navigation actions.
     */
    fun getSettingsTemplate(
        screen: Screen,
        mode: CarSettingsMode,
    ): ListTemplate
}
