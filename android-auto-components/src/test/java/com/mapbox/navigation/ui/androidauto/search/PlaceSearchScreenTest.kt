package com.mapbox.navigation.ui.androidauto.search

import android.text.SpannableString
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearch
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.search.result.SearchSuggestion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val carPlaceSearch: CarPlaceSearch = mockk(relaxed = true)

    private val searchCarContext: SearchCarContext = mockk {
        every { mapboxCarContext } returns mockk(relaxed = true)
        every { carContext } returns mockk {
            every { getString(R.string.car_search_no_results) } returns "No results"
            every {
                getString(R.string.car_search_error)
            } returns "Something went wrong. Try again."
        }
        every { carPlaceSearch } returns this@PlaceSearchScreenTest.carPlaceSearch
        every { routePreviewRequest } returns mockk()
    }

    private val placeSearchScreen = PlaceSearchScreen(searchCarContext)

    @Before
    fun setup() {
        mockkStatic(CarDistanceFormatter::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun mockSuggestion(suggestionName: String, distance: Double) = mockk<SearchSuggestion> {
        every { name } returns suggestionName
        every { distanceMeters } returns distance
    }

    @Test
    fun `initial state is empty`() {
        assertTrue(placeSearchScreen.uiState is SearchUiState.NoItems)

        val template = placeSearchScreen.onGetTemplate() as SearchTemplate
        assertEquals("No results", template.itemList.noItemsMessage.toString())
    }

    @Test
    fun `search suggestion create list row`() = coroutineRule.runBlockingTest {
        coEvery {
            carPlaceSearch.search("starbucks")
        } returns Result.success(listOf(mockSuggestion("Starbucks", 559.39)))

        every {
            CarDistanceFormatter.formatDistance(559.39)
        } returns SpannableString.valueOf("0.3 mi")

        placeSearchScreen.doSearch("starbucks", debounce = false)

        val template = placeSearchScreen.onGetTemplate() as SearchTemplate
        val firstResult = template.itemList.items[0] as Row
        assertEquals("Starbucks", firstResult.title.toString())
        assertEquals("0.3 mi", firstResult.texts[0].toString())
    }

    @Test
    fun `empty search creates no results state`() = coroutineRule.runBlockingTest {
        coEvery {
            carPlaceSearch.search("starbucks")
        } returns Result.success(listOf())

        placeSearchScreen.doSearch("starbucks", debounce = false)

        val state = placeSearchScreen.uiState
        assertTrue(state is SearchUiState.NoItems)
        assertEquals(R.string.car_search_no_results, (state as SearchUiState.NoItems).messageRes)

        val template = placeSearchScreen.onGetTemplate() as SearchTemplate
        assertEquals("No results", template.itemList.noItemsMessage.toString())
    }

    @Test
    fun `search failure surfaces as a distinct error state, not empty`() =
        coroutineRule.runBlockingTest {
            coEvery {
                carPlaceSearch.search("starbucks")
            } returns Result.failure(IllegalStateException("boom"))

            placeSearchScreen.doSearch("starbucks", debounce = false)

            assertTrue(placeSearchScreen.uiState is SearchUiState.Error)

            val template = placeSearchScreen.onGetTemplate() as SearchTemplate
            assertEquals(
                "Something went wrong. Try again.",
                template.itemList.noItemsMessage.toString(),
            )
        }

    @Test
    fun `uiState is Loading while a search is in flight`() = coroutineRule.runBlockingTest {
        val response = CompletableDeferred<Result<List<SearchSuggestion>>>()
        coEvery {
            carPlaceSearch.search("starbucks")
        } coAnswers { response.await() }

        placeSearchScreen.doSearch("starbucks", debounce = false)

        assertEquals(SearchUiState.Loading, placeSearchScreen.uiState)
        val template = placeSearchScreen.onGetTemplate() as SearchTemplate
        assertTrue(template.isLoading)

        response.complete(Result.success(emptyList()))

        assertTrue(placeSearchScreen.uiState is SearchUiState.NoItems)
    }

    @Test
    fun `typing debounces before searching`() = coroutineRule.runBlockingTest {
        coEvery {
            carPlaceSearch.search(any())
        } returns Result.success(emptyList())

        placeSearchScreen.doSearch("sta", debounce = true)
        testScheduler.advanceTimeBy(100L)
        placeSearchScreen.doSearch("star", debounce = true)
        testScheduler.advanceTimeBy(100L)
        placeSearchScreen.doSearch("starbucks", debounce = true)

        coVerify(exactly = 0) { carPlaceSearch.search(any()) }

        testScheduler.advanceTimeBy(400L)

        coVerify(exactly = 1) { carPlaceSearch.search("starbucks") }
        coVerify(exactly = 0) { carPlaceSearch.search("sta") }
        coVerify(exactly = 0) { carPlaceSearch.search("star") }
    }

    @Test
    fun `a newer keystroke cancels a debounced search already waiting on the network`() =
        coroutineRule.runBlockingTest {
            val staleResponse = CompletableDeferred<Result<List<SearchSuggestion>>>()
            coEvery {
                carPlaceSearch.search("sta")
            } coAnswers { staleResponse.await() }
            coEvery {
                carPlaceSearch.search("starbucks")
            } returns Result.success(emptyList())

            placeSearchScreen.doSearch("sta", debounce = true)
            // past the debounce delay: "sta"'s search is now genuinely in flight, awaiting the
            // network, when the next keystroke arrives.
            testScheduler.advanceTimeBy(400L)

            placeSearchScreen.doSearch("starbucks", debounce = true)
            testScheduler.advanceTimeBy(400L)

            coVerify(exactly = 1) { carPlaceSearch.search("starbucks") }
            assertTrue(placeSearchScreen.uiState is SearchUiState.NoItems)

            // the cancelled "sta" request finally resolving must not clobber "starbucks"'s result.
            staleResponse.complete(Result.success(listOf(mockSuggestion("Stale", 1.0))))
            assertTrue(placeSearchScreen.uiState is SearchUiState.NoItems)
        }

    @Test
    fun `submitting a query searches immediately without waiting for the debounce delay`() =
        coroutineRule.runBlockingTest {
            coEvery {
                carPlaceSearch.search("starbucks")
            } returns Result.success(emptyList())

            placeSearchScreen.doSearch("starbucks", debounce = false)

            coVerify(exactly = 1) { carPlaceSearch.search("starbucks") }
        }

    @Test
    fun `a newer query cancels the previous one and wins even if the old one resolves later`() =
        coroutineRule.runBlockingTest {
            val staleResponse = CompletableDeferred<Result<List<SearchSuggestion>>>()
            val freshSuggestion = mockSuggestion("New Place", 200.0)

            coEvery {
                carPlaceSearch.search("old")
            } coAnswers { staleResponse.await() }
            coEvery {
                carPlaceSearch.search("new")
            } returns Result.success(listOf(freshSuggestion))

            placeSearchScreen.doSearch("old", debounce = false)
            placeSearchScreen.doSearch("new", debounce = false)

            val stateAfterNew = placeSearchScreen.uiState
            assertTrue(stateAfterNew is SearchUiState.Content)
            assertEquals(
                freshSuggestion,
                (stateAfterNew as SearchUiState.Content).suggestions.first(),
            )

            // the stale request for "old" finally resolves after "new" already won; it must be
            // discarded rather than overwriting the current state.
            staleResponse.complete(Result.success(listOf(mockSuggestion("Old Place", 100.0))))

            assertEquals(stateAfterNew, placeSearchScreen.uiState)
        }

    @Test
    fun `cancelling a superseded query never surfaces as an error or empty result`() =
        coroutineRule.runBlockingTest {
            val staleResponse = CompletableDeferred<Result<List<SearchSuggestion>>>()

            coEvery {
                carPlaceSearch.search("old")
            } coAnswers { staleResponse.await() }
            coEvery {
                carPlaceSearch.search("new")
            } returns Result.success(emptyList())

            placeSearchScreen.doSearch("old", debounce = false)
            placeSearchScreen.doSearch("new", debounce = false)

            val stateAfterNew = placeSearchScreen.uiState

            // if the superseded "old" request had been allowed to complete, it would have failed;
            // completing it now must not turn the already-settled state into an error.
            staleResponse.completeExceptionally(IllegalStateException("boom"))

            assertEquals(stateAfterNew, placeSearchScreen.uiState)
            assertTrue(placeSearchScreen.uiState !is SearchUiState.Error)
        }
}
