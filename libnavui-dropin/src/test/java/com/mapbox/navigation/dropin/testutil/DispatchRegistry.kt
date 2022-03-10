package com.mapbox.navigation.dropin.testutil

import org.junit.Assert
import org.junit.Ignore

@Ignore
internal class DispatchRegistry {
    val actions = mutableListOf<Any>()

    operator fun invoke(a: Any) {
        actions.add(a)
    }

    fun verifyDispatched(a: Any) {
        Assert.assertTrue(
            "expected ${a::class.java.simpleName} dispatch",
            actions.contains(a)
        )
    }
}
