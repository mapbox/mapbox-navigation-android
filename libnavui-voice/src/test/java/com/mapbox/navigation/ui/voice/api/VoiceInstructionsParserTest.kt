package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.voice.testutils.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceInstructionsParserTest {

    @Test
    fun `parse - should return SSML announcement`() {
        val voiceInstructions = Fixtures.ssmlInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNotNull(result.value)
        assertEquals(voiceInstructions.ssmlAnnouncement(), result.value?.announcement)
        assertEquals("ssml", result.value?.type)
    }

    @Test
    fun `parse - should return TEXT announcement`() {
        val voiceInstructions = Fixtures.textInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNotNull(result.value)
        assertEquals(voiceInstructions.announcement(), result.value?.announcement)
        assertEquals("text", result.value?.type)
    }

    @Test
    fun `parse - should return an error for empty announcement`() {
        val voiceInstructions = Fixtures.emptyInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNull(result.value)
        assertNotNull(result.error)
    }

    @Test
    fun `parse - should return an error for invalid announcement`() {
        val voiceInstructions = Fixtures.nullInstructions()

        val result = VoiceInstructionsParser.parse(voiceInstructions)

        assertNull(result.value)
        assertNotNull(result.error)
    }
}
