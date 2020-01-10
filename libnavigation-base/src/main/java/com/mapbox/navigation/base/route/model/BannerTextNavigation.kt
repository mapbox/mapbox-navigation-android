package com.mapbox.navigation.base.route.model

/**
 * Includes both plain text information that can be visualized inside your navigation application
 * along with the text string broken down into {@link BannerComponents} which may or may not
 * include a image url. To receive this information, your request must have
 * {@link MapboxDirections#bannerInstructions()} set to true.
 *
 * @property text Plain text with all the [BannerComponentsNavigation] text combined.
 * @since 1.0
 *
 * @property components A part or element of the [BannerInstructionsNavigation].
 * Return [BannerComponentsNavigation] specific to a [LegStepNavigation]
 * @since 1.0
 *
 * @property type This indicates the type of maneuver.
 * @see StepManeuverNavigation.StepManeuverTypeNavigation
 * @since 1.0
 *
 * @property modifier This indicates the mode of the maneuver. If type is of turn, the modifier indicates the
 * change in direction accomplished through the turn. If the type is of depart/arrive, the
 * modifier indicates the position of waypoint from the current direction of travel.
 * @since 1.0
 *
 * @property degrees The degrees at which you will be exiting a roundabout, assuming `180` indicates
 * going straight through the roundabout.
 * @since 1.0
 *
 * @property drivingSide A string representing which side the of the street people drive on
 * in that location. Can be 'left' or 'right'.
 * @since 1.0
 */
class BannerTextNavigation(
    val text: String?,
    val components: List<BannerComponentsNavigation>?,
    @StepManeuverType
    val type: String?,
    val modifier: String?,
    val degrees: Double?,
    val drivingSide: String?
)
