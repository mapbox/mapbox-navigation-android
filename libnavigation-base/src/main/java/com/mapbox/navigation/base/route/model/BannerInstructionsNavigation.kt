package com.mapbox.navigation.base.route.model

/**
 * Visual instruction information related to a particular [LegStepNavigation] useful for making UI
 * elements inside your application such as banners. To receive this information, your request must
 * have {@link MapboxDirections#bannerInstructions()} set to true.
 *
 * @property distanceAlongGeometry Distance in meters from the beginning of the step at which the visual instruction should be
 * visible.
 * @since 1.0
 *
 * @property primary A plain text representation stored inside a [BannerTextNavigation] object.
 * @since 1.0
 *
 * @property secondary Ancillary visual information about the [LegStepNavigation].
 * @return [BannerTextNavigation] representing the secondary visual information
 * @since 1.0
 *
 * @property sub Additional information that is included if we feel the driver needs a heads up about something.
 * Can include information about the next maneuver (the one after the upcoming one),
 * if the step is short - can be null, or can be lane information.
 * If we have lane information, that trumps information about the next maneuver.
 * @return [BannerTextNavigation] representing the sub visual information
 * @since 1.0
 */
class BannerInstructionsNavigation(
    val distanceAlongGeometry: Double,
    val primary: BannerTextNavigation?,
    val secondary: BannerTextNavigation?,
    val sub: BannerTextNavigation?
)
