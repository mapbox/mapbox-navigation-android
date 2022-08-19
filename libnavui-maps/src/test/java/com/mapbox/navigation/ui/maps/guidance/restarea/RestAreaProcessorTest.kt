package com.mapbox.navigation.ui.maps.guidance.restarea

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class RestAreaProcessorTest {

    @Test
    fun `process action sapa map availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no subType component`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no image url`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSapaSubType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result available`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(
            getComponentGuidanceViewTypeSapaSubTypeImageUrl("https://abc.mapbox.com&")
        )
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapAvailable("https://abc.mapbox.com&")
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapAvailable

        assertEquals(expected.sapaMapUrl, result.sapaMapUrl)
    }

    @Test
    fun `process prepare sapa map request action should return sapa map request`() {
        val action = RestAreaAction.PrepareRestAreaMapRequest("https://abc.mapbox.com")

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapRequest

        assertEquals(action.sapaMapUrl, result.request.url)
    }

    @Test
    fun `process process sapa map response action with resource load status unauthorized`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.UNAUTHORIZED,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)
        val expected = RestAreaResult.RestAreaMapSvg.Failure(
            "Your token cannot access this resource, contact support"
        )

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process process sapa map response action with resource load status not found`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.NOT_FOUND,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Failure("Resource is missing")
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process sapa map response action with resource load status available without data`() {
        val loadResult = resourceLoadResult(
            data = resourceData(ByteArray(0)),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Empty
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process sapa map response action with resource load status available`() {
        val blob = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val loadResult = resourceLoadResult(
            data = resourceData(blob),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Success(blob)
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Success

        assertEquals(expected.data, result.data)
    }

    @Test
    fun `process action sapa map process bytearray to bitmap failure`() {
        mockkObject(SvgUtil)
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val mockOptions = mockk<MapboxRestAreaOptions> {
            every { desiredGuideMapWidth } returns 1000
        }
        val action = RestAreaAction.ParseSvgToBitmap(mockData, mockOptions)
        every {
            SvgUtil.renderAsBitmapWithWidth(any(), any(), any())
        }.throws(IllegalStateException("whatever"))

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaBitmap.Failure

        assertEquals("whatever", result.error)
        unmockkObject(SvgUtil)
    }

    private fun getComponentGuidanceViewType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSapaSubType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SAPAGUIDEMAP)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSapaSubTypeImageUrl(url: String): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SAPAGUIDEMAP)
            .text("some text")
            .imageUrl(url)
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
