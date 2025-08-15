package com.mapbox.navigation.tripdata.shield

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
internal class RoadShieldDownloaderTest {

    private lateinit var mockResourceLoader: ResourceLoader
    private lateinit var sut: RoadShieldDownloader

    @Before
    fun setUp() {
        mockkObject(ResourceLoaderFactory)

        mockResourceLoader = mockk(relaxed = true)
        every { ResourceLoaderFactory.getInstance() } returns mockResourceLoader

        sut = RoadShieldDownloader
    }

    @After
    fun tearDown() {
        unmockkObject(ResourceLoaderFactory)
    }

    @Test
    fun `download - should use ResourceLoader`() = runBlockingTest {
        val url = "http://example.com/some-shield-asset"
        val requestCapture = slot<ResourceLoadRequest>()
        val callbackCapture = slot<ResourceLoadCallback>()
        val loadResult = Fixtures.resourceLoadResult(
            null,
            ResourceLoadStatus.NOT_FOUND,
        )
        every {
            mockResourceLoader.load(capture(requestCapture), capture(callbackCapture))
        } answers {
            callbackCapture.captured.onFinish(
                requestCapture.captured,
                ExpectedFactory.createValue(loadResult),
            )
            1L
        }

        sut.download(url)

        assertEquals(url, requestCapture.captured.url)
    }

    @Test
    fun `download - when ResourceLoadStatus AVAILABLE and BLOB data not empty should return result with BLOB`() =
        runBlockingTest {
            val url = "http://example.com/some-shield-asset"
            val blob = Fixtures.nonEmptyBlobData()
            val loadRequest = ResourceLoadRequest(url)
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(blob),
                ResourceLoadStatus.AVAILABLE,
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.download(url)

            assertTrue(blob.contentEquals(result.value))
        }

    @Test
    fun `download - when ResourceLoadStatus AVAILABLE but BLOB data is empty should return an Error`() =
        runBlockingTest {
            val expectedError = "No data available."
            val url = "http://example.com/some-shield-asset"
            val blob = Fixtures.emptyBlobData()
            val loadRequest = ResourceLoadRequest(url)
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(blob),
                ResourceLoadStatus.AVAILABLE,
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.download(url)

            assertEquals(expectedError, result.error)
        }

    @Test
    fun `download - when ResourceLoadStatus UNAUTHORIZED should return an Error`() =
        runBlockingTest {
            val expectedError = "Your token cannot access this resource."
            val url = "http://example.com/some-shield-asset"
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.UNAUTHORIZED,
            )
            givenResourceLoaderResponse(
                ResourceLoadRequest(url),
                ExpectedFactory.createValue(loadResult),
            )

            val result = sut.download(url)

            assertEquals(expectedError, result.error)
        }

    @Test
    fun `download - when ResourceLoadStatus NOT_FOUND should return an Error`() = runBlockingTest {
        val expectedError = "Resource is missing."
        val url = "http://example.com/some-shield-asset"
        val loadResult = Fixtures.resourceLoadResult(
            null,
            ResourceLoadStatus.NOT_FOUND,
        )
        givenResourceLoaderResponse(
            ResourceLoadRequest(url),
            ExpectedFactory.createValue(loadResult),
        )

        val result = sut.download(url)

        assertEquals(expectedError, result.error)
    }

    private fun givenResourceLoaderResponse(
        request: ResourceLoadRequest,
        response: Expected<ResourceLoadError, ResourceLoadResult>,
    ) {
        val loadCallbackSlot = slot<ResourceLoadCallback>()
        every { mockResourceLoader.load(any(), capture(loadCallbackSlot)) } answers {
            loadCallbackSlot.captured.onFinish(request, response)
            0L
        }
    }
}

object Fixtures {

    fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): DataRef = blob.toDataRef()
    }

    fun resourceLoadResult(
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

    fun emptyBlobData() = byteArrayOf()

    fun nonEmptyBlobData() = """{"turning-circle":{"width":126,"height":126}}""".encodeToByteArray()
}
