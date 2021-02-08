package com.mapbox.navigation.ui.base.formatter

/**
 * An interface for formatting from one value to another.
 */
interface ValueFormatter<T, R> {

    /**
     * Formats the input to the output.
     */
    fun format(t: T): R
}
