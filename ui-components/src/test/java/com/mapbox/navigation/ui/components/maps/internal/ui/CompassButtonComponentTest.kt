package com.mapbox.navigation.ui.components.maps.internal.ui

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChangedCoalesced
import com.mapbox.maps.CameraChangedCoalescedCallback
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.delegates.MapPluginExtensionsDelegate
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.components.MapboxExtendableButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(MapboxExperimental::class)
class CompassButtonComponentTest {

    private val mapboxMap = mockk<MapboxMap>(relaxed = true)
    private val mapView = mockk<MapView>(relaxed = true) {
        every { getMapboxMap() } returns this@CompassButtonComponentTest.mapboxMap
    }
    private val iconImage = mockk<AppCompatImageView>(relaxed = true)
    private val compassButton = mockk<MapboxExtendableButton>(relaxed = true) {
        every { iconImage } returns this@CompassButtonComponentTest.iconImage
    }
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private lateinit var component: CompassButtonComponent

    @Before
    fun setUp() {
        mockkStatic(MapPluginExtensionsDelegate::flyTo)
    }

    @After
    fun tearDown() {
        unmockkStatic(MapPluginExtensionsDelegate::flyTo)
    }

    @Test
    fun `onAttached when mapView is null`() {
        component = CompassButtonComponent(compassButton, null)

        component.onAttached(mapboxNavigation)

        verify(exactly = 0) { compassButton.setOnClickListener(any()) }
        verify(exactly = 0) { mapboxMap.subscribeStyleLoaded(any()) }
    }

    @Test
    fun `onAttached when mapView is NOT null`() {
        component = CompassButtonComponent(compassButton, mapView)

        component.onAttached(mapboxNavigation)

        verify(exactly = 1) { compassButton.setOnClickListener(any()) }
        verify(exactly = 1) { mapboxMap.subscribeCameraChangedCoalesced(any()) }
    }

    @Test
    fun `onDetached`() {
        val cameraChangedTask = mockk<Cancelable>(relaxed = true)
        every { mapboxMap.subscribeCameraChangedCoalesced(any()) } returns cameraChangedTask
        val cameraCallbacks = mutableListOf<CameraChangedCoalescedCallback>()
        component = CompassButtonComponent(compassButton, mapView)
        component.onAttached(mapboxNavigation)
        verify(exactly = 1) { mapboxMap.subscribeCameraChangedCoalesced(capture(cameraCallbacks)) }

        component.onDetached(mapboxNavigation)

        verify(exactly = 1) { compassButton.setOnClickListener(null) }
        verify(exactly = 1) { cameraChangedTask.cancel() }
    }

    @Test
    fun `compass button click returns to north position`() {
        val cameraOptions = mutableListOf<CameraOptions>()
        val btnOnClickListeners = mutableListOf<View.OnClickListener>()
        component = CompassButtonComponent(compassButton, mapView)

        component.onAttached(mapboxNavigation)

        verify(exactly = 1) { compassButton.setOnClickListener(capture(btnOnClickListeners)) }
        btnOnClickListeners.first().onClick(mockk())
        verify { mapboxMap.flyTo(capture(cameraOptions)) }
        assertEquals(0.0, cameraOptions.first().bearing)
    }

    @Test
    fun `camera state change rotates the image accordingly`() {
        val cameraChangedTask = mockk<Cancelable>()
        every { mapboxMap.subscribeCameraChangedCoalesced(any()) } returns cameraChangedTask
        val cameraCallbacks = mutableListOf<CameraChangedCoalescedCallback>()
        component = CompassButtonComponent(compassButton, mapView)

        component.onAttached(mapboxNavigation)

        verify(exactly = 1) { mapboxMap.subscribeCameraChangedCoalesced(capture(cameraCallbacks)) }
        val cameraState = mockk<CameraState>()
        every { cameraState.bearing } returns 78.12
        cameraCallbacks.first().run(CameraChangedCoalesced(cameraState, mockk()))
        verify { iconImage.rotation = -78.12f }
    }
}
