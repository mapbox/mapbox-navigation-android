package com.mapbox.navigation.ui.utils.internal.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation for preferences that exist beyond app and car sessions.
 */
class NavigationDataStoreOwner(context: Context) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    fun <T> read(key: NavigationDataStoreKey<T>): Flow<T> {
        return dataStore.data.map { preferences ->
            preferences[key.preferenceKey] ?: key.defaultValue
        }
    }

    suspend fun <T> write(key: NavigationDataStoreKey<T>, value: T?) {
        dataStore.edit { preferences ->
            preferences[key.preferenceKey] = value ?: key.defaultValue
        }
    }

    private companion object {
        private const val NAVIGATION_DATA_STORE_NAME = "mapbox_navigation_preferences"
        private val Context.dataStore by preferencesDataStore(name = NAVIGATION_DATA_STORE_NAME)
    }
}
