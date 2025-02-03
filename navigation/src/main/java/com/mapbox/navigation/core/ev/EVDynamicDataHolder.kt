package com.mapbox.navigation.core.ev

import com.google.gson.JsonElement

internal class EVDynamicDataHolder {

    private val currentData = mutableMapOf<String, String>()

    private val evRefreshKeys = setOf(
        "energy_consumption_curve",
        "ev_initial_charge",
        "auxiliary_consumption",
        "ev_pre_conditioning_time",
    )

    @Synchronized
    fun updateData(data: Map<String, String>) {
        currentData.putAll(data)
    }

    @Synchronized
    fun currentData(initialData: Map<String, JsonElement>): Map<String, String> = mergeEvData(
        initialData,
        HashMap(currentData),
    )

    private fun mergeEvData(
        initialData: Map<String, JsonElement>,
        latestUpdate: Map<String, String>,
    ): Map<String, String> {
        val result = HashMap(latestUpdate)
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
