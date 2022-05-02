package com.mapbox.androidauto.car.search

import android.text.SpannableString
import androidx.car.app.model.Row
import com.mapbox.examples.androidauto.R
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.search.result.SearchSuggestion
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class SearchScreenTest : MapboxRobolectricTestRunner() {

    private val searchCarContext: SearchCarContext = mockk {
        every { carContext } returns mockk {
            every { getString(R.string.car_search_no_results) } returns "No results"
        }
        every { carSearchEngine } returns mockk()
        every { distanceFormatter } returns mockk()
    }

    private val searchScreen = SearchScreen(searchCarContext)

    @Test
    fun `search suggestion create list row`() {
        val callbackSlot = mockSearch("starbucks")
        mockDistanceFormat(559.39, "0.3 mi")

        searchScreen.doSearch("starbucks")
        callbackSlot.captured(
            listOf(
                mockSearchSuggestion(
                    mockName = "Starbucks",
                    mockDistanceMeters = 559.39
                )
            )
        )

        val firstResult = searchScreen.itemList.items[0] as Row
        assertEquals("Starbucks", firstResult.title.toString())
        assertEquals("0.3 mi", firstResult.texts[0].toString())
    }

    @Test
    fun `empty search creates no results item`() {
        val callbackSlot = mockSearch("starbucks")

        searchScreen.doSearch("starbucks")
        callbackSlot.captured(emptyList())

        assertTrue(searchScreen.itemList.items.isEmpty())
        assertEquals("No results", searchScreen.itemList.noItemsMessage.toString())
    }

    private fun mockSearch(query: String): CapturingSlot<(List<SearchSuggestion>) -> Unit> {
        val callbackSlot = CapturingSlot<(List<SearchSuggestion>) -> Unit>()
        every {
            searchCarContext.carSearchEngine.search(query, capture(callbackSlot))
        } just Runs
        return callbackSlot
    }

    private fun mockSearchSuggestion(
        mockName: String = "Starbucks",
        mockDistanceMeters: Double? = 559.3991196008689
    ): SearchSuggestion = mockk {
        every { name } returns mockName
        every { distanceMeters } returns mockDistanceMeters
    }

    private fun mockDistanceFormat(
        value: Double = 559.3991196008689,
        formatted: String = "0.3 mi"
    ) {
        every {
            searchCarContext.distanceFormatter.formatDistance(value)
        } returns SpannableString.valueOf(formatted)
    }
}
