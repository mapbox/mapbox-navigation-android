package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstVoiceInstructionsCheckerTest {

    private val sut = FirstVoiceInstructionsChecker()

    @Test
    fun `isFirstInstruction no saved instruction`() {
        val instruction = VoiceInstructions.builder().announcement("turn left").build()
        assertFalse(sut.isFirstVoiceInstruction(instruction))
    }

    @Test
    fun `isFirstInstruction has same saved instruction`() {
        val instruction1 = VoiceInstructions.builder().announcement("turn left").build()
        val instruction2 = VoiceInstructions.builder().announcement("turn left").build()
        sut.onNewFirstInstruction(instruction1)

        assertTrue(sut.isFirstVoiceInstruction(instruction2))
    }

    @Test
    fun `isFirstInstruction has another saved instruction`() {
        val instruction1 = VoiceInstructions.builder().announcement("turn left").build()
        val instruction2 = VoiceInstructions.builder().ssmlAnnouncement("turn left").build()
        sut.onNewFirstInstruction(instruction1)

        assertFalse(sut.isFirstVoiceInstruction(instruction2))
    }

    @Test
    fun `isFirstInstruction has removed instruction`() {
        val instruction1 = VoiceInstructions.builder().announcement("turn left").build()
        sut.onNewFirstInstruction(instruction1)
        sut.onNewFirstInstruction(null)

        assertFalse(sut.isFirstVoiceInstruction(instruction1))
    }

    @Test
    fun `onNewFirstInstruction invalid instruction`() {
        val instruction = VoiceInstructions.builder().announcement("turn left").build()
        sut.onNewFirstInstruction(VoiceInstructions.builder().build())

        assertFalse(sut.isFirstVoiceInstruction(instruction))
    }
}
