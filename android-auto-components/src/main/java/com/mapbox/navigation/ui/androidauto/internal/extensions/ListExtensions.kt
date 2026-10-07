@file:JvmName("ListEx")

package com.mapbox.navigation.ui.androidauto.internal.extensions

/**
 * Takes at most [limit] items and always keeps the item at [keepIndex]. When that item is beyond
 * the limit, it replaces the last item taken, so a selected item is never hidden.
 */
internal fun <T> List<T>.takeKeeping(limit: Int, keepIndex: Int): List<T> {
    if (size <= limit) return this
    if (limit <= 0) return emptyList()
    val taken = take(limit)
    if (keepIndex < limit || keepIndex !in indices) return taken
    return taken.dropLast(1) + this[keepIndex]
}

/**
 * The position of the item at [keepIndex] in the result of [takeKeeping].
 */
internal fun keptIndex(keepIndex: Int, shownSize: Int): Int =
    if (keepIndex < shownSize) keepIndex.coerceAtLeast(0) else shownSize - 1
