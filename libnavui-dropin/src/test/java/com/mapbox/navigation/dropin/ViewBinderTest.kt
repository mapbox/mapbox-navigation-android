package com.mapbox.navigation.dropin

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
internal class ViewBinderTest {

    lateinit var sut: ViewBinder

    @Before
    fun setUp() {
        sut = ViewBinder()
    }

    @Test
    fun `applyCustomization should update NON NULL binders`() {
        val c = customization()

        sut.applyCustomization(c)

        assertEquals(c.speedLimitBinder, sut.speedLimit.value)
        assertEquals(c.maneuverBinder, sut.maneuver.value)
        assertEquals(c.roadNameBinder, sut.roadName.value)
        assertEquals(c.infoPanelTripProgressBinder, sut.infoPanelTripProgressBinder.value)
        assertEquals(c.infoPanelHeaderBinder, sut.infoPanelHeaderBinder.value)
        assertEquals(c.infoPanelContentBinder, sut.infoPanelContentBinder.value)
        assertEquals(c.actionButtonsBinder, sut.actionButtonsBinder.value)
    }

    @Test
    fun `applyCustomization should reset to default binders`() {
        sut.applyCustomization(customization())

        sut.applyCustomization(
            ViewBinderCustomization().apply {
                speedLimitBinder = UIBinder.USE_DEFAULT
                maneuverBinder = UIBinder.USE_DEFAULT
                roadNameBinder = UIBinder.USE_DEFAULT
                infoPanelTripProgressBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderBinder = UIBinder.USE_DEFAULT
                infoPanelContentBinder = UIBinder.USE_DEFAULT
                actionButtonsBinder = UIBinder.USE_DEFAULT
            }
        )

        assertTrue(sut.speedLimit.value == null)
        assertTrue(sut.maneuver.value == null)
        assertTrue(sut.roadName.value == null)
        assertTrue(sut.infoPanelTripProgressBinder.value == null)
        assertTrue(sut.infoPanelHeaderBinder.value == null)
        assertTrue(sut.infoPanelContentBinder.value == null)
        assertTrue(sut.actionButtonsBinder.value == null)
    }

    private fun customization() = ViewBinderCustomization().apply {
        speedLimitBinder = EmptyBinder()
        maneuverBinder = EmptyBinder()
        roadNameBinder = EmptyBinder()
        infoPanelTripProgressBinder = EmptyBinder()
        infoPanelHeaderBinder = EmptyBinder()
        infoPanelContentBinder = EmptyBinder()
        actionButtonsBinder = EmptyBinder()
    }
}
