package com.mapbox.androidauto.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * This class contains all the data store preferences.
 *
 * To add a new value, make sure your new preferenceKey does not collide with existing keys.
 * To change an existing value, consider implementing migrations so the preferences are not lost.
 * To test your code using the data store, see TestCarAppDataStoreOwner
 */
sealed class CarAppDataStoreKey<T>(
    val preferenceKey: Preferences.Key<T>,
    val defaultValue: T
)

object StoreAudioGuidanceMuted : CarAppDataStoreKey<Boolean>(
    preferenceKey = booleanPreferencesKey("audio_guidance_muted"),
    defaultValue = false
)
