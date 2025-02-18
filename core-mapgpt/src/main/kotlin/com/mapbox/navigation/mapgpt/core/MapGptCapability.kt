package com.mapbox.navigation.mapgpt.core

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Represents a capability that the MapGPT SDK can provide.
 *
 * See TODO link to documentation for more information on capabilities.
 */
@Serializable
open class MapGptCapability(
    val capabilityId: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MapGptCapability

        return capabilityId == other.capabilityId
    }

    override fun hashCode(): Int {
        return capabilityId.hashCode()
    }
}

/**
 * Any implementation of this interface can be used to provide capabilities to the
 * [MapGptCapabilitiesProvider].
 */
interface MapGptCapabilities {
    /**
     * Returns the current set of capabilities for the service implementing the interface.
     */
    val capabilities: Flow<Set<MapGptCapability>>
}

/**
 * Provides a full set of capabilities definitions. This can be used to populate
 * the settings screen with capabilities so that they can be force enabled or disabled.
 * Note that, enabling a capability when it is unavailable will create a broken experience.
 * This is intended for testing purposes only.
 */
val MAP_GPT_ALL_CAPABILITY_SETTINGS = setOf(
    MapGptCapabilitySetting(AppleMusicCapability, "Apple Music"),
    MapGptCapabilitySetting(SpotifyMusicCapability, "Spotify Music"),
    MapGptCapabilitySetting(ClimateControlCapability, "Climate Control"),
    MapGptCapabilitySetting(AutopilotControlCapability, "Autopilot Control"),
    MapGptCapabilitySetting(WindowControlCapability, "Window Control"),
    MapGptCapabilitySetting(NavigateToFavoriteCapability("home"), "Navigate to Home"),
    MapGptCapabilitySetting(NavigateToFavoriteCapability("work"), "Navigate to Work"),
)

/**
 * Represents the capability for the app to control Apple Music following successful
 * authentication and integration.
 */
@Serializable
object AppleMusicCapability : MapGptCapability("applemusic")

/**
 * Represents the capability for the app to control Spotify Music
 */
@Serializable
object SpotifyMusicCapability : MapGptCapability("spotifymusic")

@Serializable
object AutopilotControlCapability : MapGptCapability("entities_v0.control_autopilot")

@Serializable
object WindowControlCapability : MapGptCapability("entities_v0.control_windows")

@Serializable
object ClimateControlCapability : MapGptCapability("entities_v0.control_climate")

@Serializable
class NavigateToFavoriteCapability(val key: String) : MapGptCapability("navigate_to_$key")
