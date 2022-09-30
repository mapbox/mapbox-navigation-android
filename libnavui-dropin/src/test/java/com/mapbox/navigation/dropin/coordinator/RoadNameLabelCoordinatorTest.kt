package com.mapbox.navigation.dropin.coordinator

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.lifecycle.Binder
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoadNameLabelCoordinatorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val bottomSheetFlow = MutableStateFlow<Int?>(null)
    private val roadNameFlow = MutableStateFlow<UIBinder?>(null)
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val configuration = Configuration()
    private val layoutParams = ConstraintLayout.LayoutParams(50, 50)
//    private val roadNameLayout = mockk<ViewGroup> {
    private val roadNameLayout = mockk<ConstraintLayout>(relaxed = true) {
        every { resources } returns mockk {
            every { configuration } returns this@RoadNameLabelCoordinatorTest.configuration
        }
    }
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { infoPanelBehavior } returns mockk {
            every { bottomSheetState } returns bottomSheetFlow
        }
        every { uiBinders } returns mockk {
            every { roadName } returns roadNameFlow
        }
    }
    private val observer = mockk<MapboxNavigationObserver>(relaxed = true)
    private val coordinator = RoadNameLabelCoordinator(context, roadNameLayout)

    @Before
    fun setUp() {
        mockkStatic("androidx.core.view.ViewKt")
        mockkConstructor(RoadNameViewBinder::class)
        every { anyConstructed<RoadNameViewBinder>().bind(any()) } returns observer
        every { roadNameLayout.layoutParams } returns this@RoadNameLabelCoordinatorTest.layoutParams
        every { roadNameLayout.context } returns ApplicationProvider.getApplicationContext()
//        every { roadNameLayout.updateLayoutParams(any()) } answers {
//            val block = firstArg() as ViewGroup.LayoutParams.() -> Unit
//            this@RoadNameLabelCoordinatorTest.layoutParams.block()
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
    fun `should reload binder when speedLimit changes`() = runBlockingTest {
        val customBinder1 = mockk<UIBinder>()
        val customBinder2 = mockk<UIBinder>()
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(4).toList(collectedBinders)
            }
            roadNameFlow.value = customBinder1
            roadNameFlow.value = null
            roadNameFlow.value = customBinder2
            job.join()
            assertEquals(4, collectedBinders.size)
            assertTrue(collectedBinders[0] is RoadNameViewBinder)
            assertTrue(collectedBinders[1] === customBinder1)
            assertTrue(collectedBinders[2] is RoadNameViewBinder)
            assertTrue(collectedBinders[3] === customBinder2)
        }
    }

    @Test
    fun `should use different default binder instances`() = runBlockingTest {
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(3).toList(collectedBinders)
            }
            roadNameFlow.value = mockk()
            roadNameFlow.value = null
            job.join()
            assertTrue(collectedBinders[0] is RoadNameViewBinder)
            assertTrue(collectedBinders[2] is RoadNameViewBinder)
            assertFalse(collectedBinders[0] === collectedBinders[2])
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

    // TODO custom onAttached
}
