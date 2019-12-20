package com.mapbox.services.android.navigation.v5.navigation

import timber.log.Timber

abstract class NavigationLibraryLoader {

    abstract fun load(name: String)

    companion object {

        private val NAVIGATION_NATIVE = "navigator-android"
        private val DEFAULT: NavigationLibraryLoader = object : NavigationLibraryLoader() {
            override fun load(name: String) {
                System.loadLibrary(name)
            }
        }

        @Volatile
        private var loader = DEFAULT

        /**
         * Loads navigation shared library.
         *
         *
         * Catches UnsatisfiedLinkErrors and prints a warning to logcat.
         *
         */
        fun load() {
            try {
                loader.load(NAVIGATION_NATIVE)
            } catch (error: UnsatisfiedLinkError) {
                Timber.e(error, "Failed to load native shared library.")
            }
        }
    }
}
