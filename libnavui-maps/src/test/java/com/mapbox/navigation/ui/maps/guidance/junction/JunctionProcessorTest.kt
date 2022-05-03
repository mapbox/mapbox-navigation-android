package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxRasterToBitmapParser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class JunctionProcessorTest {

    @Test
    fun `process action junction availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action junction availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action junction availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no subType component`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no image url`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSignboardSubType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result available`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSignboardSubTypeImageUrl())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionAvailable("https://abc.mapbox.com")
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionAvailable

        assertEquals(expected.junctionUrl, result.junctionUrl)
    }

    @Test
    fun `processing PrepareJunctionRequest action should return JunctionRequest`() {
        val action = JunctionAction.PrepareJunctionRequest("https://abc.mapbox.com")

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRequest

        assertEquals(action.junctionUrl, result.request.url)
    }

    @Test
    fun `process ProcessJunctionResponse action with ResourceLoadStatus UNAUTHORIZED`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.UNAUTHORIZED,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> =
            ExpectedFactory.createValue(loadResult)
        val action = JunctionAction.ProcessJunctionResponse(response)
        val expected = JunctionResult.JunctionRaster.Failure(
            "Your token cannot access this " +
                "resource, contact support"
        )

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRaster.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process ProcessJunctionResponse action with ResourceLoadStatus NOT_FOUND`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.NOT_FOUND,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> =
            ExpectedFactory.createValue(loadResult)
        val expected = JunctionResult.JunctionRaster.Failure("Resource is missing")
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRaster.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process ProcessJunctionResponse action with ResourceLoadStatus AVAILABLE but with no data`() {
        val loadResult = resourceLoadResult(
            data = resourceData(ByteArray(0)),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> =
            ExpectedFactory.createValue(loadResult)
        val expected = JunctionResult.JunctionRaster.Empty
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRaster.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process ProcessJunctionResponse action with ResourceLoadStatus AVAILABLE`() {
        val blob = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val loadResult = resourceLoadResult(
            data = resourceData(blob),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> =
            ExpectedFactory.createValue(loadResult)
        val expected = JunctionResult.JunctionRaster.Success(blob)
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRaster.Success

        assertEquals(expected.data, result.data)
    }

    @Test
    fun `process action junction process raster to bitmap failure`() {
        mockkStatic(MapboxRasterToBitmapParser::class)
        val mockData = byteArrayOf()
        val action = JunctionAction.ParseRasterToBitmap(mockData)
        every { MapboxRasterToBitmapParser.parse(any()) } returns
            ExpectedFactory.createError(
                "Error parsing raster to bitmap as raster is empty"
            )

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionBitmap.Failure

        assertEquals("Error parsing raster to bitmap as raster is empty", result.message)
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
            .subType(BannerComponents.JCT)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSignboardSubTypeImageUrl(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.JCT)
            .text("some text")
            .imageUrl("https://abc.mapbox.com")
            .build()
    }

    private fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): ByteArray = blob
    }

    private fun resourceLoadResult(
        data: ResourceData?,
        status: ResourceLoadStatus,
        immutable: Boolean = false,
        mustRevalidate: Boolean = false,
        expires: Date = Date(),
        totalBytes: Long = 0,
        transferredBytes: Long = 0,
        contentType: String = "image/png"
    ): ResourceLoadResult {
        return ResourceLoadResult(
            data,
            status,
            immutable,
            mustRevalidate,
            expires,
            totalBytes,
            transferredBytes,
            contentType
        )
    }
}
