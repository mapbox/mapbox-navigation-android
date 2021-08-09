package com.mapbox.navigation.ui.utils.internal

object CompareUtils {

    fun <T> areEqualContentsIgnoreOrder(first: Collection<T>, second: Collection<T>): Boolean {
        if (first !== second) {
            if (first.size != second.size) return false
            val areNotEqual = first.asSequence()
                .map { it in second }
                .contains(false)
            if (areNotEqual) return false
        }
        return true
    }
}
