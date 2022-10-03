package com.mapbox.navigation.core.internal.fasterroute

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

class RecordRouteObserverResults(
    private val navigation: ()->MapboxNavigation
) : RoutesObserver {

    private var scope = CoroutineScope(Dispatchers.IO)
    private var routesChangedCounter: Int = 0
    private lateinit var preparation: Deferred<File>

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        if (routesChangedCounter == 0) {
            prepareFolders()
        }
        val content = result.navigationRoutes
            .map { route ->
                listOf(
                    route.routeIndex.toString(),
                    route.routeOptions.toUrl("***").toString(),
                    route.directionsResponse.toJson(),
                    navigation().getAlternativeMetadataFor(route)?.let { toJson(it) }.orEmpty()
                )
            }
            .flatten()
            .joinToString(separator = "\n") { it }
        val alternativesIds = result.navigationRoutes.drop(1)
            .map { navigation().getAlternativeMetadataFor(it)!!.alternativeId }
            .joinToString(separator = ",") { it.toString() }
        writeOnDisk("$routesChangedCounter-${result.reason}-$alternativesIds.txt", content)
        routesChangedCounter++
    }

    private fun toJson(metadata: AlternativeRouteMetadata): String {
        val gson = GsonBuilder()
            .registerTypeAdapter(NavigationRoute::class.java, NavigationRouteTypeAdapter { error("I can only write") })
            .create()
        return gson.toJson(metadata)
    }

    private fun prepareFolders() {
        preparation = scope.async {
            val folder = File(navigation().navigationOptions.applicationContext.filesDir, "record-routes-observer")
            if (!folder.exists()) {
                folder.mkdir()
            } else {
                folder.deleteRecursively()
                folder.mkdir()
            }
            folder
        }
    }

    private fun writeOnDisk(fileName: String, content: String) {
        scope.launch {
            val folder = preparation.await()
            val file = File(folder, fileName)
            file.writeText(content)
        }
    }
}

class NavigationRouteTypeAdapter(
    private val routesProvider: (id: String) -> NavigationRoute
) : TypeAdapter<NavigationRoute>() {
    override fun write(out: JsonWriter, value: NavigationRoute) {
        out.value(value.id)
    }

    override fun read(`in`: JsonReader): NavigationRoute {
        val routeId = `in`.nextString()
        return routesProvider(routeId)
    }
}