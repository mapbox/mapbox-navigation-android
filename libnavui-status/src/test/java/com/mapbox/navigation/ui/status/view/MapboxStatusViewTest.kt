package com.mapbox.navigation.ui.status.view

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.status.R
import com.mapbox.navigation.utils.internal.isVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import com.mapbox.navigation.ui.status.model.StatusFactory.buildStatus as status

@OptIn(ExperimentalMapboxNavigationAPI::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class MapboxStatusViewTest {

    private lateinit var ctx: Context
    private lateinit var sut: MapboxStatusView

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = MapboxStatusView(ctx)
    }

    @Test
    fun `constructor - should configure view with default style`() {
        val defaults = object {
            val showAnimRes = android.R.animator.fade_in
            val hideAnimRes = android.R.animator.fade_out
            val textColors = ctx.getColorStateList(R.color.mapbox_status_view_foreground)
        }

        assertEquals("showAnimRes", defaults.showAnimRes, sut.showAnimRes)
        assertEquals("hideAnimRes", defaults.hideAnimRes, sut.hideAnimRes)
        assertEquals(
            "messageTextView.textColor",
            defaults.textColors,
            sut.messageTextView.textColors
        )
    }

    @Test
    fun `render - should update isRendered and currentState`() {
        val status = status("message", animated = false)

        sut.render(status)

        assertEquals("currentStatus", status, sut.currentStatus)
        assertTrue("isRendered", sut.isRendered)
    }

    @Test
    fun `render - should update message text`() {
        val status = status("message", animated = false)

        sut.render(status)

        assertEquals("text", status.message, sut.messageTextView.text)
    }

    @Test
    fun `render - should update spinner`() {
        val status = status("message", animated = false, spinner = true)

        sut.render(status)

        assertTrue("isVisible", sut.spinnerProgressBar.isVisible)
    }

    @Test
    fun `render - should update icon`() {
        val status = status("message", animated = false, icon = android.R.drawable.ic_secure)

        sut.render(status)

        assertTrue("isVisible", sut.iconImage.isVisible)
    }

    @Test
    fun `cancel - should hide the view`() {
        val status = status("message", animated = true, icon = android.R.drawable.ic_secure)
        sut.render(status)
        sut.cancel()

        ShadowLooper.idleMainLooper()
        assertFalse("isVisible", sut.isRendered)
    }
}
