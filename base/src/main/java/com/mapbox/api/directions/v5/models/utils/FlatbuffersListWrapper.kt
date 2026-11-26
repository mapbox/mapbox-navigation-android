package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlexBuffers

// Primitive index: a Kotlin (Int) -> T lambda boxes indexes above the Integer cache on each call.
// The result T is still boxed for primitives. That is accepted: the getter only backs generic
// List<T> instances, which box their elements anyway. Don't use it for code that reads primitives
// without exposing them as a generic collection, read through the accessors directly instead.
internal fun interface ElementGetter<T> {
    fun get(index: Int): T
}

internal object FlatbuffersListWrapper {

    fun <T> get(vector: FlexBuffers.Vector?, getter: ElementGetter<T>): List<T>? {
        return if (vector == null || vector.size() == 0) {
            null
        } else {
            get(vector.size(), getter)
        }
    }

    fun <T> get(listSize: Int, getter: ElementGetter<T>): List<T>? {
        if (listSize <= 0) return null
        return object : FlatbuffersNonCachingListWrapper<T>(listSize) {
            override fun get(index: Int): T {
                return getter.get(index)
            }
        }
    }

    private abstract class FlatbuffersNonCachingListWrapper<T>(
        override val size: Int,
    ) : AbstractList<T>()
}
