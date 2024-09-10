package com.mapbox.navigation.voice.api

import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.testutils.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceInstructionsParserTest {

    @Test
    fun `parse instruction - should return SSML announcement`() {
        val voiceInstructions = Fixtures.ssmlInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNotNull(result.value)
        assertEquals(voiceInstructions.ssmlAnnouncement(), result.value?.announcement)
        assertEquals("ssml", result.value?.type)
    }

    @Test
    fun `parse instruction - should return TEXT announcement`() {
        val voiceInstructions = Fixtures.textInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNotNull(result.value)
        assertEquals(voiceInstructions.announcement(), result.value?.announcement)
        assertEquals("text", result.value?.type)
    }

    @Test
    fun `parse instruction - should return an error for empty announcement`() {
        val voiceInstructions = Fixtures.emptyInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNull(result.value)
        assertNotNull(result.error)
    }

    @Test
    fun `parse instruction - should return an error for invalid announcement`() {
        val voiceInstructions = Fixtures.nullInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNull(result.value)
        assertNotNull(result.error)
    }

    @Test
    fun `parse announcement - should return SSML announcement`() {
        val ssmlAnnouncement = "Turn left"
        val speechAnnouncement = SpeechAnnouncement.Builder("Turn right")
            .ssmlAnnouncement(ssmlAnnouncement)
            .build()

        val result = VoiceInstructionsParser.parse(speechAnnouncement)

        assertNotNull(result.value)
        assertEquals(ssmlAnnouncement, result.value?.announcement)
        assertEquals("ssml", result.value?.type)
    }

    @Test
    fun `parse announcement - should return TEXT announcement`() {
        val announcement = "Turn left"
        val speechAnnouncement = SpeechAnnouncement.Builder(announcement).build()

        val result = VoiceInstructionsParser.parse(speechAnnouncement)

        assertNotNull(result.value)
        assertEquals(announcement, result.value?.announcement)
        assertEquals("text", result.value?.type)
    }

    @Test
    fun `parse announcement - should return an error for empty announcement`() {
        val announcement = SpeechAnnouncement.Builder("").build()

        val result = VoiceInstructionsParser.parse(announcement)

        assertNull(result.value)
        assertNotNull(result.error)
    }
}
