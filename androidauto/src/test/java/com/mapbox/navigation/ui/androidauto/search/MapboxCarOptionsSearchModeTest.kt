package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxCarOptionsSearchModeTest {

    @Test
    fun `default searchMode is Legacy`() {
        val options = MapboxCarOptions()

        assertSame(CarSearchMode.Legacy, options.searchMode)
    }

    @Test
    fun `searchMode switches to SearchBox via customization`() {
        val options = MapboxCarOptions()
        options.applyCustomization(
            MapboxCarOptions.Customization().apply {
                searchMode = CarSearchMode.SearchBox
            },
        )

        assertSame(CarSearchMode.SearchBox, options.searchMode)
    }

    @Test
    fun `searchMode switches back to Legacy via customization`() {
        val options = MapboxCarOptions()
        options.applyCustomization(
            MapboxCarOptions.Customization().apply { searchMode = CarSearchMode.SearchBox },
        )
        options.applyCustomization(
            MapboxCarOptions.Customization().apply { searchMode = CarSearchMode.Legacy },
        )

        assertSame(CarSearchMode.Legacy, options.searchMode)
    }

    @Test
    fun `searchMode is unchanged when customization does not set it`() {
        val options = MapboxCarOptions()
        options.applyCustomization(MapboxCarOptions.Customization())

        assertSame(CarSearchMode.Legacy, options.searchMode)
    }
}
