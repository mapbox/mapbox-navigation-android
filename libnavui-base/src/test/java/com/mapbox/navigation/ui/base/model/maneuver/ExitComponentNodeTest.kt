package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ExitComponentNodeTest : BuilderTest<ExitComponentNode,
    ExitComponentNode.Builder>() {

    override fun getImplementationClass(): KClass<ExitComponentNode> =
        ExitComponentNode::class

    override fun getFilledUpBuilder(): ExitComponentNode.Builder {
        return ExitComponentNode.Builder()
            .text("exit")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
