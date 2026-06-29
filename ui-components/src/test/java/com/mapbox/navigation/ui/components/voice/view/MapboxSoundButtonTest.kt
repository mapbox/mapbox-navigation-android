package com.mapbox.navigation.ui.components.voice.view

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.components.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

private const val customText = "custom text"

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MapboxSoundButtonTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxSoundButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxSoundButton(ctx, null)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        view.unmute()

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        view.mute()

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `update style`() {
        val view = MapboxSoundButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        view.updateStyle(R.style.MapboxStyleSound)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `mute returns true`() {
        val view = MapboxSoundButton(ctx, null, 0)

        assertTrue(view.mute())
    }

    @Test
    fun `unmute returns false`() {
        val view = MapboxSoundButton(ctx, null, 0)

        assertFalse(view.unmute())
    }

    @Test
    fun `mute and extend`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        assertTrue(view.muteAndExtend(0))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(soundButtonText.text.isEmpty())
        assertEquals(soundButtonText.visibility, View.INVISIBLE)
    }

    @Test
    fun `mute and extend with text`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        view.muteAndExtend(0, customText)

        assertEquals(soundButtonText.text, customText)
        assertEquals(soundButtonText.visibility, View.VISIBLE)
    }

    @Test
    fun `mute and unmute and extend multiple times is allowed`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        assertTrue(view.muteAndExtend(100))
        assertTrue(view.muteAndExtend(100))
        assertFalse(view.unmuteAndExtend(100))
        assertTrue(view.muteAndExtend(100))
        assertFalse(view.unmuteAndExtend(100))
        assertFalse(view.unmuteAndExtend(100))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(soundButtonText.text.isEmpty())
        assertEquals(soundButtonText.visibility, View.INVISIBLE)
    }

    @Test
    fun `unmute and extend`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        assertFalse(view.unmuteAndExtend(0))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(soundButtonText.text.isEmpty())
        assertEquals(soundButtonText.visibility, View.INVISIBLE)
    }

    @Test
    fun `unmute and extend with text`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        view.unmuteAndExtend(0, customText)

        assertEquals(soundButtonText.text, customText)
        assertEquals(soundButtonText.visibility, View.VISIBLE)
    }
}
