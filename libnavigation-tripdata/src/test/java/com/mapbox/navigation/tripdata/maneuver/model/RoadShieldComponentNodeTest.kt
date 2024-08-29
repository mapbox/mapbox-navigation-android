package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class RoadShieldComponentNodeTest : BuilderTest<RoadShieldComponentNode,
    RoadShieldComponentNode.Builder,>() {

    override fun getImplementationClass(): KClass<RoadShieldComponentNode> =
        RoadShieldComponentNode::class

    override fun getFilledUpBuilder(): RoadShieldComponentNode.Builder {
        return RoadShieldComponentNode.Builder()
            .text("exit-number")
            .mapboxShield(
                MapboxShield
                    .builder()
                    .name("us-interstate")
                    .textColor("black")
                    .displayRef("880")
                    .baseUrl("https://mapbox.test.com/v1")
                    .build(),
            )
            .shieldUrl("https://api.mapbox.com/test_url")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
