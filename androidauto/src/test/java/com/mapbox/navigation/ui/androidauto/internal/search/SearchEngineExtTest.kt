package com.mapbox.navigation.ui.androidauto.internal.search

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.search.ForwardSearchOptions
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.result.SearchResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SearchEngineExtTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val searchEngine: SearchEngine = mockk()

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `forward returns success when onResults is called`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.forward(any(), any(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val asyncResult = async {
            searchEngine.forward("coffee", ForwardSearchOptions.Builder().build())
        }

        val mockResults = listOf<SearchResult>(mockk(relaxed = true))
        callbackSlot.captured.onResults(mockResults, mockk<ResponseInfo>())

        val result = asyncResult.await()
        assertTrue(result.isSuccess)
        assertEquals(mockResults, result.getOrNull())
    }

    @Test
    fun `forward returns failure when onError is called`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.forward(any(), any(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val asyncResult = async {
            searchEngine.forward("coffee", ForwardSearchOptions.Builder().build())
        }
        callbackSlot.captured.onError(RuntimeException("forward error"))
        val result = asyncResult.await()

        assertTrue(result.isFailure)
    }

    @Test
    fun `forward cancels task when coroutine is cancelled`() = coroutineRule.runBlockingTest {
        val task = mockk<AsyncOperationTask>(relaxed = true)
        every {
            searchEngine.forward(any(), any(), any<SearchCallback>())
        } returns task

        val job = async {
            searchEngine.forward("coffee", ForwardSearchOptions.Builder().build())
        }
        job.cancel()

        verify { task.cancel() }
    }

    @Test
    fun `search returns success when onResults is called`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.search(any<ReverseGeoOptions>(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val options = ReverseGeoOptions(center = Point.fromLngLat(-121.0, 45.0))
        val asyncResult = async {
            searchEngine.search(options)
        }
        val mockResults = listOf<SearchResult>(mockk(relaxed = true))
        callbackSlot.captured.onResults(mockResults, mockk<ResponseInfo>())

        val result = asyncResult.await()
        assertTrue(result.isSuccess)
        assertEquals(mockResults, result.getOrNull())
    }

    @Test
    fun `search returns failure when onError is called`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.search(any<ReverseGeoOptions>(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val options = ReverseGeoOptions(center = Point.fromLngLat(-121.0, 45.0))
        val asyncResult = async {
            searchEngine.search(options)
        }
        callbackSlot.captured.onError(RuntimeException("search error"))
        val result = asyncResult.await()

        assertTrue(result.isFailure)
    }

    @Test
    fun `search cancels task when coroutine is cancelled`() = coroutineRule.runBlockingTest {
        val task = mockk<AsyncOperationTask>(relaxed = true)
        every {
            searchEngine.search(any<ReverseGeoOptions>(), any<SearchCallback>())
        } returns task

        val options = ReverseGeoOptions(center = Point.fromLngLat(-121.0, 45.0))
        val job = async {
            searchEngine.search(options)
        }
        job.cancel()

        verify { task.cancel() }
    }
}
