package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.InputsServiceHandleInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DataInputsManagerTest {

    private val nativeInputsService: InputsServiceHandleInterface = mockk(relaxed = true)
    private val nativeNavigator: MapboxNativeNavigator = mockk(relaxed = true)

    private lateinit var dataInputsManager: DataInputsManager

    @Before
    fun setUp() {
        every { nativeNavigator.inputsService } returns nativeInputsService

        dataInputsManager = DataInputsManager(nativeNavigator)
    }

    @Test
    fun `updates native with received OdometryData`() {
        val nativeData = mockk<com.mapbox.navigator.OdometryData>(relaxed = true)
        val platformData = mockk<OdometryData>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateOdometryData(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateOdometryData(eq(nativeData)) }
    }

    @Test
    fun `updates native with received RawGnssData`() {
        val nativeData = mockk<com.mapbox.navigator.RawGnssData>(relaxed = true)
        val platformData = mockk<RawGnssData>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateRawGnssData(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateRawGnssData(eq(nativeData)) }
    }

    @Test
    fun `updates native with received CompassData`() {
        val nativeData = mockk<com.mapbox.navigator.CompassData>(relaxed = true)
        val platformData = mockk<CompassData>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateCompassData(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateCompassData(eq(nativeData)) }
    }

    @Test
    fun `updates native with received MotionData`() {
        val nativeData = mockk<com.mapbox.navigator.MotionData>(relaxed = true)
        val platformData = mockk<MotionData>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateMotionData(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateMotionData(eq(nativeData)) }
    }

    @Test
    fun `updates native with received AltimeterData`() {
        val nativeData = mockk<com.mapbox.navigator.AltimeterData>(relaxed = true)
        val platformData = mockk<AltimeterData>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateAltimeterData(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateAltimeterData(eq(nativeData)) }
    }

    @Test
    fun `updates native with received EtcGateInfo`() {
        val nativeData = mockk<com.mapbox.navigator.ETCGateInfo>(relaxed = true)
        val platformData = mockk<EtcGateInfo>(relaxed = true).apply {
            every { mapToNative() } returns nativeData
        }

        dataInputsManager.updateEtcGateInfo(platformData)

        verify(exactly = 1) { platformData.mapToNative() }
        verify(exactly = 1) { nativeInputsService.updateEtcGateInfo(eq(nativeData)) }
    }

    @Test
    fun `uses correct native inputs service after navigator recreation`() {
        val newNativeInputsService = mockk<InputsServiceHandleInterface>(relaxed = true)
        every { nativeNavigator.inputsService } returns newNativeInputsService

        val platformData = mockk<CompassData>(relaxed = true).apply {
            every { mapToNative() } returns mockk()
        }
        dataInputsManager.updateCompassData(platformData)

        verify(exactly = 0) { nativeInputsService.updateCompassData(any()) }
        verify(exactly = 1) { newNativeInputsService.updateCompassData(any()) }
    }
}
