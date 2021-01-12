package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ExitNumberComponentNodeTest : BuilderTest<ExitNumberComponentNode,
    ExitNumberComponentNode.Builder>() {

    override fun getImplementationClass(): KClass<ExitNumberComponentNode> =
        ExitNumberComponentNode::class

    override fun getFilledUpBuilder(): ExitNumberComponentNode.Builder {
        return ExitNumberComponentNode.Builder()
            .text("exit-number")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
