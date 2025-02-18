package com.mapbox.navigation.mapgpt.core

import com.mapbox.navigation.mapgpt.core.settings.BooleanSettingEditor

/**
 * Represents settings for a specific capability that can be toggled on or off. These settings
 * are intended primarily for developers or testers. Enabling a capability without the proper
 * system configuration may result in the client failing to handle corresponding events correctly.
 *
 * @param capability Unique identifier for the capability this setting manages.
 * @param isEnabled Indicates whether the system is configured to handle the capability.
 * @param label A brief, human-readable label to identify the capability.
 * @param settingEditor Provides the interface to forcefully enable or disable the capability.
 */
data class MapGptCapabilitySettingEditor(
    val capability: MapGptCapability,
    val isEnabled: Boolean,
    val isCustom: Boolean,
    val label: String,
    val settingEditor: BooleanSettingEditor,
)
