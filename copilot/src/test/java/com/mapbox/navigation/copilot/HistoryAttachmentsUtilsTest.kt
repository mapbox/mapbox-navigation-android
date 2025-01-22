package com.mapbox.navigation.copilot

import android.util.Base64
import com.mapbox.navigation.copilot.internal.CopilotSession
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64.getDecoder

/**
 * HistoryAttachmentsUtilsTest
 *
 * NOTE FOR FUTURE SECURITY AUDITS:
 * The fakeAccessToken used below in the tests, although it seems legitimate it is a
 * fake one manually generated so that the owner associated to is is copilot-test-owner.
 */
class HistoryAttachmentsUtilsTest {

    @Test
    fun `generate filename`() {
        val navigationSession = CopilotSession(
            appMode = "mbx-debug",
            driveMode = "free-drive",
            driveId = "3e48fd7a-fc82-42a8-9bae-baeb724f92ce",
            startedAt = "2022-05-12T17:47:42.353Z",
            endedAt = "2022-05-12T17:48:12.504Z",
            navSdkVersion = "2.7.0-beta.1",
            navNativeSdkVersion = "108.0.1",
            appVersion = "v0.108.0-9-g0527ee4",
            appUserId = "wBzYwfK0oCYMTNYPIFHhYuYOLLs1",
            appSessionId = "3e48fd7b-ac82-42a8-9abe-aaeb724f92ce",
        )

        val filename = HistoryAttachmentsUtils.attachmentFilename(navigationSession)

        val expectedFilename = "2022-05-12T17:47:42.353Z__2022-05-12T17:48:12.504Z__android__" +
            "2.7.0-beta.1__108.0.1_____v0.108.0-9-g0527ee4__" +
            "wBzYwfK0oCYMTNYPIFHhYuYOLLs1__3e48fd7b-ac82-42a8-9abe-aaeb724f92ce.pbf.gz"
        assertEquals(expectedFilename, filename)
    }

    @Test
    fun `generate session id`() {
        val navigationSession = CopilotSession(
            appMode = "mbx-debug",
            driveMode = "free-drive",
            driveId = "3e48fd7a-fc82-42a8-9bae-baeb724f92ce",
            startedAt = "2022-05-12T17:47:42.353Z",
            endedAt = "2022-05-12T17:48:12.504Z",
            navSdkVersion = "2.7.0-beta.1",
            navNativeSdkVersion = "108.0.1",
            appVersion = "v0.108.0-9-g0527ee4",
            appUserId = "wBzYwfK0oCYMTNYPIFHhYuYOLLs1",
            appSessionId = "3e48fd7b-ac82-42a8-9abe-aaeb724f92ce",
            owner = "owner",
        )

        val sessionId = HistoryAttachmentsUtils.generateSessionId(navigationSession)

        val expectedSessionId = "co-pilot/owner/1.2/mbx-debug/-/-/free-drive/" +
            "-/3e48fd7a-fc82-42a8-9bae-baeb724f92ce"
        assertEquals(expectedSessionId, sessionId)
    }

    @Test
    fun decode() {
        mockkStatic(Base64::class) {
            every { Base64.decode(any<String>(), any()) } answers {
                getDecoder().decode(firstArg<String>())
            }
            val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
            val owner = HistoryAttachmentsUtils.retrieveOwnerFrom(fakeAccessToken)
            assertEquals("copilot-test-owner", owner)
        }
    }
}
