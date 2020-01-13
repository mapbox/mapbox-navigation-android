package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.VoiceInstructionsNavigation

class VoiceInstructionsNavigationDto(
    val distanceAlongGeometry: Double?,
    val announcement: String?,
    val ssmlAnnouncement: String?
)

fun VoiceInstructionsNavigationDto.mapToModel() = VoiceInstructionsNavigation(
    distanceAlongGeometry = distanceAlongGeometry,
    announcement = announcement,
    ssmlAnnouncement = ssmlAnnouncement
)
