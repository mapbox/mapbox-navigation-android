package com.mapbox.navigation.mapgpt.core.settings

import com.mapbox.navigation.mapgpt.core.MapGptCore
import com.mapbox.navigation.mapgpt.core.common.getBoolean
import com.mapbox.navigation.mapgpt.core.common.observeBoolean
import com.mapbox.navigation.mapgpt.core.common.setBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlin.properties.ReadOnlyProperty

fun MapGptCore.booleanSetting(key: String): ReadOnlyProperty<Any, BooleanSettingEditor> {
    return ReadOnlyProperty { _, _ -> BooleanSettingEditor(this@booleanSetting, key) }
}

class BooleanSettingEditor(
    private val mapGptCore: MapGptCore,
    private val key: String,
) {
    fun get(): Boolean? = mapGptCore.execute { userSettings.getBoolean(key) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<Boolean?> = mapGptCore.context().flatMapLatest { context ->
        context?.userSettings?.observeBoolean(key) ?: flow { emit(null) }
    }

    fun set(value: Boolean) = mapGptCore.execute { userSettings.setBoolean(key, value) }
    fun erase() = mapGptCore.execute { userSettings.erase(key) }
}
