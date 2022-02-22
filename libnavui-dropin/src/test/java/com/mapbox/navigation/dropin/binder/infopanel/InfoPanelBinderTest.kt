package com.mapbox.navigation.dropin.binder.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.EmptyBinder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class InfoPanelBinderTest {

    private lateinit var ctx: Context
    private lateinit var sut: InfoPanelBinder

    @SpyK
    var headerBinder = EmptyBinder()

    @SpyK
    var contentBinder = EmptyBinder()

    @MockK
    lateinit var mockViewGroup: ViewGroup

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        ctx = ApplicationProvider.getApplicationContext()
        sut = InfoPanelBinder(headerBinder, contentBinder)
        every { mockViewGroup.findViewById<ViewGroup>(any()) } returns FrameLayout(ctx)
    }

    @Test
    fun `bind should call headerBinder`() {
        sut.bind(mockViewGroup)

        verify { headerBinder.bind(allAny()) }
    }

    @Test
    fun `bind should call contentBinder`() {
        sut.bind(mockViewGroup)

        verify { contentBinder.bind(allAny()) }
    }
}
