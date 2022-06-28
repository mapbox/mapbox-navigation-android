package com.mapbox.navigation.ui.base.view

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton.State
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@ExperimentalPreviewMapboxNavigationAPI
class MapboxExtendableButtonTest {

    private lateinit var ctx: Context
    private lateinit var sut: MapboxExtendableButton

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = MapboxExtendableButton(ctx, null, 0)
    }

    @Test
    fun `setState should update iconImage`() {
        sut.setState(State(android.R.drawable.ic_secure))

        assertEquals(
            android.R.drawable.ic_secure,
            shadowOf(sut.iconImage.drawable).createdFromResId
        )
    }

    @Test
    fun `setState should update and show TEXT`() {
        sut.setState(State(android.R.drawable.ic_secure, "text", 1000))

        assertEquals("text", sut.textView.text)
        assertEquals(View.VISIBLE, sut.textView.visibility)
    }
}
