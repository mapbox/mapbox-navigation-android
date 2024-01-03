package com.mapbox.navigation.benchmark

import android.content.Context
import android.util.Log
import androidx.annotation.IntegerRes
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.bindgen.Value
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.trip.model.eh.OpenLRStandard
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.CacheFactory
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteParser
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val LOG_TAG = "vadzim-test"

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    init {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
    }

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun timeToRetrieveRouteAlerts() {
        val route = routeWithLongIncident()
        logShapeOfFirstRouteAlertLocation(route)
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }

    @Test
    fun timeToRetrieveRouteAlertsWithShapes() {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        val route = routeWithLongIncident()
        logShapeOfFirstRouteAlertLocation(route)
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            alerts.forEach {
                val location = it.roadObject.location
                if (location.isRouteAlertLocation) {
                    location.routeAlertLocation.shape
                }
            }
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }

    //@Test
    fun timeToRetrieveOpenLRMatchedObject() {
        val navigation = MapboxNavigation(
            NavigationOptions.Builder(context)
                .accessToken(context.getString(R.string.mapbox_access_token))
                .routingTilesOptions(
                    RoutingTilesOptions.Builder().tileStore(createTileStore(context)).build()
                )
                .build()
        )
        Log.d(LOG_TAG, "loading tiles for matching")
        runBlocking {
            loadRegion(navigation, OfflineRegion(
                "test",
                FeatureCollection.fromJson(taiwanGeometry).features()!!.first().geometry()!!)
            )
        }
        Log.d(LOG_TAG, "matching open lr object")
        val testObjectId = "test"
        val roadObject = runBlocking<RoadObject> {
            suspendCoroutine { continuation ->
                navigation.roadObjectMatcher.registerRoadObjectMatcherObserver { result ->
                    result.onError {
                        Log.e(LOG_TAG, "error matching object: $it")
                        error("can't match object")
                    }.onValue {
                        continuation.resume(it)
                    }
                }
                navigation.roadObjectMatcher.matchOpenLRObjects(
                    listOf(
                        com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr(
                            testObjectId,
                            "C1ZAJBE5bxNQ//RGDFITQA4A5wHdE2nk/w==",
                            OpenLRStandard.TOM_TOM
                        )
                    ),
                    true
                )
            }
        }
        val store = navigation.roadObjectsStore
        store.addCustomRoadObject(roadObject)
        Log.d(LOG_TAG, "starting measurements")
        benchmarkRule.measureRepeated {
            val roadObject = store.getRoadObject(testObjectId)
            //roadObject!!.location.openLRLineLocation
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }

    private fun logShapeOfFirstRouteAlertLocation(route: RouteInterface) {
        val alertsCount = route.routeInfo.alerts.size
        val shape = route.routeInfo.alerts
            .filter { it.roadObject.type == RoadObjectType.INCIDENT }
            .map { it.roadObject.location }
            .first()
            .routeAlertLocation
            .shape
        Log.d(LOG_TAG, "alerts count is $alertsCount")
        Log.d(LOG_TAG, "shape is $shape")
        val lineStringCoordinates = (shape as? LineString)?.coordinates()?.size
        Log.d(LOG_TAG, "Line string coordinates count is $lineStringCoordinates")
    }

    private fun routeWithLongIncident(): RouteInterface {
        val response = readRawFileText(context, R.raw.test_route)
        val route = RouteParser.parseDirectionsResponse(
            response,
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/14.958055640969633,51.2002127680808;5.2869509774857875,51.058961031629536?access_token=***&geometries=polyline6&overview=full&steps=true&continue_straight=true&annotations=congestion_numeric%2Cmaxspeed%2Cclosure%2Cspeed%2Cduration%2Cdistance&language=en&roundabout_exits=true&voice_instructions=true&banner_instructions=true&voice_units=imperial&enable_refresh=true",
            RouterOrigin.ONLINE
        ).onError {
            Log.d(LOG_TAG, "error parsing route $it")
            Log.d(LOG_TAG, "response was $response")
        }.value!!.first()
        return route
    }
}

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }

private fun provideCacheHandle(
    tilesConfig: TilesConfig = provideTilesConfig(),
    configHandle: ConfigHandle,
): CacheHandle =
    CacheFactory.build(tilesConfig, configHandle, null)

private fun provideTilesConfig(): TilesConfig = TilesConfig(
    InstrumentationRegistry.getInstrumentation()
        .targetContext.getExternalFilesDir(null)!!.absolutePath,
    null,
    null,
    null,
    null,
)


data class OfflineRegion(
    val id: String,
    val geometry: Geometry
)
suspend fun loadRegion(navigation: MapboxNavigation, region: OfflineRegion) {
    val navTilesetDescriptor = navigation.tilesetDescriptorFactory.getLatest()

    val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
        .geometry(region.geometry)
        .descriptors(listOf(navTilesetDescriptor))
        .build()
    val tileStore = navigation.navigationOptions.routingTilesOptions.tileStore!!

    suspendCancellableCoroutine<Unit> { continuation ->
        tileStore.loadTileRegion(
            region.id,
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
            }
        )
    }
}

fun createTileStore(context: Context): TileStore {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val tileStore = TileStore.create()
    tileStore.setOption(
        TileStoreOptions.MAPBOX_ACCESS_TOKEN,
        TileDataDomain.NAVIGATION,
        Value.valueOf(context.getString(R.string.mapbox_access_token))
    )
    return tileStore
}

val taiwanGeometry = "{\n" +
    "  \"type\": \"FeatureCollection\",\n" +
    "  \"features\": [\n" +
    "    {\n" +
    "      \"type\": \"Feature\",\n" +
    "      \"properties\": {},\n" +
    "      \"geometry\": {\n" +
    "        \"coordinates\": [\n" +
    "          [\n" +
    "            [\n" +
    "              121.27058293773052,\n" +
    "              24.21893612393643\n" +
    "            ],\n" +
    "            [\n" +
    "              121.27058293773052,\n" +
    "              24.170221332936876\n" +
    "            ],\n" +
    "            [\n" +
    "              121.32713050767683,\n" +
    "              24.170221332936876\n" +
    "            ],\n" +
    "            [\n" +
    "              121.32713050767683,\n" +
    "              24.21893612393643\n" +
    "            ],\n" +
    "            [\n" +
    "              121.27058293773052,\n" +
    "              24.21893612393643\n" +
    "            ]\n" +
    "          ]\n" +
    "        ],\n" +
    "        \"type\": \"Polygon\"\n" +
    "      }\n" +
    "    }\n" +
    "  ]\n" +
    "}"