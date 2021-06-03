package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class RoadShieldComponentNodeTest : BuilderTest<RoadShieldComponentNode,
    RoadShieldComponentNode.Builder>() {

    override fun getImplementationClass(): KClass<RoadShieldComponentNode> =
        RoadShieldComponentNode::class

    override fun getFilledUpBuilder(): RoadShieldComponentNode.Builder {
        return RoadShieldComponentNode.Builder()
            .text("exit-number")
            .shieldUrl("https://api.mapbox.com/test_url")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
