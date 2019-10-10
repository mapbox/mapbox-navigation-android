package com.mapbox.services.android.navigation.v5.internal.utils

import androidx.annotation.IntRange
import java.util.ArrayDeque

class RingBuffer<T>(
    @IntRange(from = 0) private val maxSize: Int
) : ArrayDeque<T>() {

    override fun add(element: T): Boolean {
        val result = super.add(element)
        resize()
        return result
    }

    override fun addFirst(element: T) {
        super.addFirst(element)
        resize()
    }

    override fun addLast(element: T) {
        super.addLast(element)
        resize()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val result = super.addAll(elements)
        resize()
        return result
    }

    override fun push(element: T) {
        super.push(element)
        resize()
    }

    private fun resize() {
        while (size > maxSize) {
            pop()
        }
    }
}
