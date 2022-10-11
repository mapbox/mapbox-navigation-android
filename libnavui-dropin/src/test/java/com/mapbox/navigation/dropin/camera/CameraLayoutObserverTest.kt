@file:Suppress("PrivatePropertyName", "MaxLineLength")

package com.mapbox.navigation.dropin.camera

import android.app.Service
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraLayoutObserverTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var binding: MapboxNavigationViewLayoutBinding
    private lateinit var mapView: View
    private lateinit var sut: CameraLayoutObserver

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        binding = MapboxNavigationViewLayoutBinding.inflate(
            context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            FrameLayout(context)
        ).apply {
            // slightly modifying default layout to simulate layout changes
            guidanceLayout.bottom = 100
            roadNameLayout.top = 100
            actionListLayout.left = 100
            infoPanelLayout.right = 100
        }
        mapView = View(context)
        store = TestStore()
        sut = CameraLayoutObserver(store, mapView, binding)
    }

    @Test
    @Config(qualifiers = "port")
    fun `portrait - should update bottom padding for DestinationPreview, FreeDrive and RoutePreview state`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            triggerLayoutChangesAndVerifyDispatchedActions(
                givenNavigationStates = listOf(
                    NavigationState.DestinationPreview,
                    NavigationState.FreeDrive,
                    NavigationState.RoutePreview,
                )
            ) { state, action ->
                assertEquals("$state|top", action.padding.top, PADDING_V_PORT, 0.001)
                assertNotEquals("$state|bottom", action.padding.bottom, PADDING_V_PORT, 0.001)
                assertEquals("$state|left", action.padding.left, PADDING_H_PORT, 0.001)
                assertEquals("$state|right", action.padding.right, PADDING_H_PORT, 0.001)
            }
        }

    @Test
    @Config(qualifiers = "port")
    fun `portrait - should update top and bottom padding for ActiveNavigation and Arrival state`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            triggerLayoutChangesAndVerifyDispatchedActions(
                givenNavigationStates = listOf(
                    NavigationState.ActiveNavigation,
                    NavigationState.Arrival,
                )
            ) { s, action ->
                assertNotEquals("$s|top", action.padding.top, PADDING_V_PORT, 0.001)
                assertNotEquals("$s|bottom", action.padding.bottom, PADDING_V_PORT, 0.001)
                assertEquals("$s|left", action.padding.left, PADDING_H_PORT, 0.001)
                assertEquals("$s|right", action.padding.right, PADDING_H_PORT, 0.001)
            }
        }

    @Test
    @Config(qualifiers = "land")
    fun `landscape - should update bottom padding for FreeDrive state`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            triggerLayoutChangesAndVerifyDispatchedActions(
                givenNavigationStates = listOf(
                    NavigationState.FreeDrive
                )
            ) { s, action ->
                assertEquals("$s|top", action.padding.top, PADDING_V_LAND, 0.001)
                assertNotEquals("$s|bottom", action.padding.bottom, PADDING_V_LAND, 0.001)
                assertEquals("$s|left", action.padding.left, PADDING_H_LAND, 0.001)
                assertEquals("$s|right", action.padding.right, PADDING_H_LAND, 0.001)
            }
        }

    @Test
    @Config(qualifiers = "land")
    fun `landscape - should update left, bottom and right padding for all states except FreeDrive`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            triggerLayoutChangesAndVerifyDispatchedActions(
                givenNavigationStates = listOf(
                    NavigationState.DestinationPreview,
                    NavigationState.RoutePreview,
                    NavigationState.ActiveNavigation,
                    NavigationState.Arrival,
                )
            ) { s, action ->
                assertEquals("$s|top", action.padding.top, PADDING_V_LAND, 0.001)
                assertNotEquals("$s|bottom", action.padding.bottom, PADDING_V_LAND, 0.001)
                assertNotEquals("$s|left", action.padding.left, PADDING_H_LAND, 0.001)
                assertNotEquals("$s|right", action.padding.right, PADDING_H_LAND, 0.001)
            }
        }

    private suspend fun triggerLayoutChangesAndVerifyDispatchedActions(
        givenNavigationStates: List<NavigationState>,
        assertAction: (state: NavigationState, action: CameraAction.UpdatePadding) -> Unit
    ) {
        givenNavigationStates.forEachIndexed { index, navState ->
            store.updateState { it.copy(navigation = navState) }
            binding.coordinatorLayout.triggerLayoutChange()
            assertAction(navState, store.actions[index] as CameraAction.UpdatePadding)
        }
    }

    private suspend fun View.triggerLayoutChange() = coroutineScope {
        launch {
            waitForLayoutChange()
        }
        layout(left + 1, 0, 0, 0)
        // this coroutine scope will wait for all launched coroutines to finish before returning
    }

    private suspend fun View.waitForLayoutChange() = suspendCancellableCoroutine<Unit> { cont ->
        addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                cont.resume(Unit)
                removeOnLayoutChangeListener(this)
            }
        })
    }

    private val PADDING_V_PORT: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val PADDING_H_PORT: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()
    private val PADDING_V_LAND: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_landscape_v).toDouble()
    private val PADDING_H_LAND: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_landscape_h).toDouble()
}
