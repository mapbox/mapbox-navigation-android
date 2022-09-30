package com.mapbox.navigation.dropin.actionbutton

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Custom Action Button description.
 *
 * @param view a view that will be added to the [NavigationView]
 * @param position determines if the custom button should be placed before or after existing controls.
 *                Defaults to Position.END.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ActionButtonDescription(
    val view: View,
    val position: Position = Position.END
) {
    /**
     * An enum that determines placement of the button.
     */
    enum class Position {
        /**
         * Place before existing controls.
         */
        START,

        /**
         * Place after existing controls.
         */
        END;
    }
}
