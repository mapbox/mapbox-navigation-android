package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.google.gson.Gson

internal object ElectronicHorizonRequestBuilder {
    private val gson = Gson()

    fun build(
        expansion: Expansion,
        locations: List<Location>
    ): String {
        val positions = locations.map {
            Position(
                it.latitude,
                it.longitude
            )
        }
        val options = mapOf("expansion" to expansion.value)
        val request = ElectronicHorizonRequest(positions, options)

        return gson.toJson(request)
    }

    internal enum class Expansion(val value: String) {
        _1D("1D"),
        _1_5D("1.5D"),
        _2D("2D")
    }

    private data class Position(
        val lat: Double,
        val lon: Double
    )

    private data class ElectronicHorizonRequest(
        val shape: List<Position>,
        val eh_options: Map<String, Any>
    )
}
