package com.mapbox.navigation.core.trip.model.eh

import com.google.gson.Gson
import com.mapbox.navigator.ElectronicHorizon
import org.apache.commons.io.IOUtils

object EHorizonTestUtil {
    fun loadSmallGraph(): ElectronicHorizon {
        return resourceAsElectronicHorizon("ehorizon_small_graph.txt")
    }

    fun loadLongBranch(): ElectronicHorizon {
        return resourceAsElectronicHorizon("ehorizon_long_branch.txt")
    }

    private fun resourceAsElectronicHorizon(name: String): ElectronicHorizon {
        val packageName = "com.mapbox.navigation.core.ehorizon"
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        val resourceAsString = IOUtils.toString(inputStream, "UTF-8")
        return Gson().fromJson(resourceAsString, ElectronicHorizon::class.java)
    }
}
