package com.mapbox.navigation.voice.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import java.io.File

class SpeechAnnouncementTest : BuilderTest<SpeechAnnouncement, SpeechAnnouncement.Builder>() {

    override fun getImplementationClass() = SpeechAnnouncement::class

    override fun getFilledUpBuilder(): SpeechAnnouncement.Builder {
        val anAnnouncement = "Turn right onto Anza Street."
        val aSsmlAnnouncement = """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Anza Street.</prosody>
                </amazon:effect>
            </speak>
        """.trimIndent()
        val aMockedFile: File = mockk()
        return SpeechAnnouncement.Builder(anAnnouncement)
            .ssmlAnnouncement(aSsmlAnnouncement)
            .file(aMockedFile)
    }

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
