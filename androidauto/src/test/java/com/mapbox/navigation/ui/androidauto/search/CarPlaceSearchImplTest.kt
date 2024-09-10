package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearchImpl
import com.mapbox.navigation.ui.androidauto.internal.search.CarSearchLocationProvider
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarPlaceSearchImplTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val options: MapboxCarOptions = mockk {
        every { carPlaceSearchOptions } returns mockk {
            every { accessToken } returns "pk.search-token"
        }
    }
    private val locationProvider: CarSearchLocationProvider = mockk(relaxed = true)
    private val searchEngine: SearchEngine = mockk()
    private val sut = CarPlaceSearchImpl(options, locationProvider)

    @Before
    fun setup() {
        mockkObject(SearchEngine)
        every {
            SearchEngine.createSearchEngineWithBuiltInDataProviders(
                any(),
                any(),
                any(),
                any(),
            )
        } returns searchEngine
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `search fails when service is not attached`() = coroutineRule.runBlockingTest {
        val result = sut.search("coffee")

        assertTrue(result.isFailure)
    }

    @Test
    fun `select fails when service is not attached`() = coroutineRule.runBlockingTest {
        val result = sut.select(mockk())

        assertTrue(result.isFailure)
    }

    @Test
    fun `onAttached will create attach the location provider`() {
        val mapboxNavigation = mockk<MapboxNavigation>()

        sut.onAttached(mapboxNavigation)

        verify { locationProvider.onAttached(mapboxNavigation) }
    }

    @Test
    fun `onDetached will detach the location provider`() {
        val mapboxNavigation = mockk<MapboxNavigation>()

        sut.onDetached(mapboxNavigation)

        verify { locationProvider.onDetached(mapboxNavigation) }
    }

    @Test
    fun `search returns success with expected parameters`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchSuggestionsCallback>()
        every {
            searchEngine.search(any(), any(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        sut.onAttached(mockk())
        val asyncResult = async { sut.search("coffee") }
        callbackSlot.captured.onSuggestions(listOf(), mockk())
        val result = asyncResult.await()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `search returns failure with callback fails`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchSuggestionsCallback>()
        every {
            searchEngine.search(any(), any(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        sut.onAttached(mockk())
        val asyncResult = async { sut.search("coffee") }
        callbackSlot.captured.onError(mockk(relaxed = true))
        val result = asyncResult.await()

        assertTrue(result.isFailure)
    }

    @Test
    fun `select returns success with expected parameters`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchSelectionCallback>()
        every {
            searchEngine.select(any(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        sut.onAttached(mockk())
        val asyncResult = async { sut.select(mockk()) }
        callbackSlot.captured.onResult(mockk(), mockk(), mockk())
        val result = asyncResult.await()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `select returns failure with callback fails`() = coroutineRule.runBlockingTest {
        val callbackSlot = slot<SearchSelectionCallback>()
        every {
            searchEngine.select(any(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        sut.onAttached(mockk())
        val asyncResult = async { sut.select(mockk()) }
        callbackSlot.captured.onError(mockk(relaxed = true))
        val result = asyncResult.await()

        assertTrue(result.isFailure)
    }
}
