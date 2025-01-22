package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class TextComponentNodeTest : BuilderTest<TextComponentNode,
    TextComponentNode.Builder,>() {

    override fun getImplementationClass(): KClass<TextComponentNode> =
        TextComponentNode::class

    override fun getFilledUpBuilder(): TextComponentNode.Builder {
        return TextComponentNode.Builder()
            .text("exit-number")
            .abbr("mapbox")
            .abbrPriority(4)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
