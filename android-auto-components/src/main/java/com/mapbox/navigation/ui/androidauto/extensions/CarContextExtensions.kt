@file:JvmName("CarContextExtensions")

package com.mapbox.navigation.ui.androidauto.extensions

import androidx.car.app.CarContext

/**
 * Returns whether the connected car app host supports unified navigation with
 * [androidx.car.app.navigation.model.MapWithContentTemplate].
 *
 * Unified navigation is available on hosts with Car App API level 7 or higher.
 */
fun CarContext.supportsUnifiedNavigation(): Boolean =
    carAppApiLevel >= 7
