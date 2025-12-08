package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlexBuffers

internal object FlatbuffersListWrapper {

    fun <T> get(vector: FlexBuffers.Vector?, getter: (Int) -> T): List<T>? {
        return if (vector == null || vector.size() == 0) {
            null
        } else {
            get(vector.size(), getter)
        }
    }

    fun <T> get(listSize: Int, getter: (Int) -> T): List<T>? {
        if (listSize <= 0) return null
        return object : FlatbuffersNonCachingListWrapper<T>(listSize) {
            override fun get(index: Int): T {
                return getter(index)
            }
        }
    }

    private abstract class FlatbuffersNonCachingListWrapper<T>(
        override val size: Int,
    ) : AbstractList<T>()
}
