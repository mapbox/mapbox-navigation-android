package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.internal.extensions.retrieveNavigatorHandle
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigator.NavigatorHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI

class NavigatorInstanceReleaseTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Test
    fun nativeNavigatorThreadsDoNotAccumulateAcrossCreateDestroyLifecycles() {
        val cycles = 5

        runOnMainSync { MapboxNavigationProvider.create(buildNavigationOptions()) }
        val threadCountWhileAlive = countNativeNavigatorThreads()
        assertTrue(
            "Expected NN-* threads to be present while a navigator instance is alive; " +
                "found 0 — the thread-name prefix may have changed",
            threadCountWhileAlive > 0,
        )
        runOnMainSync { MapboxNavigationProvider.destroy() }

        repeat(cycles - 1) {
            runOnMainSync {
                MapboxNavigationProvider.create(buildNavigationOptions())
            }
            runOnMainSync {
                MapboxNavigationProvider.destroy()
            }
        }

        val leakedThreadCount = waitForNativeNavigatorThreadsToTerminate(timeoutMs = 30_000)

        assertEquals(
            "Expected 0 NN-* threads after $cycles create/destroy cycles and GC; " +
                "found $leakedThreadCount — one or more NavigatorImpl instances were not released",
            0,
            leakedThreadCount,
        )
    }

    @Test
    fun destroyedMapboxNavigationIsCollectedWhileItsNavigatorHandleIsStillReferenced() {
        // SDK components (e.g. HD controllers) may hold the handle after destroy.
        lateinit var navigationReference: WeakReference<MapboxNavigation>
        lateinit var navigatorHandle: NavigatorHandle
        runOnMainSync {
            val navigation = MapboxNavigationProvider.create(buildNavigationOptions())
            navigationReference = WeakReference(navigation)
            navigatorHandle = navigation.retrieveNavigatorHandle()
            MapboxNavigationProvider.destroy()
        }

        val collected = waitForCollection(navigationReference, timeoutMs = 30_000)

        // Using the handle here keeps it alive during the wait.
        assertTrue("MapboxNavigation retained by navigator handle $navigatorHandle", collected)
    }

    private fun waitForCollection(reference: WeakReference<*>, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (reference.get() != null && System.currentTimeMillis() < deadline) {
            Runtime.getRuntime().gc()
            System.runFinalization()
            Thread.sleep(500)
        }
        return reference.get() == null
    }

    private fun buildNavigationOptions() = NavigationOptions.Builder(context)
        .routingTilesOptions(
            RoutingTilesOptions.Builder()
                .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                .build(),
        )
        .build()

    private fun waitForNativeNavigatorThreadsToTerminate(timeoutMs: Long): Int {
        val deadline = System.currentTimeMillis() + timeoutMs
        var count = countNativeNavigatorThreads()
        while (count > 0 && System.currentTimeMillis() < deadline) {
            System.gc()
            System.runFinalization()
            Thread.sleep(1_000)
            count = countNativeNavigatorThreads()
        }
        return count
    }

    private fun countNativeNavigatorThreads(): Int =
        File("/proc/self/task/")
            .listFiles()
            ?.count { taskDir ->
                try {
                    File(taskDir, "status").readLines().any { line ->
                        line.startsWith("Name:") &&
                            line.substringAfter("Name:").trim().startsWith("NN-")
                    }
                } catch (_: Exception) {
                    false
                }
            } ?: 0
}
