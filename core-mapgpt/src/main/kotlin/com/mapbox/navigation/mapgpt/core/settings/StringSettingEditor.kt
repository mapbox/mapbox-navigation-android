package com.mapbox.navigation.mapgpt.core.settings

import com.mapbox.navigation.mapgpt.core.MapGptCore
import com.mapbox.navigation.mapgpt.core.common.getString
import com.mapbox.navigation.mapgpt.core.common.observeString
import com.mapbox.navigation.mapgpt.core.common.setString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlin.properties.ReadOnlyProperty

fun MapGptCore.stringSetting(key: String): ReadOnlyProperty<Any, StringSettingEditor> {
    return ReadOnlyProperty { _, _ -> StringSettingEditor(this@stringSetting, key) }
}

class StringSettingEditor(
    private val mapGptCore: MapGptCore,
    private val key: String,
) {
    fun get(): String? = mapGptCore.execute { userSettings.getString(key) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<String?> = mapGptCore.context().flatMapLatest { context ->
        context?.userSettings?.observeString(key) ?: flow { emit(null) }
    }

    fun set(value: String) = mapGptCore.execute {
        userSettings.setString(key, value)
    }
    fun erase() = mapGptCore.execute { userSettings.erase(key) }
}
