package com.mapbox.navigation.dropin

import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelTripProgressBinder
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewBinder
import com.mapbox.navigation.dropin.component.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewBinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class NavigationUIBindersTest {

    lateinit var sut: NavigationUIBinders

    @Before
    fun setUp() {
        sut = NavigationUIBinders()
    }

    @Test
    fun `applyCustomization should update NON NULL binders`() {
        val c = customization()

        sut.applyCustomization(c)

        assertEquals(c.speedLimit, sut.speedLimit.value)
        assertEquals(c.maneuver, sut.maneuver.value)
        assertEquals(c.roadName, sut.roadName.value)
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
                speedLimit = UIBinder.USE_DEFAULT
                maneuver = UIBinder.USE_DEFAULT
                roadName = UIBinder.USE_DEFAULT
                infoPanelTripProgressBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderBinder = UIBinder.USE_DEFAULT
                infoPanelContentBinder = UIBinder.USE_DEFAULT
                actionButtonsBinder = UIBinder.USE_DEFAULT
            }
        )

        assertTrue(sut.speedLimit.value is SpeedLimitViewBinder)
        assertTrue(sut.maneuver.value is ManeuverViewBinder)
        assertTrue(sut.roadName.value is RoadNameViewBinder)
        assertTrue(sut.infoPanelTripProgressBinder.value is InfoPanelTripProgressBinder)
        assertTrue(sut.infoPanelHeaderBinder.value == null)
        assertTrue(sut.infoPanelContentBinder.value == null)
        assertTrue(sut.actionButtonsBinder.value == null)
    }

    private fun customization() = ViewBinderCustomization().apply {
        speedLimit = EmptyBinder()
        maneuver = EmptyBinder()
        roadName = EmptyBinder()
        infoPanelTripProgressBinder = EmptyBinder()
        infoPanelHeaderBinder = EmptyBinder()
        infoPanelContentBinder = EmptyBinder()
        actionButtonsBinder = EmptyBinder()
    }
}
