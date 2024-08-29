package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class DelimiterComponentNodeTest : BuilderTest<DelimiterComponentNode,
    DelimiterComponentNode.Builder,>() {

    override fun getImplementationClass(): KClass<DelimiterComponentNode> =
        DelimiterComponentNode::class

    override fun getFilledUpBuilder(): DelimiterComponentNode.Builder {
        return DelimiterComponentNode.Builder()
            .text("/")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
