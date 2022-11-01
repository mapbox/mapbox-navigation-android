package com.mapbox.navigation.core.routerefresh

import com.google.gson.JsonElement

internal class EVDataHolder {

    private val evRefreshKeys = setOf(
        "energy_consumption_curve",
        "ev_initial_charge",
        "auxiliary_consumption",
        "ev_pre_conditioning_time",
    )
    private val currentData = mutableMapOf<String, String>()

    @Synchronized
    fun updateData(data: Map<String, String>) {
        currentData.putAll(data)
    }

    @Synchronized
    fun currentData(initialData: Map<String, JsonElement>?): Map<String, String> = mergeEvData(
        initialData,
        HashMap(currentData)
    )

    private fun mergeEvData(
        unrecognizedProperties: Map<String, JsonElement>?,
        latestUpdate: Map<String, String>
    ): Map<String, String> {
        val result = HashMap<String, String>(latestUpdate)
        val fallbackData = extractEvRefreshData(unrecognizedProperties)
        fallbackData.forEach { (key, value) ->
            if (key !in result.keys) {
                result[key] = value
            }
        }
        return result
    }

    private fun extractEvRefreshData(
        unrecognizedProperties: Map<String, JsonElement>?
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (unrecognizedProperties != null) {
            val engine = unrecognizedProperties["engine"]
                ?.asStringOrNull()
            if (engine == "electric") {
                evRefreshKeys.forEach { key ->
                    unrecognizedProperties[key]?.asStringOrNull()?.let { value ->
                        result[key] = value
                    }
                }
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
