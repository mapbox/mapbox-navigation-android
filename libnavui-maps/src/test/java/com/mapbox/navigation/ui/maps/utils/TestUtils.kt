package com.mapbox.navigation.ui.maps.utils

import java.util.*

object TestUtils {

    fun loadJsonFixture(filename: String): String? {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader?.getResourceAsStream(filename)
        val scanner = Scanner(inputStream!!, "UTF-8").useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }
}
