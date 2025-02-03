package com.mapbox.navigation.ui.base.formatter

/**
 * An interface for formatting from one value to another.
 */
fun interface ValueFormatter<in T, out R> {

    /**
     * Formats the input to the output.
     */
    fun format(update: T): R
}
