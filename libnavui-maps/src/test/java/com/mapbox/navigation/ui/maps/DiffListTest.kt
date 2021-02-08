package com.mapbox.navigation.ui.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class DiffListTest {
    @Test
    fun diffAdded() {
        val mapSources = mutableListOf<String>()
        mapSources.add("A")
        mapSources.add("B")
        mapSources.add("C")
        mapSources.add("D")
        val attachedMapSources = mutableListOf<String>()
        attachedMapSources.add("B")
        attachedMapSources.add("C")
        attachedMapSources.add("E")
        val expectedAdded = mutableListOf<String>()
        expectedAdded.add("A")
        expectedAdded.add("D")

        val differenceAdded = mapSources.filterNot { attachedMapSources.contains(it) }

        assertEquals(expectedAdded, differenceAdded)
    }

    @Test
    fun diffRemoved() {
        val mapSources = mutableListOf<String>()
        mapSources.add("A")
        mapSources.add("B")
        mapSources.add("C")
        mapSources.add("D")
        val attachedMapSources = mutableListOf<String>()
        attachedMapSources.add("B")
        attachedMapSources.add("C")
        attachedMapSources.add("E")
        val expectedRemoved = mutableListOf<String>()
        expectedRemoved.add("E")

        val differenceRemoved = attachedMapSources.filterNot { mapSources.contains(it) }

        assertEquals(expectedRemoved, differenceRemoved)
    }
}
