package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
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
            .map {
                listOf(
                    it.routeIndex.toString(),
                    it.routeOptions.toUrl("***").toString(),
                    it.directionsResponse.toJson(),
                    navigation().getAlternativeMetadataFor(it)?.alternativeId?.toString().orEmpty()
                )
            }
            .flatten()
            .joinToString(separator = "\n") { it }
        writeOnDisk("$routesChangedCounter-${result.reason}.txt", content)
        routesChangedCounter++
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