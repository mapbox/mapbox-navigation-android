package com.mapbox.navigation.ui.androidauto.search

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.car.app.CarContext
import androidx.car.app.CarToast

/**
 * Shows a message about a failed place selection or route request on top of the current
 * template, so the places the driver chose from stay visible and can be tapped again.
 */
@UiThread
internal fun CarContext.showSearchToast(@StringRes messageRes: Int) {
    CarToast.makeText(this, getString(messageRes), CarToast.LENGTH_LONG).show()
}
