package com.mapbox.navigation.ui.maps.snapshotter

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Image
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.ui.maps.snapshotter.model.CameraPosition
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.turf.TurfMeasurement
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
class SnapshotterProcessorTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `process action generate snapshot result snapshot unavailable no banner instructions`() {
        val bannerInstructions: BannerInstructions? = null

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotUnavailable)
    }

    @Test
    fun `process action generate snapshot result snapshot unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()

        every { bannerInstructions.view() } returns null

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotUnavailable)
    }

    @Test
    fun `process action generate snapshot result snapshot unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()

        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotUnavailable)
    }

    @Test
    fun `process action generate snapshot result snapshot unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()

        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotUnavailable)
    }

    @Test
    fun `process action generate snapshot result snapshot unavailable no sub type component`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewType())

        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotUnavailable)
    }

    @Test
    fun `process action generate snapshot result snapshot available`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSignboardSubType())

        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList

        val action = SnapshotterAction.GenerateSnapshot(bannerInstructions)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotAvailable)
    }

    @Test
    fun `process action generate camera position no current geometry`() {
        val currentGeometry = null
        val nextGeometry = "nextStepGeometry"
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)
        val expectedResult = SnapshotterResult.SnapshotterCameraPosition(null)
        val actualResult = SnapshotterProcessor.process(action)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `process action generate camera position no next geometry`() {
        val currentGeometry = "currentGeometry"
        val nextGeometry = null
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)
        val expectedResult = SnapshotterResult.SnapshotterCameraPosition(null)
        val actualResult = SnapshotterProcessor.process(action)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `process action generate camera position no current or next geometry`() {
        val currentGeometry = null
        val nextGeometry = null
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)
        val expectedResult = SnapshotterResult.SnapshotterCameraPosition(null)
        val actualResult = SnapshotterProcessor.process(action)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `process action generate camera position check bearing`() {
        val currentGeometry = getDirectionsRoute().legs()!![0].steps()!![0].geometry()
        val nextGeometry = getDirectionsRoute().legs()!![0].steps()!![1].geometry()
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val point1 = Point.fromLngLat(-3.585887766653666, 40.575847283559874)
        val point2 = Point.fromLngLat(-3.58577, 40.57647)
        val cameraPosition = CameraPosition(
            listOf(),
            options.edgeInsets,
            TurfMeasurement.bearing(point1, point2),
            0.0
        )
        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)

        val expectedBearing = cameraPosition.bearing
        val actualBearing =
            (SnapshotterProcessor.process(action) as SnapshotterResult.SnapshotterCameraPosition)
                .cameraPosition!!.bearing

        assertEquals(expectedBearing, actualBearing, 0.0)
    }

    @Test
    fun `process action generate camera position check frame points`() {
        val currentGeometry = getDirectionsRoute().legs()!![0].steps()!![0].geometry()
        val nextGeometry = getDirectionsRoute().legs()!![0].steps()!![1].geometry()
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val cameraPosition = CameraPosition(
            listOf(
                Point.fromLngLat(-3.585887766653666, 40.575847283559874),
                Point.fromLngLat(-3.58586, 40.576),
                Point.fromLngLat(-3.58581, 40.57627),
                Point.fromLngLat(-3.5858, 40.57635),
                Point.fromLngLat(-3.58577, 40.57647),
                Point.fromLngLat(-3.58577, 40.57647),
                Point.fromLngLat(-3.585727976530563, 40.5765540472028)
            ),
            options.edgeInsets,
            0.0,
            0.0
        )
        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)

        val expectedFramePoints = cameraPosition.points
        val actualFramePoints =
            (SnapshotterProcessor.process(action) as SnapshotterResult.SnapshotterCameraPosition)
                .cameraPosition!!.points

        assertEquals(expectedFramePoints, actualFramePoints)
    }

    @Test
    fun `process action generate camera position check edge insets`() {
        val currentGeometry = getDirectionsRoute().legs()!![0].steps()!![0].geometry()
        val nextGeometry = getDirectionsRoute().legs()!![0].steps()!![1].geometry()
        val options =
            MapboxSnapshotterOptions.Builder(ctx)
                .edgeInsets(EdgeInsets(10.0, 20.0, 30.0, 40.0))
                .build()

        val cameraPosition = CameraPosition(
            listOf(),
            options.edgeInsets,
            0.0,
            0.0
        )
        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)

        val expectedInsets = cameraPosition.insets
        val actualInsets =
            (SnapshotterProcessor.process(action) as SnapshotterResult.SnapshotterCameraPosition)
                .cameraPosition!!.insets

        assertEquals(expectedInsets, actualInsets)
    }

    @Test
    fun `process action generate camera position check pitch`() {
        val currentGeometry = getDirectionsRoute().legs()!![0].steps()!![0].geometry()
        val nextGeometry = getDirectionsRoute().legs()!![0].steps()!![1].geometry()
        val options = MapboxSnapshotterOptions.Builder(ctx).build()
        val cameraPosition = CameraPosition(
            listOf(),
            options.edgeInsets,
            0.0,
            72.0
        )
        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)

        val expectedPitch = cameraPosition.pitch
        val actualPitch =
            (SnapshotterProcessor.process(action) as SnapshotterResult.SnapshotterCameraPosition)
                .cameraPosition!!.pitch

        assertEquals(expectedPitch, actualPitch, 0.0)
    }

    @Test
    fun `process action generate camera position result camera position`() {
        val currentGeometry = getDirectionsRoute().legs()!![0].steps()!![0].geometry()
        val nextGeometry = getDirectionsRoute().legs()!![0].steps()!![1].geometry()
        val options = MapboxSnapshotterOptions.Builder(ctx).build()

        val action =
            SnapshotterAction.GenerateCameraPosition(currentGeometry, nextGeometry, options)
        val result = SnapshotterProcessor.process(action)
        assert(result is SnapshotterResult.SnapshotterCameraPosition)
    }

    @Test
    fun `process action generate bitmap result failure`() {
        val options = MapboxSnapshotterOptions.Builder(ctx).build()
        val snapshot: MapSnapshotInterface? = null
        val action = SnapshotterAction.GenerateBitmap(options, snapshot)

        val actualResult = SnapshotterProcessor.process(action)

        assertEquals(
            "Failed to generate map snapshot.",
            (actualResult as SnapshotterResult.Snapshot.Failure).error
        )
    }

    @Test
    fun `process action generate bitmap result success`() {
        val options = MapboxSnapshotterOptions.Builder(ctx).build()
        val mockSnapshot: MapSnapshotInterface = mockk()
        val mockImage: Image = mockk()

        every { mockImage.height } returns 1
        every { mockImage.width } returns 1
        every { mockImage.data } returns byteArrayOf(-56, -50, -62, -1, -56, -50)
        every { mockSnapshot.image() } returns mockImage

        val action = SnapshotterAction.GenerateBitmap(options, mockSnapshot)

        val result = SnapshotterProcessor.process(action)

        assert(result is SnapshotterResult.Snapshot.Success)
    }

    @Test
    fun `process action generate bitmap result success check bitmap`() {
        val options = MapboxSnapshotterOptions.Builder(ctx).build()
        val data = byteArrayOf(-56, -50, -62, -1, -56, -50, -62, -1, -55, -50, -62)
        val mockSnapshot: MapSnapshotInterface = mockk()
        val mockImage: Image = mockk()

        every { mockImage.height } returns 1
        every { mockImage.width } returns 1
        every { mockImage.data } returns data
        every { mockSnapshot.image() } returns mockImage

        val action = SnapshotterAction.GenerateBitmap(options, mockSnapshot)

        val expectedBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        expectedBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        val result = SnapshotterProcessor.process(action)

        assert((result as SnapshotterResult.Snapshot.Success).bitmap.sameAs(expectedBitmap))
    }

    @Test
    fun `process action generate line layer`() {
        val action = SnapshotterAction.GenerateLineLayer
        val result = SnapshotterProcessor.process(action)

        assert(result is SnapshotterResult.SnapshotLineLayer)
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val asJson = FileUtils.loadJsonFixture("snapshotter-here-based-junctions.json")
        return DirectionsRoute.fromJson(asJson)
    }

    private fun getComponentGuidanceViewType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSignboardSubType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SIGNBOARD)
            .text("some text")
            .imageUrl(null)
            .build()
    }
}
