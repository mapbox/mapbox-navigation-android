package com.mapbox.androidauto.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.coroutineScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation for preferences that exist beyond app and car sessions.
 */
class CarAppDataStoreOwner internal constructor() {

    private val Context.dataStore by preferencesDataStore("car_app_mapbox_preferences")
    private lateinit var carAppDataStore: DataStore<Preferences>

    fun setup(applicationContext: Context) {
        carAppDataStore = applicationContext.dataStore
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun launch(block: suspend CarAppDataStoreOwner.() -> Unit): Job =
        MapboxNavigationApp.lifecycleOwner.lifecycle.coroutineScope.launchWhenStarted {
            block()
        }

    fun <T> read(key: CarAppDataStoreKey<T>): Flow<T> {
        return carAppDataStore.data.map { preferences ->
            preferences[key.preferenceKey] ?: key.defaultValue
        }
    }

    suspend fun <T> write(key: CarAppDataStoreKey<T>, value: T?) {
        carAppDataStore.edit { preferences ->
            preferences[key.preferenceKey] = value ?: key.defaultValue
        }
    }
}
