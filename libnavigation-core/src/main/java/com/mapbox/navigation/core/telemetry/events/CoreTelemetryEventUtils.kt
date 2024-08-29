package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value

internal fun String.toValue(): Value = Value.valueOf(this)
internal fun Boolean.toValue(): Value = Value.valueOf(this)
internal fun Int.toValue(): Value = Value.valueOf(this.toLong())
internal fun Double.toValue(): Value = Value.valueOf(this)

// FIXME: require to support Value.valueOf(Float). #CORESDK-1351
internal fun Float.toValue(): Value = Value.valueOf(this.toDouble())

internal fun TelemetryLocation.toValue(): Value {
    val fields = hashMapOf<String, Value>()
    fields["lat"] = latitude.toValue()
    fields["lng"] = longitude.toValue()
    speed?.let { fields["speed"] = it.toValue() }
    bearing?.let { fields["course"] = it.toValue() }
    altitude?.let { fields["altitude"] = it.toValue() }
    fields["timestamp"] = timestamp.toValue()
    fields["horizontalAccuracy"] = horizontalAccuracy.toValue()
    fields["verticalAccuracy"] = verticalAccuracy.toValue()
    return Value.valueOf(fields)
}

internal fun AppMetadata.toValue(): Value {
    val fields = hashMapOf<String, Value>()
    fields["name"] = name.toValue()
    fields["version"] = version.toValue()
    userId?.let { fields["userId"] = it.toValue() }
    sessionId?.let { fields["sessionId"] = it.toValue() }
    return Value.valueOf(fields)
}

internal fun NavigationStepData.toValue(): Value {
    val fields = hashMapOf<String, Value>()

    upcomingInstruction?.let { fields["upcomingInstruction"] = it.toValue() }
    upcomingModifier?.let { fields["upcomingModifier"] = it.toValue() }
    upcomingName?.let { fields["upcomingName"] = it.toValue() }
    upcomingType?.let { fields["upcomingType"] = it.toValue() }
    previousInstruction?.let { fields["previousInstruction"] = it.toValue() }
    previousModifier?.let { fields["previousModifier"] = it.toValue() }
    fields["previousName"] = previousName.toValue()
    previousType?.let { fields["previousType"] = it.toValue() }
    fields["distance"] = distance.toValue()
    fields["duration"] = duration.toValue()
    fields["distanceRemaining"] = distanceRemaining.toValue()
    fields["durationRemaining"] = durationRemaining.toValue()

    return Value.valueOf(fields)
}

internal fun <T> Array<T>.toValue(toValue: T.() -> Value): Value {
    val values = mutableListOf<Value>()
    for (item in this) {
        values.add(item.toValue())
    }
    return Value.valueOf(values)
}
