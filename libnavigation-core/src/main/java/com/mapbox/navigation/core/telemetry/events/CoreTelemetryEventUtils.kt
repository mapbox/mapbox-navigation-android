package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value

internal fun String.toValue(): Value = Value.valueOf(this)
internal fun Boolean.toValue(): Value = Value.valueOf(this)
internal fun Int.toValue(): Value = Value.valueOf(this.toLong())
internal fun Double.toValue(): Value = Value.valueOf(this)
internal fun Float.toValue(): Value = Value.valueOf(this.toDouble())

internal fun TelemetryLocation.toValue(): Value {
    val fields = hashMapOf<String, Value>()
    fields["lat"] = latitude.toValue()
    fields["lng"] = longitude.toValue()
    fields["speed"] = speed.toValue()
    fields["course"] = bearing.toValue()
    fields["altitude"] = altitude.toValue()
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
