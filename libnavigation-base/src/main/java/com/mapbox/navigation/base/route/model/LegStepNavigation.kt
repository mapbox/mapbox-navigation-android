package com.mapbox.navigation.base.route.model

/**
 *
 * @property distance The distance traveled from the maneuver to the next [LegStepNavigation]. Unit is meters
 * @since 1.0
 *
 * @property duration The estimated travel time from the maneuver to the next [LegStepNavigation]. Unit is seconds
 * @since 1.0
 *
 * @property geometry Gives the geometry of the leg step. Encoded polyline string
 * @since 1.0
 *
 * @property name String with the name of the way along which the travel proceeds.
 * @since 1.0
 *
 * @property ref Any road designations associated with the road or path leading from this step&#39;s
 * maneuver to the next step&#39;s maneuver. Optionally included, if data is available.
 * If multiple road designations are associated with the road, they are separated by semicolons.
 * A road designation typically consists of an alphabetic network code (identifying the road type
 * or numbering system), a space or hyphen, and a route number. You should not assume that
 * the network code is globally unique: for example, a network code of &quot;NH&quot; may appear
 * on a &quot;National Highway&quot; or &quot;New Hampshire&quot;. Moreover, a route number may
 * not even uniquely identify a road within a given network.
 * String with reference number or code of the way along which the travel proceeds.
 * Optionally included, if data is available.
 * @since 1.0
 *
 * @property destinations String with the destinations of the way along which the travel proceeds.
 * Optionally included, if data is available
 * @since 1.0
 *
 * @property mode String indicating the mode of transportation.
 * @since 1.0
 *
 * @property pronunciation The pronunciation hint of the way name. Will be undefined if no pronunciation is hit.
 * @since 1.0
 *
 * @property rotaryName An optional string indicating the name of the rotary. This will only be a nonnull when the
 * maneuver type equals `rotary`.
 * @since 1.0
 *
 * @property rotaryPronunciation An optional string indicating the pronunciation of the name of the rotary. This will only be a
 * nonnull when the maneuver type equals `rotary`.
 * @since 1.0
 *
 * @property maneuver A [StepManeuverNavigation] object that typically represents the first coordinate making up the
 * [LegStepNavigation.geometry].
 * @since 1.0
 *
 * @property voiceInstructions The voice instructions object is useful for navigation sessions providing well spoken text
 * instructions along with the distance from the maneuver the instructions should be said.
 * @since 1.0
 *
 * @property bannerInstructions If in your request you set [MapboxDirections.bannerInstructions] to true, you'll
 * receive a list of [BannerInstructionsNavigation] which encompasses all information necessary for
 * creating a visual cue about a given [LegStepNavigation].
 * @since 1.0
 *
 * @property drivingSide The legal driving side at the location for this step. Result will either be `left` or `right`.
 * @since 1.0
 *
 * @property weight Specifies a decimal precision of edge weights, default value 1.
 * @since 1.0
 *
 * @property intersections Provides a list of all the intersections connected to the current way the
 * user is traveling along.
 * @since 1.0
 *
 * @property exits String with the exit numbers or names of the way. Optionally included, if data is available.
 * @since 1.0
 */
class LegStepNavigation(
    val distance: Double,
    val duration: Double,
    val geometry: String?,
    val name: String?,
    val ref: String?,
    val destinations: String?,
    val mode: String?,
    val pronunciation: String?,
    val rotaryName: String?,
    val rotaryPronunciation: String?,
    val maneuver: StepManeuverNavigation?,
    val voiceInstructions: List<VoiceInstructionsNavigation>?,
    val bannerInstructions: List<BannerInstructionsNavigation>?,
    val drivingSide: String?,
    val weight: Double,
    val intersections: List<StepIntersectionNavigation>?,
    val exits: String?
)
