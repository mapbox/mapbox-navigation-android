package com.mapbox.navigation.voice.testutils

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.voice.model.TypeAndAnnouncement
import io.mockk.every
import io.mockk.mockk
import java.util.Date

internal object Fixtures {
    val ssmlAnnouncementString = """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Frederick Road, Maryland 3 55.</prosody>
                </amazon:effect>
            </speak>
    """.trimIndent()

    val textAnnouncementString = "Turn right onto Frederick Road, Maryland 3 55."

    fun ssmlInstructions(): VoiceInstructions = mockk {
        every { ssmlAnnouncement() } returns ssmlAnnouncementString
        every { announcement() } returns textAnnouncementString
    }

    fun textInstructions(): VoiceInstructions = mockk {
        every { ssmlAnnouncement() } returns ""
        every { announcement() } returns textAnnouncementString
    }

    fun emptyInstructions(): VoiceInstructions = mockk {
        every { ssmlAnnouncement() } returns "  "
        every { announcement() } returns "  "
    }

    fun nullInstructions(): VoiceInstructions = mockk {
        every { ssmlAnnouncement() } returns null
        every { announcement() } returns null
    }

    fun ssmlAnnouncement(): TypeAndAnnouncement =
        TypeAndAnnouncement("ssml", ssmlAnnouncementString)

    fun textAnnouncement(): TypeAndAnnouncement =
        TypeAndAnnouncement("text", textAnnouncementString)

    fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): DataRef = blob.toDataRef()
    }

    fun resourceLoadResult(
        data: ResourceData?,
        status: ResourceLoadStatus,
        immutable: Boolean = false,
        mustRevalidate: Boolean = false,
        expires: Date = Date(),
        totalBytes: Long = 0,
        transferredBytes: Long = 0,
        contentType: String = "audio/mp3",
        etag: String = "",
        belongsToGroup: Boolean = false,
    ): ResourceLoadResult {
        return ResourceLoadResult(
            data,
            status,
            immutable,
            mustRevalidate,
            expires,
            totalBytes,
            transferredBytes,
            contentType,
            etag,
            belongsToGroup,
        )
    }
}
