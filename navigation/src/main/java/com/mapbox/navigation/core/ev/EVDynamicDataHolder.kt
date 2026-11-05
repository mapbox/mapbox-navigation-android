@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.ev

import com.google.gson.JsonElement
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class EVDynamicDataHolder {

    private val currentData = MutableStateFlow(mapOf<String, String>())

    private val evRefreshKeys = setOf(
        "energy_consumption_curve",
        "ev_freeflow_consumption_curve",
        "ev_initial_charge",
        "auxiliary_consumption",
        "ev_pre_conditioning_time",
        "ev_unconditioned_charging_curve",
        "ev_battery_temperature",
    )

    // The battery preconditioning model and the legacy preconditioning pair are mutually exclusive:
    // Directions rejects a URI that carries parameters from both. They describe the same state in
    // two incompatible ways, so only the group the latest update carries survives the merge below.
    private val legacyPreconditioningKeys = setOf(
        "ev_pre_conditioning_time",
        "ev_unconditioned_charging_curve",
    )

    private val batteryPreconditioningKeys = setOf(
        "ev_battery_charging_power_at_temperature",
        "ev_battery_charging_power_optimal_temperature",
        "ev_battery_heating_parameters",
        "ev_battery_temperature",
    )

    fun updateData(data: Map<String, String>) {
        currentData.update { current ->
            (current + data).withoutSupersededPreconditioning(data.keys)
        }
    }

    fun currentData(initialData: Map<String, JsonElement>): Map<String, String> = mergeEvData(
        initialData,
        HashMap(currentData.value),
    )

    /**
     * Unlike [currentData], this method doesn't filter out internal nav sdk keys which are not
     * supposed to be passed to Directions API.
     */
    fun updatedRawData(): StateFlow<Map<String, String>> = currentData

    private fun mergeEvData(
        initialData: Map<String, JsonElement>,
        latestUpdate: Map<String, String>,
    ): Map<String, String> {
        val result = HashMap(latestUpdate)
        result.remove(EV_EFFICIENCY_KEY)
        val fallbackData = extractEvRefreshData(initialData)
        fallbackData.entries.forEach { (key, value) ->
            if (key !in result.keys && !key.conflictsWithPreconditioningIn(result.keys)) {
                result[key] = value
            }
        }
        return result
    }

    /**
     * [legacyPreconditioningKeys] and [batteryPreconditioningKeys] are two mutually exclusive ways
     * to describe the same preconditioning state, and Directions rejects a URI that carries both.
     * This drops the group not represented in [updatedKeys] so the merged data never holds both:
     * - an update bringing the battery model keeps it and drops the legacy pair;
     * - an update bringing only the legacy pair keeps it and drops the battery model;
     * - an update bringing both (contradictory input) keeps the battery model, as the newer
     *   representation, and drops the legacy pair;
     * - an update bringing neither leaves the data unchanged.
     */
    private fun Map<String, String>.withoutSupersededPreconditioning(
        updatedKeys: Set<String>,
    ): Map<String, String> {
        val updateBringsBattery = updatedKeys.any { it in batteryPreconditioningKeys }
        val updateBringsLegacy = updatedKeys.any { it in legacyPreconditioningKeys }
        return when {
            updateBringsBattery -> this - legacyPreconditioningKeys
            updateBringsLegacy -> this - batteryPreconditioningKeys
            else -> this
        }
    }

    private fun String.conflictsWithPreconditioningIn(presentKeys: Set<String>): Boolean =
        when (this) {
            in legacyPreconditioningKeys -> presentKeys.any { it in batteryPreconditioningKeys }
            in batteryPreconditioningKeys -> presentKeys.any { it in legacyPreconditioningKeys }
            else -> false
        }

    private fun extractEvRefreshData(
        unrecognizedProperties: Map<String, JsonElement>,
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        evRefreshKeys.forEach { key ->
            unrecognizedProperties[key]?.asStringOrNull()?.let { value ->
                result[key] = value
            }
        }
        return result
    }

    private fun JsonElement.asStringOrNull(): String? = try {
        asString
    } catch (ex: Throwable) {
        null
    }
}
