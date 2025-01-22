package com.mapbox.navigation.ui.maps.building.model

import android.graphics.Color
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class BuildingHighlightOptionsTest :
    BuilderTest<MapboxBuildingHighlightOptions, MapboxBuildingHighlightOptions.Builder>() {
    override fun getImplementationClass(): KClass<MapboxBuildingHighlightOptions> =
        MapboxBuildingHighlightOptions::class

    override fun getFilledUpBuilder() = MapboxBuildingHighlightOptions.Builder()
        .fillExtrusionColor(Color.GREEN)
        .fillExtrusionOpacity(1.0)

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
