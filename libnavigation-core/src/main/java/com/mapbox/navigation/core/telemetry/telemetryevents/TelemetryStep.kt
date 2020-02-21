package com.mapbox.navigation.core.telemetry.telemetryevents
/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional

data class TelemetryStep(
    val upcomingInstruction: String,
    val upcomingType: String,
    val upcomingModifier: String,
    val upcomingName: String,
    val previousInstruction: String,
    val previousType: String,
    val previousModifier: String,
    val previousName: String,
    val distance: Int,
    val duration: Int,
    val distanceRemaining: Int,
    val durationRemaining: Int
)
