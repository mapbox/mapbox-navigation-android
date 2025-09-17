package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KClass

class RoadObjectMatcherOptionsTest :
    BuilderTest<RoadObjectMatcherOptions, RoadObjectMatcherOptions.Builder>() {
    override fun getImplementationClass(): KClass<RoadObjectMatcherOptions> =
        RoadObjectMatcherOptions::class

    override fun getFilledUpBuilder(): RoadObjectMatcherOptions.Builder =
        RoadObjectMatcherOptions.Builder()
            .openLRMaxDistanceToNode(60.0)
            .matchingGraphType(newType = NavigationTileDataDomain.NAVIGATION_HD)

    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `validate default values`() {
        val roadObjectMatcherOptions = RoadObjectMatcherOptions.Builder().build()
        assertEquals(null, roadObjectMatcherOptions.openLRMaxDistanceToNode)
        assertEquals(
            NavigationTileDataDomain.NAVIGATION,
            roadObjectMatcherOptions.matchingGraphType,
        )
    }

    @Test
    fun `validate setting values`() {
        val testMaxDistance = 10.0
        val testMatchingGraphType = NavigationTileDataDomain.NAVIGATION
        val roadObjectMatcherOptions = RoadObjectMatcherOptions.Builder()
            .openLRMaxDistanceToNode(testMaxDistance)
            .matchingGraphType(testMatchingGraphType)
            .build()

        assertEquals(
            testMaxDistance,
            roadObjectMatcherOptions.openLRMaxDistanceToNode,
        )
        assertEquals(
            testMatchingGraphType,
            roadObjectMatcherOptions.matchingGraphType,
        )
    }
}
