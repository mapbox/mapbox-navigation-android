package com.mapbox.navigation.dropin.coordinator

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.NavigationViewModel
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelCoordinatorTest {

    private lateinit var ctx: Context
    private lateinit var sut: InfoPanelCoordinator
    private lateinit var binding: MapboxNavigationViewLayoutBinding

    private lateinit var viewContext: NavigationViewContext
    private lateinit var testStore: TestStore
    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        binding = MapboxNavigationViewLayoutBinding.inflate(
            LayoutInflater.from(ctx),
            FrameLayout(ctx)
        )
        testStore = TestStore()
        viewContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { testStore }
        )
        mapboxNavigation = mockk(relaxed = true)

        sut = InfoPanelCoordinator(
            viewContext,
            binding.infoPanelLayout,
            binding.guidelineBottom
        )
    }

    @Test
    fun `init - should hide BottomSheet`() {
        assertEquals(
            BottomSheetBehavior.STATE_HIDDEN,
            BottomSheetBehavior.from(binding.infoPanelLayout).state
        )
    }

    @Test
    fun `should show BottomSheet when NOT in FreeDrive state`() =
        runBlockingTest {
            testStore.updateState {
                it.copy(navigation = NavigationState.RoutePreview)
            }

            sut.onAttached(mapboxNavigation)

            ShadowLooper.idleMainLooper()
            assertEquals(BottomSheetBehavior.STATE_COLLAPSED, bottomSheetBehavior().state)
        }

    @Test
    fun `should hide BottomSheet when in FreeDrive state`() =
        runBlockingTest {
            testStore.updateState {
                it.copy(navigation = NavigationState.RoutePreview)
            }

            sut.onAttached(mapboxNavigation)
            testStore.updateState {
                it.copy(navigation = NavigationState.FreeDrive)
            }

            ShadowLooper.idleMainLooper()
            assertEquals(BottomSheetBehavior.STATE_HIDDEN, bottomSheetBehavior().state)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `showInfoPanelInFreeDrive customization - should show BottomSheet in FreeDrive when values is true`() =
        runBlockingTest {
            viewContext.applyOptionsCustomization {
                showInfoPanelInFreeDrive = true
            }
            testStore.updateState {
                it.copy(navigation = NavigationState.FreeDrive)
            }

            sut.onAttached(mapboxNavigation)

            ShadowLooper.idleMainLooper()
            assertEquals(BottomSheetBehavior.STATE_COLLAPSED, bottomSheetBehavior().state)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `infoPanelForcedState customization - should update BottomSheet state`() =
        runBlockingTest {
            viewContext.applyOptionsCustomization {
                infoPanelForcedState = BottomSheetBehavior.STATE_EXPANDED
            }
            sut.onAttached(mapboxNavigation)

            ShadowLooper.idleMainLooper()
            assertEquals(BottomSheetBehavior.STATE_EXPANDED, bottomSheetBehavior().state)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `infoPanelForcedState customization - should not hide BottomSheet when infoPanelForcedState is set`() =
        runBlockingTest {
            testStore.updateState {
                it.copy(navigation = NavigationState.RoutePreview)
            }
            viewContext.applyOptionsCustomization {
                infoPanelForcedState = BottomSheetBehavior.STATE_EXPANDED
            }

            sut.onAttached(mapboxNavigation)
            testStore.updateState {
                it.copy(navigation = NavigationState.FreeDrive)
            }

            ShadowLooper.idleMainLooper()
            assertEquals(BottomSheetBehavior.STATE_EXPANDED, bottomSheetBehavior().state)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `isInfoPanelHideable customization - should enable hiding of BottomSheet`() =
        runBlockingTest {
            testStore.updateState {
                it.copy(navigation = NavigationState.RoutePreview)
            }
            sut.onAttached(mapboxNavigation)

            viewContext.applyOptionsCustomization {
                isInfoPanelHideable = true
            }

            assertTrue(bottomSheetBehavior().isHideable)
        }

    private fun bottomSheetBehavior() = BottomSheetBehavior.from(binding.infoPanelLayout)

    private class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }
}
