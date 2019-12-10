package com.mapbox.navigation.base.route.model

/**
 *
 * @property distanceAlongGeometry This provides the missing piece in which is needed to announce
 * instructions at accuratetimes. If the user is less distance away from the maneuver than
 * what this `distanceAlongGeometry` than, the announcement should be called.
 * @since 1.0
 *
 * @property announcement Provides the instruction string which was build on the server-side and can sometimes
 * concatenate instructions together if maneuver instructions are too close to each other.
 * @since 1.0
 *
 * @property ssmlAnnouncement Get the same instruction string you'd get from [.announcement] but this one includes
 * Speech Synthesis Markup Language which helps voice synthesiser read information more humanely.
 * @since 1.0
 */
class VoiceInstructionsNavigation(
    val distanceAlongGeometry: Double?,
    val announcement: String?,
    val ssmlAnnouncement: String?
)
