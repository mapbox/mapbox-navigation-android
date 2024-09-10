package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ReachabilityInterface
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceDescription
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadOptions
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadProgressCallback
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadResultCallback
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.navigation.testing.toDataRef
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class DefaultResourceLoaderTest {

    private lateinit var mockTileStore: TileStore
    private lateinit var mockReachability: ReachabilityInterface
    private lateinit var sut: ResourceLoader

    @Before
    fun setUp() {
        mockTileStore = mockk<StubTileStore>(relaxed = true)
        mockReachability = mockk(relaxed = true) {
            every { isReachable } returns true
        }
        sut = DefaultResourceLoader(mockTileStore, mockReachability)
    }

    @Test
    fun `load - should call TileStore loadResource`() {
        val loadRequest = ResourceLoadRequest(
            "http://example.com/some-resource",
        ).apply {
            flags = ResourceLoadFlags.SKIP_DATA_LOADING
            networkRestriction = NetworkRestriction.DISALLOW_ALL
        }
        val descriptionCapture = slot<ResourceDescription>()
        val optionsCapture = slot<ResourceLoadOptions>()
        every {
            mockTileStore.loadResource(
                capture(descriptionCapture),
                capture(optionsCapture),
                ofType(ResourceLoadProgressCallback::class),
                ofType(ResourceLoadResultCallback::class),
            )
        } returns mockk()

        sut.load(loadRequest, mockk(relaxed = true))

        assertEquals(loadRequest.url, descriptionCapture.captured.url)
        assertEquals(TileDataDomain.NAVIGATION, descriptionCapture.captured.domain)
        assertEquals(loadRequest.flags, optionsCapture.captured.flags)
        assertEquals(loadRequest.networkRestriction, optionsCapture.captured.networkRestriction)
    }

    @Test
    fun `load - should notify ResourceLoadCallback`() {
        val callback = mockk<ResourceLoadCallback>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val loadProgress = ResourceLoadProgress(0, 0)
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND),
        )
        val progressCallbackCapture = slot<ResourceLoadProgressCallback>()
        val resultCallbackCapture = slot<ResourceLoadResultCallback>()
        every {
            mockTileStore.loadResource(
                any(),
                any(),
                capture(progressCallbackCapture),
                capture(resultCallbackCapture),
            )
        } answers {
            progressCallbackCapture.captured.run(loadProgress)
            progressCallbackCapture.captured.run(loadProgress) // simulate multiple calls
            resultCallbackCapture.captured.run(loadResult)
            stubCancelable()
        }

        sut.load(loadRequest, callback)

        verifyOrder {
            callback.onStart(loadRequest)
            callback.onProgress(loadRequest, loadProgress)
            callback.onProgress(loadRequest, loadProgress)
            callback.onFinish(loadRequest, loadResult)
        }
    }

    @Test
    fun `load - should force load from disk cache when network is not reachable`() {
        val callback = mockk<ResourceLoadCallback>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        every { mockReachability.isReachable } returns false
        sut.load(loadRequest, callback)

        val optionsCapture = slot<ResourceLoadOptions>()
        verify {
            mockTileStore.loadResource(
                ResourceDescription(TileDataDomain.NAVIGATION, loadRequest.url),
                capture(optionsCapture),
                any(),
                any(),
            )
        }

        assertEquals(ResourceLoadFlags.ACCEPT_EXPIRED, optionsCapture.captured.flags)
        assertEquals(NetworkRestriction.DISALLOW_ALL, optionsCapture.captured.networkRestriction)
    }

    @Test
    fun `cancel - should call Cancelable`() {
        val loadRequest = ResourceLoadRequest(
            "http://example.com/some-resource",
        ).apply {
            flags = ResourceLoadFlags.SKIP_DATA_LOADING
            networkRestriction = NetworkRestriction.DISALLOW_ALL
        }
        val cancelable = spyk(stubCancelable())
        every {
            mockTileStore.loadResource(any(), any(), any(), any())
        } returns cancelable

        val requestId = sut.load(loadRequest, mockk(relaxed = true))
        sut.cancel(requestId)

        verify { cancelable.cancel() }
    }

    @Test
    fun `ResourceLoadObserver - should notify all registered observers`() {
        val observer1 = mockk<ResourceLoadObserver>(relaxed = true)
        val observer2 = mockk<ResourceLoadObserver>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val loadProgress = ResourceLoadProgress(0, 0)
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND),
        )
        val progressCallbackCapture = slot<ResourceLoadProgressCallback>()
        val resultCallbackCapture = slot<ResourceLoadResultCallback>()
        every {
            mockTileStore.loadResource(
                any(),
                any(),
                capture(progressCallbackCapture),
                capture(resultCallbackCapture),
            )
        } answers {
            progressCallbackCapture.captured.run(loadProgress)
            progressCallbackCapture.captured.run(loadProgress) // simulate multiple calls
            resultCallbackCapture.captured.run(loadResult)
            stubCancelable()
        }

        sut.registerObserver(observer1)
        sut.registerObserver(observer2)
        sut.load(loadRequest, mockk(relaxed = true))

        verifyOrder {
            observer1.onStart(loadRequest)
            observer1.onProgress(loadRequest, loadProgress)
            observer1.onProgress(loadRequest, loadProgress)
            observer1.onFinish(loadRequest, loadResult)
        }
        verifyOrder {
            observer2.onStart(loadRequest)
            observer2.onProgress(loadRequest, loadProgress)
            observer2.onProgress(loadRequest, loadProgress)
            observer2.onFinish(loadRequest, loadResult)
        }
    }

    @Test
    fun `ResourceLoadObserver - should not throw ConcurrentModificationException when calling unregisterObserver`() {
        val observer = mockk<ResourceLoadObserver>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val loadProgress = ResourceLoadProgress(0, 0)
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND),
        )
        val progressCallbackCapture = slot<ResourceLoadProgressCallback>()
        val resultCallbackCapture = slot<ResourceLoadResultCallback>()
        every {
            mockTileStore.loadResource(
                any(),
                any(),
                capture(progressCallbackCapture),
                capture(resultCallbackCapture),
            )
        } answers {
            progressCallbackCapture.captured.run(loadProgress)
            progressCallbackCapture.captured.run(loadProgress) // simulate multiple calls
            resultCallbackCapture.captured.run(loadResult)
            stubCancelable()
        }

        sut.registerObserver(
            object : ResourceLoadObserver {
                override fun onStart(request: ResourceLoadRequest) = Unit
                override fun onProgress(
                    request: ResourceLoadRequest,
                    progress: ResourceLoadProgress,
                ) = Unit

                override fun onFinish(
                    request: ResourceLoadRequest,
                    result: Expected<ResourceLoadError, ResourceLoadResult>,
                ) {
                    // This observer immediately unregisters itself after receiving loader result
                    sut.unregisterObserver(this)
                }
            },
        )
        sut.registerObserver(observer)
        sut.load(loadRequest, mockk(relaxed = true))

        verifyOrder {
            observer.onStart(loadRequest)
            observer.onProgress(loadRequest, loadProgress)
            observer.onProgress(loadRequest, loadProgress)
            observer.onFinish(loadRequest, loadResult)
        }
    }
}

// Stub TileStore to avoid java.lang.UnsatisfiedLinkError
private class StubTileStore : TileStore(0) {
    override fun loadResource(
        description: ResourceDescription,
        options: ResourceLoadOptions,
        progressCallback: ResourceLoadProgressCallback,
        resultCallback: ResourceLoadResultCallback,
    ) = stubCancelable()
}

// stub Cancelable to avoid java.lang.UnsatisfiedLinkError
private fun stubCancelable() = object : Cancelable {
    override fun cancel() {
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
}
