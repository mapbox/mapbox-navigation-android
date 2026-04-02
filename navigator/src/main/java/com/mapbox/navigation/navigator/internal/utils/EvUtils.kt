package com.mapbox.navigation.navigator.internal.utils

import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.CurveElement
import com.mapbox.navigator.EvStateData

private const val EV_KEY_INITIAL_CHARGE = "ev_initial_charge"
private const val EV_KEY_ENERGY_CONSUMPTION_CURVE = "energy_consumption_curve"
private const val EV_KEY_AUX_CONSUMPTION = "auxiliary_consumption"
private const val EV_KEY_PRE_CONDITIONING_TIME = "ev_pre_conditioning_time"
private const val EV_KEY_UNCONDITIONED_CHARGING_CURVE = "ev_unconditioned_charging_curve"
private const val EV_FREEFLOW_CONSUMPTION_CURVE = "ev_freeflow_consumption_curve"

fun Map<String, String>.toEvStateData(): EvStateData {
    val additionalParameters = HashMap(this).apply {
        remove(EV_KEY_INITIAL_CHARGE)
        remove(EV_KEY_ENERGY_CONSUMPTION_CURVE)
        remove(EV_KEY_AUX_CONSUMPTION)
        remove(EV_KEY_PRE_CONDITIONING_TIME)
        remove(EV_KEY_UNCONDITIONED_CHARGING_CURVE)
        remove(EV_FREEFLOW_CONSUMPTION_CURVE)
    }

    return EvStateData(
        this[EV_KEY_INITIAL_CHARGE]?.toIntOrNull() ?: 0,
        this[EV_KEY_ENERGY_CONSUMPTION_CURVE]?.toCurveElements() ?: emptyList(),
        this[EV_FREEFLOW_CONSUMPTION_CURVE]?.toCurveElements(),
        null, // TODO: https://mapbox.atlassian.net/browse/NAVAND-7128
        this[EV_KEY_AUX_CONSUMPTION]?.toIntOrNull(),
        this[EV_KEY_PRE_CONDITIONING_TIME]?.toIntOrNull(),
        this[EV_KEY_UNCONDITIONED_CHARGING_CURVE]?.toCurveElements() ?: emptyList(),
        additionalParameters,
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
