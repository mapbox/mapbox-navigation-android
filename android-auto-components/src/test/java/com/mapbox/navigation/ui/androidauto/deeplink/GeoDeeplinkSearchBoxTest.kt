package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.testing.LoggingFrontendTestRule
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GeoDeeplinkSearchBoxTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val searchEngine: SearchEngine = mockk()
    private lateinit var deeplinkSearchBox: GeoDeeplinkSearchBox

    @Before
    fun setup() {
        deeplinkSearchBox = GeoDeeplinkSearchBox(searchEngine)
    }

    @Test
    fun `requestPlaces with point calls search with ReverseGeoOptions`() =
        coroutineRule.runBlockingTest {
            val point = Point.fromLngLat(-121.0, 45.0)
            val geoDeeplink = mockk<GeoDeeplink> {
                every { this@mockk.point } returns point
                every { placeQuery } returns null
            }
            val optionsSlot = slot<ReverseGeoOptions>()
            val callbackSlot = slot<SearchCallback>()
            every {
                searchEngine.search(capture(optionsSlot), capture(callbackSlot))
            } returns mockk<AsyncOperationTask>(relaxed = true)

            val asyncResult = async {
                deeplinkSearchBox.requestPlaces(geoDeeplink, Point.fromLngLat(-122.0, 47.0))
            }
            callbackSlot.captured.onResults(emptyList(), mockk<ResponseInfo>())
            asyncResult.await()

            assertEquals(point, optionsSlot.captured.center)
        }

    @Test
    fun `requestPlaces with placeQuery calls forward with proximity`() =
        coroutineRule.runBlockingTest {
            val origin = Point.fromLngLat(-122.0, 47.0)
            val geoDeeplink = mockk<GeoDeeplink> {
                every { point } returns null
                every { placeQuery } returns "coffee"
            }
            val querySlot = slot<String>()
            val optionsSlot = slot<ForwardSearchOptions>()
            val callbackSlot = slot<SearchCallback>()
            every {
                searchEngine.forward(
                    capture(querySlot),
                    capture(optionsSlot),
                    capture(callbackSlot),
                )
            } returns mockk<AsyncOperationTask>(relaxed = true)

            val asyncResult = async {
                deeplinkSearchBox.requestPlaces(geoDeeplink, origin)
            }
            callbackSlot.captured.onResults(emptyList(), mockk<ResponseInfo>())
            asyncResult.await()

            assertEquals("coffee", querySlot.captured)
            assertEquals(origin, optionsSlot.captured.proximity)
        }

    @Test
    fun `requestPlaces returns results on success`() = coroutineRule.runBlockingTest {
        val point = Point.fromLngLat(-121.0, 45.0)
        val geoDeeplink = mockk<GeoDeeplink> {
            every { this@mockk.point } returns point
            every { placeQuery } returns null
        }
        val searchResults = listOf(mockk<SearchResult>(relaxed = true), mockk(relaxed = true))
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.search(any<ReverseGeoOptions>(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val asyncResult = async {
            deeplinkSearchBox.requestPlaces(geoDeeplink, Point.fromLngLat(-122.0, 47.0))
        }
        callbackSlot.captured.onResults(searchResults, mockk<ResponseInfo>())
        val result = asyncResult.await()

        assertEquals(searchResults, result)
    }

    @Test
    fun `requestPlaces returns null on error`() = coroutineRule.runBlockingTest {
        val point = Point.fromLngLat(-121.0, 45.0)
        val geoDeeplink = mockk<GeoDeeplink> {
            every { this@mockk.point } returns point
            every { placeQuery } returns null
        }
        val callbackSlot = slot<SearchCallback>()
        every {
            searchEngine.search(any<ReverseGeoOptions>(), capture(callbackSlot))
        } returns mockk<AsyncOperationTask>(relaxed = true)

        val asyncResult = async {
            deeplinkSearchBox.requestPlaces(geoDeeplink, Point.fromLngLat(-122.0, 47.0))
        }
        callbackSlot.captured.onError(RuntimeException("search failed"))
        val result = asyncResult.await()

        assertNull(result)
    }

    @Test
    fun `cancel cancels the underlying task`() = coroutineRule.runBlockingTest {
        val task = mockk<AsyncOperationTask>(relaxed = true)
        val point = Point.fromLngLat(-121.0, 45.0)
        val geoDeeplink = mockk<GeoDeeplink> {
            every { this@mockk.point } returns point
            every { placeQuery } returns null
        }
        every {
            searchEngine.search(any<ReverseGeoOptions>(), any<SearchCallback>())
        } returns task

        async { deeplinkSearchBox.requestPlaces(geoDeeplink, Point.fromLngLat(-122.0, 47.0)) }
        deeplinkSearchBox.cancel()

        verify { task.cancel() }
    }
}
