package com.mapbox.navigation.instrumentation_tests.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.ArrivalIdlingResource
import org.junit.Rule
import org.junit.Test
import java.io.File


class SanityUiRouteTest : SimpleMapViewNavigationTest() {
    lateinit var context: Context


    @Test
    fun route_completes() {

        context = InstrumentationRegistry.getInstrumentation().targetContext
        // puck needs to be added first,
        // see https://github.com/mapbox/mapbox-navigation-android-internal/issues/102
        addLocationPuck()
        addRouteLine()
        addNavigationCamera()
        val arrivalIdlingResource = ArrivalIdlingResource(mapboxNavigation)
        arrivalIdlingResource.register()
     //   try {
            Espresso.onIdle()
//        } catch (e: Exception) {
//            val hist = mapboxNavigation.retrieveHistory()
//
//            val filesPath = context.externalCacheDir
//            Log.e("NavNative", filesPath.toString())
//            File(filesPath, "history.json").printWriter().use { out ->
//                out.print(hist);
//            }
//        }

        arrivalIdlingResource.unregister()
    }
}
