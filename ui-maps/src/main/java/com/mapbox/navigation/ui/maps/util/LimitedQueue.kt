package com.mapbox.navigation.ui.maps.util

import java.util.LinkedList

internal class LimitedQueue<T>(private val maxSize: Int) : Iterable<T> {

    private val list = LinkedList<T>()

    fun add(item: T) {
        list.add(item)
        while (list.size > maxSize) {
            list.remove()
        }
    }

    fun clear() {
        list.clear()
    }

    override fun iterator(): Iterator<T> {
        return list.iterator()
    }
}
