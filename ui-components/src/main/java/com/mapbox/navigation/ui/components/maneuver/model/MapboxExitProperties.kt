package com.mapbox.navigation.ui.components.maneuver.model

import androidx.annotation.DrawableRes
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.ui.components.R

/**
 * A class that allows you to define various properties you wish to use to render for [ExitComponentNode]
 * of a given [PrimaryManeuver], [SecondaryManeuver] or [SubManeuver].
 * The priority of fallback is in the order:
 * - shouldFallbackWithDrawable
 * - shouldFallbackWithText
 * If both are set to false, no fallback will be applied and only the exit text associated with the
 * [Maneuver] will be rendered.
 *
 * @param shouldFallbackWithDrawable set to true if you want to use [fallbackDrawable] in case
 * if the [Maneuver] contains an [ExitComponentNode] with a [ManeuverModifier] value other than left
 * or right.
 * @param shouldFallbackWithText set to true if you don't want to use a [fallbackDrawable] but prepend
 * text "Exit" to the exit number in the [Maneuver]
 * @param exitBackground background to be set to the text view
 * @param fallbackDrawable drawable to be used in case [ManeuverModifier] has a different value other
 * than left or right
 * @param exitLeftDrawable drawable to be used when [ManeuverModifier] is left
 * @param exitRightDrawable drawable to be used when [ManeuverModifier] is right
 */
sealed class MapboxExitProperties(
    val shouldFallbackWithText: Boolean,
    val shouldFallbackWithDrawable: Boolean,
    @DrawableRes val exitBackground: Int,
    @DrawableRes val fallbackDrawable: Int,
    @DrawableRes val exitLeftDrawable: Int,
    @DrawableRes val exitRightDrawable: Int,
) {

    /**
     * An implementation of [MapboxExitProperties] that allows you to define various properties you
     * wish to use to render for [ExitComponentNode] of a given [PrimaryManeuver], [SecondaryManeuver]
     * or [SubManeuver]. The properties specified in this implementation will be used in countries
     * that follow MUTCD convention.
     * The priority of fallback is in the order:
     * - shouldFallbackWithDrawable
     * - shouldFallbackWithText
     * If both are set to false, no fallback will be applied and only the exit text associated with the
     * [Maneuver] will be rendered.
     *
     * @param shouldFallbackWithDrawable set to true if you want to use [fallbackDrawable] in case
     * if the [Maneuver] contains an [ExitComponentNode] with a [ManeuverModifier] value other than left
     * or right.
     * @param shouldFallbackWithText set to true if you don't want to use a [fallbackDrawable] but prepend
     * text "Exit" to the exit number in the [Maneuver]
     * @param exitBackground background to be set to the text view
     * @param fallbackDrawable drawable to be used in case [ManeuverModifier] has a different value other
     * than left or right
     * @param exitLeftDrawable drawable to be used when [ManeuverModifier] is left
     * @param exitRightDrawable drawable to be used when [ManeuverModifier] is right
     */
    class PropertiesMutcd(
        shouldFallbackWithText: Boolean = false,
        shouldFallbackWithDrawable: Boolean = true,
        @DrawableRes exitBackground: Int = R.drawable.mapbox_exit_board_background,
        @DrawableRes fallbackDrawable: Int = R.drawable.mapbox_ic_exit_arrow_right_mutcd,
        @DrawableRes exitLeftDrawable: Int = R.drawable.mapbox_ic_exit_arrow_left_mutcd,
        @DrawableRes exitRightDrawable: Int = R.drawable.mapbox_ic_exit_arrow_right_mutcd,
    ) : MapboxExitProperties(
        shouldFallbackWithText,
        shouldFallbackWithDrawable,
        exitBackground,
        fallbackDrawable,
        exitLeftDrawable,
        exitRightDrawable,
    )

    /**
     * An implementation of [MapboxExitProperties] that allows you to define various properties you
     * wish to use to render for [ExitComponentNode] of a given [PrimaryManeuver], [SecondaryManeuver]
     * or [SubManeuver]. The properties specified in this implementation will be used in countries
     * that follow VIENNA convention.
     * The priority of fallback is in the order:
     * - shouldFallbackWithDrawable
     * - shouldFallbackWithText
     * If both are set to false, no fallback will be applied and only the exit text associated with the
     * [Maneuver] will be rendered.
     *
     * @param shouldFallbackWithDrawable set to true if you want to use [fallbackDrawable] in case
     * if the [Maneuver] contains an [ExitComponentNode] with a [ManeuverModifier] value other than left
     * or right.
     * @param shouldFallbackWithText set to true if you don't want to use a [fallbackDrawable] but prepend
     * text "Exit" to the exit number in the [Maneuver]
     * @param exitBackground background to be set to the text view
     * @param fallbackDrawable drawable to be used in case [ManeuverModifier] has a different value other
     * than left or right
     * @param exitLeftDrawable drawable to be used when [ManeuverModifier] is left
     * @param exitRightDrawable drawable to be used when [ManeuverModifier] is right
     */
    class PropertiesVienna(
        shouldFallbackWithText: Boolean = false,
        shouldFallbackWithDrawable: Boolean = true,
        @DrawableRes exitBackground: Int = R.drawable.mapbox_exit_board_background,
        @DrawableRes fallbackDrawable: Int = R.drawable.mapbox_ic_exit_arrow_left_vienna,
        @DrawableRes exitLeftDrawable: Int = R.drawable.mapbox_ic_exit_arrow_left_vienna,
        @DrawableRes exitRightDrawable: Int = R.drawable.mapbox_ic_exit_arrow_right_vienna,
    ) : MapboxExitProperties(
        shouldFallbackWithText,
        shouldFallbackWithDrawable,
        exitBackground,
        fallbackDrawable,
        exitLeftDrawable,
        exitRightDrawable,
    )
}
