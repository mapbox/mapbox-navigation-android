package com.mapbox.navigation.ui.maps.route.callout.api

import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class DurationKtxTest {
    @Test
    fun `value with remainder - call roundUp once - returns value greater by 1`() {
        assertEquals(3.minutes, (2.minutes + 22.seconds).roundUpByAbs(DurationUnit.MINUTES))
    }

    @Test
    fun `negative value with remainder - call roundUp once - returns value greater by 1`() {
        assertEquals((-2).minutes, ((-2).minutes + 22.seconds).roundUpByAbs(DurationUnit.MINUTES))
    }

    @Test
    fun `value without remainder - call roundUp once - returns the same value`() {
        assertEquals(2.minutes, (2.minutes).roundUpByAbs(DurationUnit.MINUTES))
    }

    @Test
    fun `value with remainder - call roundUp more than once - returns value greater by 1`() {
        assertEquals(
            2.minutes,
            (1.minutes + 22.seconds).roundUpByAbs(DurationUnit.MINUTES)
                .roundUpByAbs(DurationUnit.MINUTES)
                .roundUpByAbs(DurationUnit.MINUTES),
        )
    }
}
