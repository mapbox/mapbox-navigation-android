package com.mapbox.navigation.dropin.camera

import android.app.Service
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.infopanel.InfoPanelBehavior
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
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

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var store: TestStore
    private lateinit var binding: MapboxNavigationViewLayoutBinding
    private lateinit var infoPanelBehavior: InfoPanelBehavior
    private lateinit var sut: CameraLayoutObserver

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        binding = MapboxNavigationViewLayoutBinding.inflate(
            context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            FrameLayout(context)
        ).apply {
            // slightly modifying default layout to simulate layout changes
            container.top = 100
            container.bottom = 1100
            container.right = 1000
            guidanceLayout.bottom = 100
            speedLimitLayout.right = 100
            roadNameLayout.top = 900
            actionListLayout.left = 900
            infoPanelLayout.right = 500
        }
        store = TestStore()
        val navContext = NavigationViewContext(
            context,
            TestLifecycleOwner(),
            NavigationViewModel(),
        ) { store }
        infoPanelBehavior = navContext.behavior.infoPanelBehavior
        sut = CameraLayoutObserver(navContext, binding)
    }

    @Test
    @Config(qualifiers = "port")
    fun `portrait - should update camera paddings`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            binding.coordinatorLayout.triggerLayoutChange()

            val action = store.actions.last() as CameraAction.UpdatePadding
            assertEquals("top", action.padding.top, 200 + paddingV, 0.001)
            assertEquals("bottom", action.padding.bottom, 200 + paddingV, 0.001)
            assertEquals("left", action.padding.left, 100 + paddingH, 0.001)
            assertEquals("right", action.padding.right, 100 + paddingH, 0.001)
        }

    @Test
    @Config(qualifiers = "land")
    fun `landscape - should update camera paddings when bottom sheet is collapsed`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            infoPanelBehavior.updateBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
            binding.coordinatorLayout.triggerLayoutChange()

            val action = store.actions.last() as CameraAction.UpdatePadding
            assertEquals("top", action.padding.top, 100 + paddingV, 0.001)
            assertEquals("bottom", action.padding.bottom, 200 + paddingV, 0.001)
            assertEquals("left", action.padding.left, 500 + paddingH, 0.001)
            assertEquals("right", action.padding.right, 100 + paddingH, 0.001)
        }

    @Test
    @Config(qualifiers = "land")
    fun `landscape - should update camera paddings when bottom sheet is hidden`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            infoPanelBehavior.updateBottomSheetState(BottomSheetBehavior.STATE_HIDDEN)
            binding.coordinatorLayout.triggerLayoutChange()

            val action = store.actions.last() as CameraAction.UpdatePadding
            assertEquals("top", action.padding.top, 100 + paddingV, 0.001)
            assertEquals("bottom", action.padding.bottom, 200 + paddingV, 0.001)
            assertEquals("left", action.padding.left, 100 + paddingH, 0.001)
            assertEquals("right", action.padding.right, 100 + paddingH, 0.001)
        }

    private suspend fun View.triggerLayoutChange() = coroutineScope {
        launch {
            waitForLayoutChange()
        }
        layout(0, 0, 1000, 1200)
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

    private val paddingV: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val paddingH: Double
        get() = binding.root.resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()
}
