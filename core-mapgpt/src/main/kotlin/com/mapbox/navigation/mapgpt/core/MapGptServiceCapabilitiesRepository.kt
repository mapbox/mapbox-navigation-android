package com.mapbox.navigation.mapgpt.core

import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.getString
import com.mapbox.navigation.mapgpt.core.common.setString
import com.mapbox.navigation.mapgpt.core.settings.BooleanSettingEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capability ids can come from a few sources. The SDK has built in capabilities like apple music
 * and spotify, which will be available through [MapGptCapabilitiesProvider]. However the
 * capabilities available for editing on the setting screen are defined by [settings]. This gives
 * the option to only show specific capabilities to the user.
 *
 * The [customSettings] are capabilities that the user has added to the settings screen. These
 * capabilities are not available in the SDK by default. These capabilities are not supported
 * by the SDK internally, so it may create a broken experience when added. MapGPT will think it
 * can control something it cannot. This is supporting testing and development purposes.
 */
class MapGptServiceCapabilitiesRepository(
    private val mapGptCore: MapGptCore,
    private val capabilityProvider: MapGptCapabilitiesProvider,
) {
    private val _settings = MutableStateFlow<Set<MapGptCapabilitySetting>>(emptySet())
    val settings: StateFlow<Set<MapGptCapabilitySetting>> = _settings

    private val _customSettings = MutableStateFlow<Set<MapGptCapabilitySetting>>(emptySet())
    val customSettings: StateFlow<Set<MapGptCapabilitySetting>> = _customSettings

    private val _editors = MutableStateFlow<Set<MapGptCapabilitySettingEditor>>(emptySet())
    val editors: StateFlow<Set<MapGptCapabilitySettingEditor>> = _editors

    init {
        mapGptCore.launchOnAttached {
            _customSettings.value = loadCustomCapabilitySettings()
            combine(
                settings,
                capabilityProvider.capabilities,
                customSettings,
            ) { settings, capabilities, customSettings ->
                val allCapabilities = linkedSetOf<MapGptCapability>().apply {
                    // First add the settings, so that the order is preserved from the
                    // input configuration. The capabilities are then used to show the
                    // state of the capability in the settings view.
                    addAll(settings.map { it.capability })
                    addAll(capabilities)
                    addAll(customSettings.map { it.capability })
                }
                allCapabilities.mapNotNull { capability ->
                    val setting = settings.find { it.capability == capability }
                    val customSetting = customSettings.find { it.capability == capability }
                    setting ?: customSetting ?: return@mapNotNull null
                    MapGptCapabilitySettingEditor(
                        capability = capability,
                        isEnabled = capabilities.contains(capability),
                        isCustom = customSetting != null,
                        label = setting?.label ?: customSetting?.label ?: capability.capabilityId,
                        settingEditor = mapGptCore.capabilityEnabledEditor(capability),
                    )
                }.toSet()
            }.collect { editors ->
                _editors.value = editors
            }
        }
    }

    fun editSettings(func: MutableSet<MapGptCapabilitySetting>.() -> Unit) {
        _settings.update { settings ->
            val newSettings = settings.toMutableSet()
            newSettings.func()
            newSettings.associateBy { it.capability }.values.toSet()
        }
    }

    fun editCustomSettings(func: MutableSet<MapGptCapabilitySetting>.() -> Unit) {
        _customSettings.update { customSettings ->
            val newCustomSettings = customSettings.toMutableSet()
            newCustomSettings.func()
            newCustomSettings.associateBy { it.capability }.values.toSet()
        }
        mapGptCore.execute {
            val value = Json.encodeToString(_customSettings.value)
            SharedLog.d(TAG) { "Saving custom capability settings: $value" }
            userSettings.setString(CUSTOM_CAPABILITY_SETTINGS_KEY, value)
        }
    }

    /**
     * These are the capabilities sent to MapGPT. It takes all capabilities and applies the
     * state of the settings to determine which capabilities are enabled. If now settings are
     * enabled, the supported state of the app is used.
     */
    fun capabilityIds(): Set<String> {
        val capabilityIds = capabilityProvider.capabilityIds().toMutableSet()
        editors.value.forEach { editor ->
            editor.settingEditor.get()?.let { enabled ->
                if (enabled) {
                    capabilityIds.add(editor.capability.capabilityId)
                } else {
                    capabilityIds.remove(editor.capability.capabilityId)
                }
            }
        }
        return capabilityIds.toSet()
    }

    private suspend fun MapGptCoreContext.loadCustomCapabilitySettings(): Set<MapGptCapabilitySetting> =
        withContext(Dispatchers.Default) {
            userSettings.getString(CUSTOM_CAPABILITY_SETTINGS_KEY)?.let { value ->
                try {
                    SharedLog.d(TAG) { "Loading custom capability settings: $value" }
                    Json.decodeFromString(value)
                } catch (e: Exception) {
                    SharedLog.e(TAG) { "Failed to decode custom capability settings: $value" }
                    userSettings.erase(CUSTOM_CAPABILITY_SETTINGS_KEY)
                    null
                }
            } ?: emptySet()
        }

    companion object {
        private const val TAG = "MapGptServiceCapabilitiesRepository"

        private const val CUSTOM_CAPABILITY_SETTINGS_KEY = "map_gpt_custom_capability_settings"
    }
}

fun MapGptCore.capabilityEnabledEditor(capability: MapGptCapability): BooleanSettingEditor {
    return BooleanSettingEditor(
        mapGptCore = this,
        key = capability.capabilityId.plus("_enabled"),
    )
}
