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
private const val EV_CURVE_BLENDING_RATIO = "ev_curve_blending_ratio"
private const val EV_ADDITIONAL_PARAMETERS = "ev_additional_parameters"
private const val EV_KEY_BATTERY_TEMPERATURE = "ev_battery_temperature"
private const val EV_KEY_BATTERY_CHARGING_POWER_AT_TEMPERATURE =
    "ev_battery_charging_power_at_temperature"
private const val EV_KEY_BATTERY_CHARGING_POWER_OPTIMAL_TEMPERATURE =
    "ev_battery_charging_power_optimal_temperature"
private const val EV_KEY_BATTERY_HEATING_PARAMETERS = "ev_battery_heating_parameters"

fun Map<String, String>.toEvStateData(): EvStateData {
    return EvStateData(
        this[EV_KEY_INITIAL_CHARGE]?.toIntOrNull() ?: 0,
        this[EV_KEY_ENERGY_CONSUMPTION_CURVE]?.toCurveElements() ?: emptyList(),
        this[EV_FREEFLOW_CONSUMPTION_CURVE]?.toCurveElements(),
        this[EV_CURVE_BLENDING_RATIO]?.toCurveElements(),
        this[EV_KEY_AUX_CONSUMPTION]?.toIntOrNull(),
        this[EV_KEY_PRE_CONDITIONING_TIME]?.toIntOrNull(),
        this[EV_KEY_UNCONDITIONED_CHARGING_CURVE]?.toCurveElements() ?: emptyList(),
        this[EV_ADDITIONAL_PARAMETERS]?.toAdditionalParameters(),
        this[EV_KEY_BATTERY_TEMPERATURE]?.toFloatOrLogged(),
        this[EV_KEY_BATTERY_CHARGING_POWER_AT_TEMPERATURE]?.toCurveElements(),
        this[EV_KEY_BATTERY_CHARGING_POWER_OPTIMAL_TEMPERATURE]?.toCurveElements(),
        this[EV_KEY_BATTERY_HEATING_PARAMETERS]?.toFloatList(),
    )
}

private fun String.toAdditionalParameters() = LinkedHashMap<String, String>().apply {
    split(",").forEach { entry ->
        if (!entry.contains(":")) return@forEach
        val key = entry.substringBefore(":")
        val value = entry.substringAfter(":", "")
        if (key.isNotEmpty()) {
            this[key] = value
        }
    }
}

private fun String.toCurveElements(): List<CurveElement> = parseEvData {
    split(";").map {
        val (first, second) = it.split(",").map { it.toFloat() }
        CurveElement(first, second)
    }
}

private fun String.toFloatList(): List<Float> = parseEvData {
    split(",").map { it.toFloat() }
}

private fun String.toFloatOrLogged(): Float? {
    val value = toFloatOrNull()
    if (value == null) {
        logE("EvUtils") {
            "Could not parse EV data: '$this' is not a valid float"
        }
    }
    return value
}

private fun <T> String.parseEvData(parse: String.() -> List<T>): List<T> {
    return try {
        parse()
    } catch (ex: Throwable) {
        logE("EvUtils") {
            "Could not parse EV data: ${ex.message}"
        }
        emptyList()
    }
}
