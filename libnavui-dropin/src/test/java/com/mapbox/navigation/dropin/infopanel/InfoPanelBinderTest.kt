package com.mapbox.navigation.dropin.infopanel

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.EmptyBinder
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class InfoPanelBinderTest {

    private lateinit var ctx: Context
    private lateinit var sut: StubInfoPanelBinder

    @SpyK
    var headerBinder = EmptyBinder()

    @SpyK
    var contentBinder = EmptyBinder()

    private lateinit var viewGroup: ViewGroup

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        ctx = ApplicationProvider.getApplicationContext()
        viewGroup = FrameLayout(ctx)

        sut = StubInfoPanelBinder(ctx)
        sut.setBinders(headerBinder, contentBinder)
    }

    @Test
    fun `bind should call headerBinder`() {
        sut.bind(viewGroup)

        verify { headerBinder.bind(sut.headerLayout) }
    }

    @Test
    fun `bind should call contentBinder`() {
        sut.bind(viewGroup)

        verify { contentBinder.bind(sut.contentLayout) }
    }

    @Test
    fun `bind should NOT call headerBinder when headerLayout is null`() {
        val sut = EmptyInfoPanelBinder(ctx).apply { setBinders(headerBinder, contentBinder) }

        sut.bind(viewGroup)

        verify(exactly = 0) { contentBinder.bind(any()) }
    }

    @Test
    fun `bind should NOT call contentBinder when contentLayout is null`() {
        val sut = EmptyInfoPanelBinder(ctx).apply { setBinders(headerBinder, contentBinder) }

        sut.bind(viewGroup)

        verify(exactly = 0) { contentBinder.bind(any()) }
    }

    private class StubInfoPanelBinder(context: Context) : InfoPanelBinder() {
        var layout = FrameLayout(context)
        var headerLayout = FrameLayout(context)
        var contentLayout = FrameLayout(context)

        override fun onCreateLayout(
            layoutInflater: LayoutInflater,
            root: ViewGroup
        ): ViewGroup = layout

        override fun getHeaderLayout(layout: ViewGroup): ViewGroup = headerLayout

        override fun getContentLayout(layout: ViewGroup): ViewGroup = contentLayout
    }

    private class EmptyInfoPanelBinder(context: Context) : InfoPanelBinder() {
        var layout = FrameLayout(context)

        override fun onCreateLayout(
            layoutInflater: LayoutInflater,
            root: ViewGroup
        ): ViewGroup = layout

        override fun getHeaderLayout(layout: ViewGroup): ViewGroup? = null

        override fun getContentLayout(layout: ViewGroup): ViewGroup? = null
    }
}
