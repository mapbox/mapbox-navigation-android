package com.mapbox.navigation.ui.maps.route.line.model

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.lang.RuntimeException

class RouteLineErrorTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineError(
            "foobar",
            RuntimeException(),
        )

        val result = original.toMutableValue()

        assertEquals(original.errorMessage, result.errorMessage)
        assertEquals(original.throwable, result.throwable)
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineError(
            "foobar",
            RuntimeException(),
        )
        val replacementMessage = "doodle"
        val replacementThrowable = RuntimeException()
        val mutable = original.toMutableValue()

        mutable.errorMessage = replacementMessage
        mutable.throwable = replacementThrowable
        val result = mutable.toImmutableValue()

        assertEquals(replacementMessage, result.errorMessage)
        assertEquals(replacementThrowable, result.throwable)
    }
}
