package com.mapbox.navigation.ui.components.maps.view

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.components.test.R
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@ExperimentalPreviewMapboxNavigationAPI
class MapboxCameraModeButtonTest {

    private lateinit var ctx: Context
    private lateinit var sut: MapboxCameraModeButton

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = MapboxCameraModeButton(ctx, null, 0)
    }

    @Test
    fun `constructor should apply default style`() {
        assertNotNull(sut.followingText)
        assertNotNull(sut.overviewText)
        assertEquals(R.drawable.mapbox_ic_camera_follow, sut.followingIconResId)
        assertEquals(R.drawable.mapbox_ic_camera_overview, sut.overviewIconResId)
    }

    @Test
    fun `setState should show OVERVIEW icon for FOLLOWING camera state`() {
        sut.setState(NavigationCameraState.FOLLOWING)

        assertEquals(sut.overviewIconResId, shadowOf(sut.iconImage.drawable).createdFromResId)
    }

    @Test
    fun `setState should show FOLLOWING icon for OVERVIEW camera state`() {
        sut.setState(NavigationCameraState.OVERVIEW)

        assertEquals(sut.followingIconResId, shadowOf(sut.iconImage.drawable).createdFromResId)
    }

    @Test
    fun `setStateAndExtend should show FOLLOWING text for OVERVIEW camera state`() {
        sut.setStateAndExtend(NavigationCameraState.OVERVIEW, 0)

        assertEquals(ctx.getText(R.string.mapbox_route_follow), sut.textView.text)
        assertEquals(View.VISIBLE, sut.textView.visibility)
    }
}
