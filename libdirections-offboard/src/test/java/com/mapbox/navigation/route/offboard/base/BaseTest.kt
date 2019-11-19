package com.mapbox.navigation.route.offboard.base

import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.io.IOException
import java.util.Scanner
import okhttp3.internal.Util.UTF_8

open class BaseTest {

    companion object {
        private const val MULTI_LEG_ROUTE_FIXTURE = "directions_two_leg_route.json"
    }

    @Throws(IOException::class)
    protected fun loadJsonFixture(filename: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(filename)
        return inputStream?.let {
            val scanner = Scanner(it, UTF_8.name()).useDelimiter("\\A")
            if (scanner.hasNext()) scanner.next() else ""
        } ?: ""
    }

    @Throws(Exception::class)
    protected fun buildMultipleLegRoute(): DirectionsRoute {
        val body = loadJsonFixture(MULTI_LEG_ROUTE_FIXTURE)
        val gson = GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()
        val response = gson.fromJson(body, DirectionsResponse::class.java)
        return response.routes()[0]
    }
}
