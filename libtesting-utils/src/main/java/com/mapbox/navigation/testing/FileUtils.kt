package com.mapbox.navigation.testing

object FileUtils {
    fun loadJsonFixture(fileName: String): String {
        return javaClass.classLoader?.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }!!
    }
}
