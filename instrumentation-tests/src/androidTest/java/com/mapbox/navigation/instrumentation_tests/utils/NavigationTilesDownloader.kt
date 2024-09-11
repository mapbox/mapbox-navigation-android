package com.mapbox.navigation.instrumentation_tests.utils

import android.os.Environment
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Polygon
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NavigationTilesDownloader {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun run() {
        val tileStoreDir = File(context.filesDir, ".mapbox/tile_store").apply {
            deleteRecursively()
            mkdirs()
        }

        withMapboxNavigation { navigation ->
            val region = getDownloadPolygon()
            if (region != null) {
                try {
                    loadRegion(navigation, region)
                    println("Navigation tiles have been downloaded")
                } catch (e: Exception) {
                    System.err.println("Failed to download navigation tiles: ${e.message}")
                    return@withMapboxNavigation
                }

                try {
                    val outputFile = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "navigation-tiles.zip",
                    )
                    ZipUtils.zipDirectory(tileStoreDir, outputFile)
                    println("Navigation tiles are zipped to '$outputFile'")
                } catch (e: Exception) {
                    System.err.println("Failed to zip downloaded navigation tiles: ${e.message}")
                }
            }
        }
    }

    private fun getDownloadPolygon(): Polygon? {
        val arguments = InstrumentationRegistry.getArguments()

        val downloadPolygon = arguments.getString("downloadPolygon") ?: return null.also {
            System.err.println(
                """
                No polygon to download provided. Please specify it using the `downloadPolygon` parameter, 
                which accepts an encoded polyline with a precision of 5. This encoded polyline 
                defines the geographic region to be downloaded.
                
                Example:
                POLYGON=$(printf "%q" "ucq_I}|jpAtjE??{~PujE?")                
                adb shell am instrument -w -e class com.mapbox.navigation.instrumentation_tests.utils.NavigationTilesDownloader -e downloadPolygon ${'$'}POLYGON com.mapbox.navigation.instrumentation_tests.test/androidx.test.runner.AndroidJUnitRunner
                """.trimIndent(),
            )
        }

        val points = PolylineUtils.decode(downloadPolygon, 5)
        return Polygon.fromLngLats(listOf(points + points.first()))
    }

    private fun withMapboxNavigation(block: suspend (navigation: MapboxNavigation) -> Unit) {
        val routingTilesOptions = RoutingTilesOptions.Builder()
            .tileStore(TileStore.create())
            .build()

        val navigationOptions = NavigationOptions.Builder(context)
            .routingTilesOptions(routingTilesOptions)
            .deviceProfile(DeviceProfile.Builder()/*.customConfig(config)*/.build())
            .rerouteOptions(RerouteOptions.Builder().build())
            .build()

        try {
            runBlocking {
                block(MapboxNavigationProvider.create(navigationOptions))
            }
        } finally {
            MapboxNavigationProvider.destroy()
        }
    }

    private suspend fun loadRegion(navigation: MapboxNavigation, region: Geometry) {
        val navTilesetDescriptor = navigation.tilesetDescriptorFactory.getLatest()

        val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .geometry(region)
            .descriptors(listOf(navTilesetDescriptor))
            .build()
        val tileStore = navigation.navigationOptions.routingTilesOptions.tileStore!!

        suspendCancellableCoroutine { continuation ->
            tileStore.loadTileRegion(
                "region",
                tileRegionLoadOptions,
                { progress ->
                    Log.d("loadRegion", progress.toString())
                },
                { expected ->
                    if (expected.isValue) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(Throwable(expected.error!!.message))
                    }
                },
            )
        }
    }
}
