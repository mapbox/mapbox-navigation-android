package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ReachabilityInterface
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceDescription
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadErrorType
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadOptions
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadProgressCallback
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadResultCallback
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            "http://example.com/some-resource"
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
                ofType(ResourceLoadResultCallback::class)
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
        val data = Fixtures.resourceData(byteArrayOf(1))
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(data, ResourceLoadStatus.AVAILABLE)
        )
        val progressCallbackCapture = slot<ResourceLoadProgressCallback>()
        val resultCallbackCapture = slot<ResourceLoadResultCallback>()
        every {
            mockTileStore.loadResource(
                any(),
                any(),
                capture(progressCallbackCapture),
                capture(resultCallbackCapture)
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
    fun `load - should returned expired cached resource if network is not reachable`() {
        every { mockReachability.isReachable } returns false
        val callback = mockk<ResourceLoadCallback>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val data = Fixtures.resourceData(byteArrayOf(1))
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(data, ResourceLoadStatus.AVAILABLE)
        )
        given(
            tileStoreResult = loadResult
        )

        sut.load(loadRequest, callback)

        verifyOrder {
            callback.onStart(loadRequest)
            callback.onFinish(loadRequest, loadResult)
        }
    }

    @Test
    fun `load - should fail when network is not reachable and cached resource is not available`() {
        every { mockReachability.isReachable } returns false
        val callback = mockk<ResourceLoadCallback>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val loadResult = ExpectedFactory.createError<ResourceLoadError, ResourceLoadResult>(
            ResourceLoadError(ResourceLoadErrorType.UNSATISFIED, "error", 0L)
        )
        given(
            tileStoreResult = loadResult
        )

        sut.load(loadRequest, callback)

        val errorCapture = slot<Expected<ResourceLoadError, ResourceLoadResult>>()
        verifyOrder {
            callback.onStart(loadRequest)
            callback.onFinish(loadRequest, capture(errorCapture))
        }
        assertTrue(errorCapture.captured.isError)
    }

    // @Test
    // fun `load - should NOT call TileStore if request requires network and network is not reachable`() {
    //     val callback = mockk<ResourceLoadCallback>(relaxed = true)
    //     val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
    //     every { mockReachability.isReachable } returns false
    //     sut.load(loadRequest, callback)
    //
    //     verify(exactly = 0) { mockTileStore.loadResource(any(), any(), any(), any()) }
    // }

    @Test
    fun `cancel - should call Cancelable`() {
        val loadRequest = ResourceLoadRequest(
            "http://example.com/some-resource"
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
        val loadProgress = listOf(
            ResourceLoadProgress(0, 10),
            ResourceLoadProgress(1, 10)
        )
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND)
        )
        given(
            tileStoreProgress = loadProgress,
            tileStoreResult = loadResult
        )

        sut.registerObserver(observer1)
        sut.registerObserver(observer2)
        sut.load(loadRequest, mockk(relaxed = true))

        verifyOrder {
            observer1.onStart(loadRequest)
            observer1.onProgress(loadRequest, loadProgress[0])
            observer1.onProgress(loadRequest, loadProgress[1])
            observer1.onFinish(loadRequest, loadResult)
        }
        verifyOrder {
            observer2.onStart(loadRequest)
            observer2.onProgress(loadRequest, loadProgress[0])
            observer2.onProgress(loadRequest, loadProgress[1])
            observer2.onFinish(loadRequest, loadResult)
        }
    }

    @Test
    fun `ResourceLoadObserver - should not throw ConcurrentModificationException when calling unregisterObserver`() {
        val observer = mockk<ResourceLoadObserver>(relaxed = true)
        val loadRequest = ResourceLoadRequest("http://example.com/some-resource")
        val loadProgress = listOf(
            ResourceLoadProgress(0, 10),
            ResourceLoadProgress(1, 10)
        )
        val loadResult = ExpectedFactory.createValue<ResourceLoadError, ResourceLoadResult>(
            Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND)
        )
        given(
            tileStoreProgress = loadProgress,
            tileStoreResult = loadResult
        )

        sut.registerObserver(object : ResourceLoadObserver {
            override fun onStart(request: ResourceLoadRequest) = Unit
            override fun onProgress(request: ResourceLoadRequest, progress: ResourceLoadProgress) =
                Unit

            override fun onFinish(
                request: ResourceLoadRequest,
                result: Expected<ResourceLoadError, ResourceLoadResult>
            ) {
                // This observer immediately unregisters itself after receiving loader result
                sut.unregisterObserver(this)
            }
        })
        sut.registerObserver(observer)
        sut.load(loadRequest, mockk(relaxed = true))

        verifyOrder {
            observer.onStart(loadRequest)
            observer.onProgress(loadRequest, loadProgress[0])
            observer.onProgress(loadRequest, loadProgress[1])
            observer.onFinish(loadRequest, loadResult)
        }
    }

    private fun given(
        tileStoreResult: Expected<ResourceLoadError, ResourceLoadResult>,
        tileStoreProgress: List<ResourceLoadProgress> = emptyList()
    ) {
        val progressCallbackCapture = slot<ResourceLoadProgressCallback>()
        val resultCallbackCapture = slot<ResourceLoadResultCallback>()
        every {
            mockTileStore.loadResource(
                any(),
                any(),
                capture(progressCallbackCapture),
                capture(resultCallbackCapture)
            )
        } answers {
            tileStoreProgress.forEach { progressCallbackCapture.captured.run(it) }
            resultCallbackCapture.captured.run(tileStoreResult)
            stubCancelable()
        }
    }
}

// Stub TileStore to avoid java.lang.UnsatisfiedLinkError
private class StubTileStore : TileStore(0) {
    override fun loadResource(
        description: ResourceDescription,
        options: ResourceLoadOptions,
        progressCallback: ResourceLoadProgressCallback,
        resultCallback: ResourceLoadResultCallback
    ) = stubCancelable()
}

// stub Cancelable to avoid java.lang.UnsatisfiedLinkError
private fun stubCancelable() = object : Cancelable(0) {
    override fun cancel() = Unit
}

object Fixtures {

    fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): ByteArray = blob
    }

    fun resourceLoadResult(
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
