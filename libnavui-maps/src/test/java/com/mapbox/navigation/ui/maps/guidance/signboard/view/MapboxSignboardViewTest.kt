package com.mapbox.navigation.ui.maps.guidance.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(NavSDKRobolectricTestRunner::class)
class MapboxSignboardViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `render signboard null when error`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Failure(SignboardError("whatever", null))
        val expected = null

        view.render(signboard)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard when success`() {
        val view = MapboxSignboardView(ctx)
        val mockBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val signboard = Expected.Success(SignboardValue(mockBitmap))

        view.render(signboard)

        assertNull(Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }
}
