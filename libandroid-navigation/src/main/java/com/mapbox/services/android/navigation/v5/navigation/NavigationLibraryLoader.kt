package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.logger.MapboxLogger

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
                MapboxLogger.e(Message("Failed to load native shared library."), error)
            }
        }
    }
}
