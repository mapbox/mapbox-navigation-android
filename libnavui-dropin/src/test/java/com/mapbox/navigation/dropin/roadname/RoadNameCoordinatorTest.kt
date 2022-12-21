package com.mapbox.navigation.dropin.roadname

import android.content.res.Configuration
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoadNameCoordinatorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val bottomSheetFlow = MutableStateFlow<Int?>(null)
    private val roadNameFlow = MutableStateFlow<UIBinder?>(null)
    private val showRoadNameFlow = MutableStateFlow(true)
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val configuration = Configuration()
    private val layoutParams = ConstraintLayout.LayoutParams(50, 50)

    //    private val roadNameLayout = mockk<ViewGroup> {
    private val roadNameLayout = mockk<ConstraintLayout>(relaxed = true) {
        every { resources } returns mockk {
            every { configuration } returns this@RoadNameCoordinatorTest.configuration
        }
    }
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { behavior.infoPanelBehavior } returns mockk {
            every { bottomSheetState } returns bottomSheetFlow
        }
        every { uiBinders } returns mockk {
            every { roadName } returns roadNameFlow
        }
        every { options } returns mockk {
            every { showRoadName } returns showRoadNameFlow
        }
    }
    private val observer = mockk<MapboxNavigationObserver>(relaxed = true)
    private val coordinator = RoadNameCoordinator(context, roadNameLayout)

    @Before
    fun setUp() {
        mockkStatic("androidx.core.view.ViewKt")
        mockkConstructor(RoadNameViewBinder::class)
        every { anyConstructed<RoadNameViewBinder>().bind(any()) } returns observer
        every { roadNameLayout.layoutParams } returns this@RoadNameCoordinatorTest.layoutParams
        every { roadNameLayout.context } returns ApplicationProvider.getApplicationContext()
//        every { roadNameLayout.updateLayoutParams(any()) } answers {
//            val block = firstArg() as ViewGroup.LayoutParams.() -> Unit
//            this@RoadNameCoordinatorTest.layoutParams.block()
//        }
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.core.view.ViewKt")
        unmockkConstructor(RoadNameViewBinder::class)
    }

    @Test
    fun `should return default binder`() = runBlockingTest {
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is RoadNameViewBinder)
        }
    }

    @Test
    fun `should return custom binder`() = runBlockingTest {
        val customBinder = mockk<UIBinder>()
        coordinator.apply {
            roadNameFlow.value = customBinder
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() === customBinder)
        }
    }

    @Test
    fun onAttached_landscapeAndDragging() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_DRAGGING
        coordinator.onAttached(mapboxNavigation)
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun onAttached_landscapeAndSettling() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_SETTLING
        coordinator.onAttached(mapboxNavigation)
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun onAttached_landscapeAndExpanded() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_EXPANDED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun onAttached_landscapeAndCollapsed() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_COLLAPSED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun onAttached_landscapeAndHalfExpanded() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_HALF_EXPANDED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun onAttached_landscapeAndHidden() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_HIDDEN
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndHidden() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_HIDDEN
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndSettling() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_SETTLING
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndDragging() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_DRAGGING
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndCollapsed() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_COLLAPSED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndHalfExpanded() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_HALF_EXPANDED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun onAttached_portraitAndExpanded() = coroutineRule.runBlockingTest {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        bottomSheetFlow.value = BottomSheetBehavior.STATE_EXPANDED
        coordinator.onAttached(mapboxNavigation)
        assertEquals(ConstraintLayout.LayoutParams.PARENT_ID, layoutParams.startToStart)
    }

    @Test
    fun `startToStart changes when bottom sheet state changes`() = coroutineRule.runBlockingTest {
        coordinator.onAttached(mapboxNavigation)

        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        bottomSheetFlow.value = BottomSheetBehavior.STATE_EXPANDED
        assertEquals(R.id.guidelineBegin, layoutParams.startToStart)
    }

    @Test
    fun `null bottom sheets are ignored`() = coroutineRule.runBlockingTest {
        coordinator.onAttached(mapboxNavigation)
        clearAllMocks(answers = false)
        bottomSheetFlow.value = null
        verify(exactly = 0) { roadNameLayout.layoutParams }
        assertEquals(ConstraintLayout.LayoutParams.UNSET, layoutParams.startToStart)
    }
}
