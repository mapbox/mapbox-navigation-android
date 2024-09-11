package com.mapbox.navigation.voice

import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreKey
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

class TestCarAppDataStoreOwner {

    private val booleanStorageMap =
        mutableMapOf<NavigationDataStoreKey<Boolean>, MutableStateFlow<Boolean>>()

    val carAppDataStoreOwner: NavigationDataStoreOwner = mockk {
        every { read(any<NavigationDataStoreKey<Boolean>>()) } answers {
            val key = firstArg<NavigationDataStoreKey<Boolean>>()
            booleanStorageMap.getOrPut(key) {
                MutableStateFlow(key.defaultValue)
            }
        }
        coEvery { write(any<NavigationDataStoreKey<Boolean>>(), any()) } answers {
            val key = firstArg<NavigationDataStoreKey<Boolean>>()
            val value = secondArg<Boolean>()
            val mutableStateFlow = booleanStorageMap.getOrPut(key) {
                MutableStateFlow(key.defaultValue)
            }
            mutableStateFlow.tryEmit(value)
        }
    }
}
