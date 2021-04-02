package com.mapbox.navigation.ui.voice.view

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.R
import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MapboxSoundButtonTest {

    private lateinit var ctx: Context
    private val consumer: MapboxNavigationConsumer<Boolean> = mockk(relaxed = true)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxSoundButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.soundButtonIcon)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxSoundButton(ctx, null)
        val expectedDrawable = view.findViewById<ImageView>(R.id.soundButtonIcon)

        view.unmute(null)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val expectedDrawable = view.findViewById<ImageView>(R.id.soundButtonIcon)

        view.mute(null)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `update style`() {
        val view = MapboxSoundButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.soundButtonIcon)

        view.updateStyle(R.style.MapboxStyleSound)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `mute callback not invoked`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val messageSlot = slot<Boolean>()

        view.mute(null)

        verify(exactly = 0) { consumer.accept(capture(messageSlot)) }
    }

    @Test
    fun `mute callback invoked`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val messageSlot = slot<Boolean>()

        view.mute(consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertTrue(messageSlot.captured)
    }

    @Test
    fun `unmute callback not invoked`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val messageSlot = slot<Boolean>()

        view.unmute(null)

        verify(exactly = 0) { consumer.accept(capture(messageSlot)) }
    }

    @Test
    fun `unmute callback invoked`() {
        val view = MapboxSoundButton(ctx, null, 0)
        val messageSlot = slot<Boolean>()

        view.unmute(consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertFalse(messageSlot.captured)
    }

    @Test
    fun `mute and extend`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.soundButtonText)
        val messageSlot = slot<Boolean>()

        view.muteAndExtend(0, consumer)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertTrue(messageSlot.captured)
        assertTrue(soundButtonText.visibility == View.INVISIBLE)
    }

    @Test
    fun `mute and unmute and extend multiple times is allowed`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.soundButtonText)
        val messageSlot = slot<Boolean>()

        view.muteAndExtend(100, consumer)
        view.muteAndExtend(100, consumer)
        view.unmuteAndExtend(100, consumer)
        view.muteAndExtend(100, consumer)
        view.unmuteAndExtend(100, consumer)
        view.unmuteAndExtend(100, consumer)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 6) { consumer.accept(capture(messageSlot)) }
        verify(ordering = Ordering.SEQUENCE) {
            consumer.accept(true)
            consumer.accept(true)
            consumer.accept(false)
            consumer.accept(true)
            consumer.accept(false)
            consumer.accept(false)
        }
        assertTrue(soundButtonText.visibility == View.INVISIBLE)
        assertTrue(soundButtonText.visibility == View.INVISIBLE)
    }

    @Test
    fun `unmute and extend`() {
        val view = MapboxSoundButton(ctx)
        val soundButtonText = view.findViewById<AppCompatTextView>(R.id.soundButtonText)
        val messageSlot = slot<Boolean>()

        view.unmuteAndExtend(0, consumer)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertFalse(messageSlot.captured)
        assertTrue(soundButtonText.visibility == View.INVISIBLE)
    }
}
