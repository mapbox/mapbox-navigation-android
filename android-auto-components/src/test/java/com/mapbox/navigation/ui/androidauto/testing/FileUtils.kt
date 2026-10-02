package com.mapbox.navigation.ui.androidauto.testing

object FileUtils {
    fun loadJsonFixture(fileName: String): String {
        return javaClass.classLoader?.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }!!
    }
}
