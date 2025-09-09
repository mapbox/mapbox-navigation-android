package com.mapbox.navigation.tripdata.shield.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ShieldFontConfigTest : BuilderTest<ShieldFontConfig, ShieldFontConfig.Builder>() {

    override fun getImplementationClass(): KClass<ShieldFontConfig> = ShieldFontConfig::class

    override fun getFilledUpBuilder(): ShieldFontConfig.Builder {
        return ShieldFontConfig.Builder("CustomFont")
            .fontWeight(ShieldFontConfig.FontWeight.BOLD)
            .fontStyle("italic")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fontStyle throws when invalid value is passed`() {
        ShieldFontConfig.Builder("CustomFont")
            .fontStyle("bold-italic")
    }

    @Test
    fun `fontWeight accepts all valid values`() {
        val validWeights = listOf(
            ShieldFontConfig.FontWeight.THIN,
            ShieldFontConfig.FontWeight.EXTRA_LIGHT,
            ShieldFontConfig.FontWeight.LIGHT,
            ShieldFontConfig.FontWeight.NORMAL,
            ShieldFontConfig.FontWeight.MEDIUM,
            ShieldFontConfig.FontWeight.SEMI_BOLD,
            ShieldFontConfig.FontWeight.BOLD,
            ShieldFontConfig.FontWeight.EXTRA_BOLD,
            ShieldFontConfig.FontWeight.BLACK,
        )
        validWeights.forEach { weight ->
            ShieldFontConfig.Builder("CustomFont").fontWeight(weight).build()
        }
    }
}
