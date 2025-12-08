package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlexBuffers

internal object FlatbuffersMapWrapper {

    fun <K, V> get(flexMap: FlexBuffers.Map?, getter: (Int) -> Map.Entry<K, V>): Map<K, V>? {
        if (flexMap == null || flexMap.size() == 0) return null
        return FlatbuffersNonCachingMapWrapper(flexMap, getter)
    }

    private class FlatbuffersNonCachingMapWrapper<K, V>(
        private val flexMap: FlexBuffers.Map,
        private val getter: (Int) -> Map.Entry<K, V>,
    ) : AbstractMap<K, V>() {

        override val size: Int = flexMap.size()

        override val entries: Set<Map.Entry<K, V>>
            get() = object : AbstractSet<Map.Entry<K, V>>() {

                override val size: Int = flexMap.size()

                override fun iterator(): Iterator<Map.Entry<K, V>> {
                    return object : Iterator<Map.Entry<K, V>> {

                        private var index = 0

                        override fun hasNext(): Boolean = index < size

                        override fun next(): Map.Entry<K, V> {
                            if (!hasNext()) throw NoSuchElementException()
                            return getter(index++)
                        }
                    }
                }
            }
    }
}
