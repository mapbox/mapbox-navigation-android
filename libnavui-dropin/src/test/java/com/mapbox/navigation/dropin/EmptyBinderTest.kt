package com.mapbox.navigation.dropin

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class EmptyBinderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `bind should remove all views from bound viewGroup`() {
        val viewGroup = FrameLayout(context).apply {
            addView(View(context))
        }
        assertEquals(1, viewGroup.childCount)

        val sut = EmptyBinder()
        sut.bind(viewGroup)

        assertEquals(0, viewGroup.childCount)
    }
}
