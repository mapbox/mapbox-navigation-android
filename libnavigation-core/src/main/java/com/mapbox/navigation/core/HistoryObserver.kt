package com.mapbox.navigation.core

/**
 * Callback which is called as a result of [MapboxNavigation.retrieveHistory]
 */
fun interface HistoryObserver {
    /**
     * Invoked as a result of [MapboxNavigation.retrieveHistory] when the history file was stored
     *
     * @param filepath could be null if [MapboxNavigation.retrieveHistory] was called without any
     * actually received events or if the recorder was destroyed before [MapboxNavigation.retrieveHistory]
     * finished it's work.
     */
    fun onHistoryDumped(filepath: String?)
}
