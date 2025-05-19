package com.mapbox.navigation.testing.utils.assertions

import org.junit.Assert.assertTrue

inline fun <reified T> assertIs(value: Any?): T {
    assertTrue(
        "Expected value of type: ${T::class.java.name}, but was $value",
        value is T
    )
    return value as T
}
