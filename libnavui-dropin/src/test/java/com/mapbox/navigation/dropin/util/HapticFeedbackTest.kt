package com.mapbox.navigation.dropin.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HapticFeedbackTest {

    lateinit var context: Context
    lateinit var oldVibrator: Vibrator
    lateinit var newVibrator: Vibrator

    @Before
    fun setUp() {
        oldVibrator = mockk(relaxed = true)
        newVibrator = mockk(relaxed = true)
        context = mockk {
            every { getSystemService(Context.VIBRATOR_MANAGER_SERVICE) } answers {
                mockk<VibratorManager> {
                    every { defaultVibrator } returns newVibrator
                }
            }
            every { getSystemService(Context.VIBRATOR_SERVICE) } returns oldVibrator
        }
    }

    @Config(sdk = [Build.VERSION_CODES.N])
    @Test
    fun `vibrate should use old Vibrator API`() {
        val sut = HapticFeedback.create(context)

        sut.vibrate(100)

        verify { oldVibrator.vibrate(100) }
    }

    @Config(sdk = [Build.VERSION_CODES.S])
    @Test
    fun `vibrate should use new Vibrator API`() {
        val sut = HapticFeedback.create(context)

        sut.vibrate(100)

        val vibe = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        verify { newVibrator.vibrate(vibe) }
    }
}
