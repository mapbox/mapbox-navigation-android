package com.mapbox.navigation.mapgpt.core

import kotlinx.serialization.Serializable

/**
 * Represents settings for managing MapGpt capabilities. Used in conjunction with
 * [MapGptServiceCapabilitiesRepository] to update capability configurations.
 *
 * @param capability Unique identifier for the capability this setting manages.
 * @param label A short human-readable label to identify the capability.
 */
@Serializable
class MapGptCapabilitySetting(
    val capability: MapGptCapability,
    val label: String,
)
