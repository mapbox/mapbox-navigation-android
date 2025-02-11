package com.mapbox.navigation.navigator.internal.utils

import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.CurveElement
import com.mapbox.navigator.EvStateData

fun Map<String, String>.toEvStateData(): EvStateData {
    return EvStateData(
        this["ev_initial_charge"]?.toIntOrNull() ?: 0,
        this["energy_consumption_curve"]?.toCurveElements() ?: emptyList(),
        this["auxiliary_consumption"]?.toIntOrNull(),
        this["ev_pre_conditioning_time"]?.toIntOrNull(),
    )
}

private fun String.toCurveElements(): List<CurveElement> {
    return try {
        this.split(";").map {
            val (first, second) = it.split(",").map { it.toFloat() }
            CurveElement(first, second)
        }
    } catch (ex: Throwable) {
        logE("EvUtils") {
            "Could not parse EV data: ${ex.message}"
        }
        emptyList()
    }
}
