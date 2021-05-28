package com.mapbox.navigation.ui.maps.snapshotter.model

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Size
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(NavSDKRobolectricTestRunner::class)
class MapboxSnapshotterOptionsTest : BuilderTest<MapboxSnapshotterOptions,
    MapboxSnapshotterOptions.Builder>() {

    lateinit var ctx: Context

    override fun getImplementationClass(): KClass<MapboxSnapshotterOptions> =
        MapboxSnapshotterOptions::class

    override fun getFilledUpBuilder(): MapboxSnapshotterOptions.Builder {
        val mockDensity = 2.5f
        val mockStyleUri = "mapbox://style-uri"
        val mockRoutePrecision = 5
        val mockBitmapConfig = Bitmap.Config.RGB_565
        val mockSize = Size(500f, 250f)
        val mockEdgeInsets = EdgeInsets(10.0, 20.0, 30.0, 40.0)
        return MapboxSnapshotterOptions.Builder(ctx)
            .size(mockSize)
            .density(mockDensity)
            .styleUri(mockStyleUri)
            .edgeInsets(mockEdgeInsets)
            .bitmapConfig(mockBitmapConfig)
            .routePrecision(mockRoutePrecision)
    }

    @Test
    override fun trigger() {
        // see comments
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }
}
