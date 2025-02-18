package com.mapbox.navigation.mapgpt.core.common

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "SharedSettings"

fun PlatformSettings.getBoolean(key: String): Boolean? {
    val expected = get(key)
    return if (expected is SharedValue.BooleanValue) {
        expected.value
    } else {
        SharedLog.w(TAG) { "getBoolean: $key is $expected" }
        null
    }
}

fun PlatformSettings.getString(key: String): String? {
    val expected = get(key)
    return if (expected is SharedValue.StringValue) {
        expected.value
    } else {
        SharedLog.w(TAG) { "getString: $key is $expected" }
        null
    }
}

fun PlatformSettings.getLong(key: String): Long? {
    val expected = get(key)
    return if (expected is SharedValue.LongValue) {
        expected.value
    } else {
        SharedLog.w(TAG) { "getLong: $key is $expected" }
        null
    }
}

fun PlatformSettings.getDouble(key: String): Double? {
    val expected = get(key)
    return if (expected is SharedValue.DoubleValue) {
        expected.value
    } else {
        SharedLog.w(TAG) { "getDouble: $key is $expected" }
        null
    }
}

fun PlatformSettings.setBoolean(key: String, value: Boolean) {
    set(key, SharedValue.BooleanValue(value))
}

fun PlatformSettings.setString(key: String, value: String) {
    set(key, SharedValue.StringValue(value))
}

fun PlatformSettings.setLong(key: String, value: Long) {
    set(key, SharedValue.LongValue(value))
}

fun PlatformSettings.setDouble(key: String, value: Double) {
    set(key, SharedValue.DoubleValue(value))
}

fun PlatformSettings.observeBoolean(key: String, default: Boolean? = null): Flow<Boolean?> = callbackFlow {
    val observer = SharedSettingsObserver { observedKey, _, newValue ->
        val emitValue = (newValue as? SharedValue.BooleanValue)?.value ?: default
        if (key == observedKey) {
            trySend(emitValue)
        }
    }
    trySend(getBoolean(key) ?: default)
    registerObserver(key, observer)
    awaitClose {
        unregisterObserver(observer)
    }
}

fun PlatformSettings.observeString(key: String, default: String? = null): Flow<String?> = callbackFlow {
    val observer = SharedSettingsObserver { observedKey, _, newValue ->
        val emitValue = (newValue as? SharedValue.StringValue)?.value ?: default
        if (key == observedKey) {
            trySend(emitValue)
        }
    }
    trySend(getString(key) ?: default)
    registerObserver(key, observer)
    awaitClose { unregisterObserver(observer) }
}

fun PlatformSettings.observeLong(key: String, default: Long? = null): Flow<Long?> = callbackFlow {
    val observer = SharedSettingsObserver { observedKey, _, newValue ->
        val emitValue = (newValue as? SharedValue.LongValue)?.value ?: default
        if (key == observedKey) {
            trySend(emitValue)
        }
    }
    trySend(getLong(key) ?: default)
    registerObserver(key, observer)
    awaitClose { unregisterObserver(observer) }
}

fun PlatformSettings.observeDouble(key: String, default: Double? = null): Flow<Double?> = callbackFlow {
    val observer = SharedSettingsObserver { observedKey, _, newValue ->
        val emitValue = (newValue as? SharedValue.DoubleValue)?.value ?: default
        if (key == observedKey) {
            trySend(emitValue)
        }
    }
    trySend(getDouble(key) ?: default)
    registerObserver(key, observer)
    awaitClose { unregisterObserver(observer) }
}
