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
        "ev_initial_charge",
        "auxiliary_consumption",
        "ev_pre_conditioning_time",
    )

    fun updateData(data: Map<String, String>) {
        currentData.update {
            it + data
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
            if (key !in result.keys) {
                result[key] = value
            }
        }
        return result
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
