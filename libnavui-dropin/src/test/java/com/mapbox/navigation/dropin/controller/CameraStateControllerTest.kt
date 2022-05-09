package com.mapbox.navigation.dropin.controller

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.camera.TargetCameraMode
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testStore: TestStore

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        testStore = spyk(TestStore())
    }

    @After
    fun teardown() {
        unmockkObject(MapboxNavigationApp)
    }

    @Test
    fun `when action toIdle updates camera mode`() = coroutineRule.runBlockingTest {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(CameraAction.ToIdle)

        val cameraState = testStore.state.value.camera
        Assert.assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toOverview updates camera mode`() = coroutineRule.runBlockingTest {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(CameraAction.ToOverview)

        val cameraState = testStore.state.value.camera
        Assert.assertEquals(TargetCameraMode.Overview, cameraState.cameraMode)
    }

    @Test
    fun `when action toFollowing updates camera mode and zoomUpdatesAllowed`() =
        coroutineRule.runBlockingTest {
            val sut = CameraStateController(testStore)
            sut.onAttached(mockMapboxNavigation())

            testStore.dispatch(CameraAction.ToFollowing)

            val cameraState = testStore.state.value.camera
            Assert.assertEquals(TargetCameraMode.Following, cameraState.cameraMode)
        }

    @Test
    fun `when action UpdatePadding updates cameraPadding`() =
        coroutineRule.runBlockingTest {
            val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
            val sut = CameraStateController(testStore)
            sut.onAttached(mockMapboxNavigation())

            testStore.dispatch(CameraAction.UpdatePadding(padding))

            val cameraState = testStore.state.value.camera
            Assert.assertEquals(padding, cameraState.cameraPadding)
        }

    @Test
    fun `on SaveMapState action should save map camera state in the store`() {
        val cameraState = com.mapbox.maps.CameraState(
            Point.fromLngLat(11.0, 22.0),
            EdgeInsets(1.0, 2.0, 3.0, 4.0),
            30.0,
            40.0,
            50.0
        )
        val mockMapboxNavigation = mockMapboxNavigation()
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation)

        testStore.dispatch(CameraAction.SaveMapState(cameraState))

        Assert.assertEquals(cameraState, testStore.state.value.camera.mapCameraState)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
