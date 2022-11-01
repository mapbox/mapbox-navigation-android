package com.mapbox.androidauto.internal.car.search

import android.text.SpannableString
import androidx.car.app.model.Row
import com.mapbox.androidauto.R
import com.mapbox.androidauto.navigation.CarDistanceFormatter
import com.mapbox.androidauto.search.PlaceSearchScreen
import com.mapbox.androidauto.search.SearchCarContext
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val searchCarContext: SearchCarContext = mockk {
        every { mapboxCarContext } returns mockk()
        every { carContext } returns mockk {
            every { getString(R.string.car_search_no_results) } returns "No results"
        }
        every { carPlaceSearch } returns mockk(relaxed = true)
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

    @Test
    fun `initial results are empty`() {
        assertNotNull(placeSearchScreen.itemList.noItemsMessage)
    }

    @Test
    fun `search suggestion create list row`() = coroutineRule.runBlockingTest {
        coEvery {
            searchCarContext.carPlaceSearch.search("starbucks")
        } returns Result.success(
            listOf(
                mockk {
                    every { name } returns "Starbucks"
                    every { distanceMeters } returns 559.39
                }
            )
        )

        every {
            CarDistanceFormatter.formatDistance(559.39)
        } returns SpannableString.valueOf("0.3 mi")

        placeSearchScreen.doSearch("starbucks")

        val firstResult = placeSearchScreen.itemList.items[0] as Row
        assertEquals("Starbucks", firstResult.title.toString())
        assertEquals("0.3 mi", firstResult.texts[0].toString())
    }

    @Test
    fun `empty search creates no results item`() {
        coEvery {
            searchCarContext.carPlaceSearch.search("starbucks")
        } returns Result.success(listOf())

        placeSearchScreen.doSearch("starbucks")

        assertTrue(placeSearchScreen.itemList.items.isEmpty())
        assertEquals("No results", placeSearchScreen.itemList.noItemsMessage.toString())
    }
}
