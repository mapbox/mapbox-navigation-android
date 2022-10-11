package com.mapbox.navigation.dropin.actionbutton

import android.view.View
import com.mapbox.navigation.dropin.NavigationView

/**
 * Metadata describing the custom [view] and the [position] to which it should be attached in the
 * [ActionButtonBinder].
 *
 * @param view a view that will be added to the [NavigationView] [ActionButtonBinder]
 * @param position determines if the custom button should be placed before or after existing controls.
 *                Defaults to Position.END.
 */
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
