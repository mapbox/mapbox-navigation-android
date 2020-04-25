package com.mapbox.navigation.trip.notification.internal.maneuver

import androidx.annotation.StringDef

object ManeuverModifier {
    /**
     * Indicates "uturn" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val UTURN = "uturn"

    /**
     * Indicates "sharp right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val SHARP_RIGHT = "sharp right"

    /**
     * Indicates "right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val RIGHT = "right"

    /**
     * Indicates "slight right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val SLIGHT_RIGHT = "slight right"

    /**
     * Indicates "straight" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STRAIGHT = "straight"

    /**
     * Indicates "slight left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val SLIGHT_LEFT = "slight left"

    /**
     * Indicates "left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val LEFT = "left"

    /**
     * Indicates "sharp left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val SHARP_LEFT = "sharp left"

    /**
     * Representation of ManeuverModifier in form of logical types
     */
    @StringDef(
        UTURN,
        SHARP_RIGHT,
        RIGHT,
        SLIGHT_RIGHT,
        STRAIGHT,
        SLIGHT_LEFT,
        LEFT,
        SHARP_LEFT
    )
    annotation class Type
}
