package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewStyles
import com.mapbox.navigation.dropin.util.TestStore
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.MainScope
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class ActionButtonBinderTest {

    private lateinit var ctx: Context
    private lateinit var viewGroup: ViewGroup

    private lateinit var navContext: NavigationViewContext

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        ctx = ApplicationProvider.getApplicationContext()
        viewGroup = FrameLayout(ctx)

        navContext = mockk {
            every { store } returns TestStore()
            every { styles } returns NavigationViewStyles(ctx)
            every { viewModel } returns mockk {
                every { viewModelScope } returns MainScope()
            }
        }
    }

    @Test
    fun `should add custom buttons to Action Buttons layout`() {
        val customButtons = listOf(
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.START),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.START),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.END),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.END),
        )

        val sut = ActionButtonBinder(navContext, customButtons)
        sut.bind(viewGroup)
        val binder = MapboxActionButtonsLayoutBinding.bind(viewGroup)

        val installedButtons = binder.buttonContainer.children.toList()
        assertEquals(4, installedButtons.size)
        assertEquals(customButtons[0].view, installedButtons[0])
        assertEquals(customButtons[1].view, installedButtons[1])
        assertEquals(customButtons[2].view, installedButtons[2])
        assertEquals(customButtons[3].view, installedButtons[3])
    }
}
