package com.mapbox.navigation.dropin.navigationview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription.Position.END
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription.Position.START
import com.mapbox.navigation.dropin.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.infopanel.MapboxInfoPanelBinder
import com.mapbox.navigation.dropin.map.MapViewBinder
import com.mapbox.navigation.dropin.map.MapboxMapViewBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewBinderTest {

    lateinit var sut: NavigationViewBinder

    @Before
    fun setUp() {
        sut = NavigationViewBinder()
    }

    @Test
    fun `applyCustomization should update NON NULL binders`() {
        val c = customization()

        sut.applyCustomization(c)

        assertEquals(c.speedLimitBinder, sut.speedLimit.value)
        assertEquals(c.maneuverBinder, sut.maneuver.value)
        assertEquals(c.roadNameBinder, sut.roadName.value)

        assertEquals(c.actionButtonsBinder, sut.actionButtonsBinder.value)
        assertEquals(c.customActionButtons, sut.customActionButtons.value)
        assertEquals(c.actionCompassButtonBinder, sut.actionCompassButtonBinder.value)
        assertEquals(c.actionCameraModeButtonBinder, sut.actionCameraModeButtonBinder.value)
        assertEquals(c.actionToggleAudioButtonBinder, sut.actionToggleAudioButtonBinder.value)
        assertEquals(c.actionRecenterButtonBinder, sut.actionRecenterButtonBinder.value)

        assertEquals(c.leftFrameBinder, sut.leftFrameContentBinder.value)
        assertEquals(c.rightFrameBinder, sut.rightFrameContentBinder.value)

        assertEquals(c.infoPanelBinder, sut.infoPanelBinder.value)
        assertEquals(c.infoPanelHeaderBinder, sut.infoPanelHeaderBinder.value)
        assertEquals(c.infoPanelHeaderFreeDriveBinder, sut.infoPanelHeaderFreeDriveBinder.value)
        assertEquals(
            c.infoPanelHeaderDestinationPreviewBinder,
            sut.infoPanelHeaderDestinationPreviewBinder.value
        )
        assertEquals(
            c.infoPanelHeaderRoutesPreviewBinder,
            sut.infoPanelHeaderRoutesPreviewBinder.value
        )
        assertEquals(
            c.infoPanelHeaderActiveGuidanceBinder,
            sut.infoPanelHeaderActiveGuidanceBinder.value
        )
        assertEquals(c.infoPanelHeaderArrivalBinder, sut.infoPanelHeaderArrivalBinder.value)

        assertEquals(c.infoPanelTripProgressBinder, sut.infoPanelTripProgressBinder.value)
        assertEquals(c.infoPanelContentBinder, sut.infoPanelContentBinder.value)
        assertEquals(
            c.infoPanelRoutePreviewButtonBinder,
            sut.infoPanelRoutePreviewButtonBinder.value
        )
        assertEquals(
            c.infoPanelStartNavigationButtonBinder,
            sut.infoPanelStartNavigationButtonBinder.value
        )
        assertEquals(
            c.infoPanelEndNavigationButtonBinder,
            sut.infoPanelEndNavigationButtonBinder.value
        )
        assertEquals(c.infoPanelPoiNameBinder, sut.infoPanelPoiNameBinder.value)
        assertEquals(c.infoPanelArrivalTextBinder, sut.infoPanelArrivalTextBinder.value)
        assertEquals(c.mapViewBinder, sut.mapViewBinder.value)
    }

    @Test
    fun `applyCustomization should reset to default binders`() {
        sut.applyCustomization(customization())

        sut.applyCustomization(
            ViewBinderCustomization().apply {
                speedLimitBinder = UIBinder.USE_DEFAULT
                maneuverBinder = UIBinder.USE_DEFAULT
                roadNameBinder = UIBinder.USE_DEFAULT

                actionButtonsBinder = UIBinder.USE_DEFAULT
                customActionButtons = emptyList()
                actionCompassButtonBinder = UIBinder.USE_DEFAULT
                actionCameraModeButtonBinder = UIBinder.USE_DEFAULT
                actionToggleAudioButtonBinder = UIBinder.USE_DEFAULT
                actionRecenterButtonBinder = UIBinder.USE_DEFAULT

                leftFrameBinder = UIBinder.USE_DEFAULT
                rightFrameBinder = UIBinder.USE_DEFAULT

                infoPanelBinder = InfoPanelBinder.defaultBinder()
                infoPanelHeaderBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderFreeDriveBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderDestinationPreviewBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderRoutesPreviewBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderActiveGuidanceBinder = UIBinder.USE_DEFAULT
                infoPanelHeaderArrivalBinder = UIBinder.USE_DEFAULT
                infoPanelPoiNameBinder = UIBinder.USE_DEFAULT
                infoPanelArrivalTextBinder = UIBinder.USE_DEFAULT

                infoPanelTripProgressBinder = UIBinder.USE_DEFAULT
                infoPanelContentBinder = UIBinder.USE_DEFAULT
                infoPanelRoutePreviewButtonBinder = UIBinder.USE_DEFAULT
                infoPanelStartNavigationButtonBinder = UIBinder.USE_DEFAULT
                infoPanelEndNavigationButtonBinder = UIBinder.USE_DEFAULT

                mapViewBinder = MapViewBinder.defaultBinder()
            }
        )

        assertTrue(sut.speedLimit.value == null)
        assertTrue(sut.maneuver.value == null)
        assertTrue(sut.roadName.value == null)

        assertTrue(sut.actionButtonsBinder.value == null)
        assertTrue(sut.customActionButtons.value.isEmpty())
        assertTrue(sut.actionCompassButtonBinder.value == null)
        assertTrue(sut.actionCameraModeButtonBinder.value == null)
        assertTrue(sut.actionToggleAudioButtonBinder.value == null)
        assertTrue(sut.actionRecenterButtonBinder.value == null)

        assertTrue(sut.leftFrameContentBinder.value == null)
        assertTrue(sut.rightFrameContentBinder.value == null)

        assertTrue(sut.infoPanelBinder.value is MapboxInfoPanelBinder)
        assertTrue(sut.infoPanelHeaderBinder.value == null)
        assertTrue(sut.infoPanelHeaderFreeDriveBinder.value == null)
        assertTrue(sut.infoPanelHeaderDestinationPreviewBinder.value == null)
        assertTrue(sut.infoPanelHeaderRoutesPreviewBinder.value == null)
        assertTrue(sut.infoPanelHeaderActiveGuidanceBinder.value == null)
        assertTrue(sut.infoPanelHeaderArrivalBinder.value == null)
        assertTrue(sut.infoPanelRoutePreviewButtonBinder.value == null)
        assertTrue(sut.infoPanelStartNavigationButtonBinder.value == null)
        assertTrue(sut.infoPanelEndNavigationButtonBinder.value == null)
        assertTrue(sut.infoPanelPoiNameBinder.value == null)
        assertTrue(sut.infoPanelArrivalTextBinder.value == null)

        assertTrue(sut.infoPanelTripProgressBinder.value == null)
        assertTrue(sut.infoPanelContentBinder.value == null)
        assertTrue(sut.mapViewBinder.value is MapboxMapViewBinder)
    }

    private fun customization() = ViewBinderCustomization().apply {
        speedLimitBinder = EmptyBinder()
        maneuverBinder = EmptyBinder()
        roadNameBinder = EmptyBinder()

        leftFrameBinder = EmptyBinder()
        rightFrameBinder = EmptyBinder()

        actionButtonsBinder = EmptyBinder()
        customActionButtons = listOf(
            ActionButtonDescription(mockk(), START),
            ActionButtonDescription(mockk(), START),
            ActionButtonDescription(mockk(), END)
        )
        actionCompassButtonBinder = EmptyBinder()
        actionCameraModeButtonBinder = EmptyBinder()
        actionToggleAudioButtonBinder = EmptyBinder()
        actionRecenterButtonBinder = EmptyBinder()

        infoPanelBinder = mockk()
        infoPanelHeaderBinder = EmptyBinder()
        infoPanelHeaderFreeDriveBinder = EmptyBinder()
        infoPanelHeaderDestinationPreviewBinder = EmptyBinder()
        infoPanelHeaderRoutesPreviewBinder = EmptyBinder()
        infoPanelHeaderActiveGuidanceBinder = EmptyBinder()
        infoPanelHeaderArrivalBinder = EmptyBinder()
        infoPanelPoiNameBinder = EmptyBinder()
        infoPanelArrivalTextBinder = EmptyBinder()

        infoPanelTripProgressBinder = EmptyBinder()
        infoPanelContentBinder = EmptyBinder()
        infoPanelRoutePreviewButtonBinder = EmptyBinder()
        infoPanelStartNavigationButtonBinder = EmptyBinder()
        infoPanelEndNavigationButtonBinder = EmptyBinder()
        mapViewBinder = mockk()
    }
}
