package com.mapbox.navigation.voice.model

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechVolumeTest {

    @Test(expected = IllegalArgumentException::class)
    fun `invalid speech volume - less than minimum`() {
        SpeechVolume(-1.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid speech volume - greater than maximum`() {
        SpeechVolume(2.0f)
    }

    @Test
    fun `valid speech volume - sanity`() {
        val speechVolume = SpeechVolume(0.5f)

        assertEquals(0.5f, speechVolume.level)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(SpeechVolume::class.java)
            .verify()

        ToStringVerifier.forClass(SpeechVolume::class.java)
            .verify()
    }
}
