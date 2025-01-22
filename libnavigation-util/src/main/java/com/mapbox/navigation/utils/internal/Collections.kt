package com.mapbox.navigation.utils.internal

import kotlin.math.roundToInt

/**
 * Takes exactly [n] elements distributed evenly.
 *
 * For example, for list (0, 1, 2, 3, 4, 5, 6)
 * 0 -> []
 * 1 -> [0]
 * 2 -> [0, 6]
 * 3 -> [0, 3, 6]
 * 4 -> [0, 2, 4, 6]
 *
 * Returns empty list for [n] <= 0.
 */
fun <T> List<T>.takeEvenly(n: Int): List<T> {
    if (this.isEmpty() || n <= 0) return emptyList()
    if (n == 1) return listOf(this.first())
    if (n >= this.size) return this.toList()

    val result = mutableListOf<T>()
    val step = (this.size - 1).toDouble() / (n - 1)

    for (i in 0 until n) {
        val index = (i * step).roundToInt().coerceIn(0, this.size - 1)
        if (result.isEmpty() || result.last() != this[index]) {
            result.add(this[index])
        }
    }

    return result
}
