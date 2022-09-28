package com.mapbox.navigation.testing

object TestingUtils {
    fun <T> listElementsAreEqual(
        first: List<T>,
        second: List<T>,
        equalityFun: (T, T) -> Boolean
    ): Boolean {
        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (x, y) ->
            equalityFun(x, y)
        }
    }
}
