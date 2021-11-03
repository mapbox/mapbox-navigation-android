package com.mapbox.androidauto.testing

import com.mapbox.androidauto.datastore.CarAppDataStoreKey
import com.mapbox.androidauto.datastore.CarAppDataStoreOwner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

class TestCarAppDataStoreOwner {

    private val booleanStorageMap = mutableMapOf<CarAppDataStoreKey<Boolean>, MutableStateFlow<Boolean>>()

    val carAppDataStoreOwner: CarAppDataStoreOwner = mockk {
        every { read(any<CarAppDataStoreKey<Boolean>>()) } answers {
            val key = firstArg<CarAppDataStoreKey<Boolean>>()
            booleanStorageMap.getOrPut(key) {
                MutableStateFlow(key.defaultValue)
            }
        }
        coEvery { write(any<CarAppDataStoreKey<Boolean>>(), any()) } answers {
            val key = firstArg<CarAppDataStoreKey<Boolean>>()
            val value = secondArg<Boolean>()
            val mutableStateFlow = booleanStorageMap.getOrPut(key) {
                MutableStateFlow(key.defaultValue)
            }
            mutableStateFlow.tryEmit(value)
        }
    }
}
