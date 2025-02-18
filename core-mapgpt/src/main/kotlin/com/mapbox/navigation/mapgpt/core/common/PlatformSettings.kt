package com.mapbox.navigation.mapgpt.core.common

import com.mapbox.bindgen.Value
import com.mapbox.common.OnValueChanged
import com.mapbox.common.SettingsService

class PlatformSettings(
    private val settingsService: SettingsService,
) {
    private var observers: MutableMap<SharedSettingsObserver, Int> = mutableMapOf()

    fun has(key: String): Boolean {
        val expected = settingsService.has(key)
        expected.onError { reason ->
            SharedLog.w(TAG) { "Failed to has: $key, reason: $reason" }
        }
        return expected.value ?: run {
            SharedLog.w(TAG) { "Failed to has: $key, value: ${expected.value}" }
            false
        }
    }

    fun get(key: String): SharedValue? {
        val expected = settingsService.get(key)
        expected.onError { reason ->
            SharedLog.w(TAG) { "Failed to get: $key, reason: $reason" }
        }
        return expected.value?.toSharedValue()
    }

    fun set(
        key: String,
        value: SharedValue,
    ) {
        val expected = settingsService.set(key, value.toPlatformValue())
        expected.onError { reason ->
            SharedLog.w(TAG) { "Failed to set: $key, reason: $reason" }
        }
    }

    fun erase(key: String) {
        val expected = settingsService.erase(key)
        expected.onError { reason ->
            SharedLog.w(TAG) { "Failed to erase: $key, reason: $reason" }
        }
    }

    fun registerObserver(
        key: String,
        observer: SharedSettingsObserver,
    ) {
        val valueChanged = OnValueChanged { _, oldValue, newValue ->
            observer.onSettingsChanged(
                key = key,
                oldValue = oldValue?.toSharedValue(),
                newValue = newValue?.toSharedValue(),
            )
        }
        observers[observer] = settingsService.registerObserverAtSettingsThread(key, valueChanged)
    }

    fun unregisterObserver(observer: SharedSettingsObserver) {
        observers.remove(observer)?.let { id ->
            settingsService.unregisterObserver(id)
        }
    }

    private fun Value.toSharedValue(): SharedValue {
        return when (val contents = this.contents) {
            is Boolean -> SharedValue.BooleanValue(contents)
            is Double -> SharedValue.DoubleValue(contents)
            is Long -> SharedValue.LongValue(contents)
            is String -> SharedValue.StringValue(contents)
            else -> throw IllegalArgumentException("Unsupported value type: $contents")
        }
    }

    private fun SharedValue.toPlatformValue(): Value {
        return when (this) {
            is SharedValue.BooleanValue -> Value.valueOf(this.value)
            is SharedValue.DoubleValue -> Value.valueOf(this.value)
            is SharedValue.LongValue -> Value.valueOf(this.value)
            is SharedValue.StringValue -> Value.valueOf(this.value)
        }
    }

    private companion object {
        private const val TAG = "PlatformSettings"
    }
}
