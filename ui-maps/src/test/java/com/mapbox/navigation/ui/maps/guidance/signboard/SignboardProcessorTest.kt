package com.mapbox.navigation.ui.maps.guidance.signboard

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.utils.toByteArray
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SignboardProcessorTest {

    @Test
    fun `process action signboard availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardAvailable("https://abc.mapbox.com")
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardAvailable

        assertEquals(expected.signboardUrl, result.signboardUrl)
    }

    @Test
    fun `process PrepareSignboardRequest action should return SignboardRequest`() {
        val action = SignboardAction.PrepareSignboardRequest("https://abc.mapbox.com")

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardRequest

        assertEquals(action.signboardUrl, result.request.url)
    }

    @Test
    fun `process ProcessSignboardResponse action with ResourceLoadStatus UNAUTHORIZED`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.UNAUTHORIZED,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val action = SignboardAction.ProcessSignboardResponse(response)
        val expected = SignboardResult.SignboardSvg.Failure(
            "Your token cannot access this " +
                "resource, contact support",
        )

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process ProcessSignboardResponse action with ResourceLoadStatus NOT_FOUND`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.NOT_FOUND,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = SignboardResult.SignboardSvg.Failure("Resource is missing")
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process ProcessSignboardResponse action with ResourceLoadStatus AVAILABLE but with no data`() {
        val loadResult = resourceLoadResult(
            data = resourceData(ByteArray(0)),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = SignboardResult.SignboardSvg.Empty
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process ProcessSignboardResponse action with ResourceLoadStatus AVAILABLE`() {
        val blob = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val loadResult = resourceLoadResult(
            data = resourceData(blob),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = SignboardResult.SignboardSvg.Success(blob.toDataRef())
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Success

        val expectedBytes = expected.data.toByteArray()
        val actualBytes = result.data.toByteArray()
        assertTrue(expectedBytes.contentEquals(actualBytes))
    }

    @Test
    fun `process action signboard process bytearray to bitmap failure`() {
        val mockParser = mockk<SvgToBitmapParser>()
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11).toDataRef()
        val mockOptions = mockk<MapboxSignboardOptions>()
        val action = SignboardAction.ParseSvgToBitmap(mockData, mockParser, mockOptions)
        every {
            mockParser.parse(any(), any())
        } returns ExpectedFactory.createError("whatever")

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardBitmap.Failure

        assertEquals("whatever", result.message)
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

    private fun getComponentGuidanceViewTypeSignboardSubTypeImageUrl(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SIGNBOARD)
            .text("some text")
            .imageUrl("https://abc.mapbox.com")
            .build()
    }

    private fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): DataRef = blob.toDataRef()
    }

    private fun resourceLoadResult(
        data: ResourceData?,
        status: ResourceLoadStatus,
        immutable: Boolean = false,
        mustRevalidate: Boolean = false,
        expires: Date = Date(),
        totalBytes: Long = 0,
        transferredBytes: Long = 0,
        contentType: String = "image/png",
        etag: String = "",
        belongsToGroup: Boolean = false,
    ): ResourceLoadResult {
        return ResourceLoadResult(
            data,
            status,
            immutable,
            mustRevalidate,
            expires,
            totalBytes,
            transferredBytes,
            contentType,
            etag,
            belongsToGroup,
        )
    }
}
