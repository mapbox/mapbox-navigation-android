package com.mapbox.navigation.mapgpt.core.common

sealed class SharedValue {
    class BooleanValue(val value: Boolean) : SharedValue()
    class LongValue(val value: Long) : SharedValue()
    class DoubleValue(val value: Double) : SharedValue()
    class StringValue(val value: String) : SharedValue()
}

fun SharedValue?.asBoolean(): Boolean? {
    return (this as? SharedValue.BooleanValue)?.value
}

fun SharedValue?.asLong(): Long? {
    return (this as? SharedValue.LongValue)?.value
}

fun SharedValue?.asDouble(): Double? {
    return (this as? SharedValue.DoubleValue)?.value
}

fun SharedValue?.asString(): String? {
    return (this as? SharedValue.StringValue)?.value
}
