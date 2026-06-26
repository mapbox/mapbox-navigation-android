package com.mapbox.navigation.voice.api

import android.media.MediaPlayer
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechVolume
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInstructionsFilePlayerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mockedMediaPlayer: MediaPlayer = mockk(relaxUnitFun = true)
    private val mockedFileInputStream: FileInputStream = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        mockkObject(MediaPlayerProvider)
        every { MediaPlayerProvider.retrieveMediaPlayer() } returns mockedMediaPlayer
        mockkObject(FileInputStreamProvider)
        every {
            FileInputStreamProvider.retrieveFileInputStream(any())
        } returns mockedFileInputStream
        mockkObject(InternalJobControlFactory)
        val defaultJob = SupervisorJob()
        every { InternalJobControlFactory.createMainScopeJobControl() } answers {
            JobControl(defaultJob, CoroutineScope(defaultJob + UnconfinedTestDispatcher()))
        }
    }

    @After
    fun tearDown() {
        unmockkObject(MediaPlayerProvider)
        unmockkObject(FileInputStreamProvider)
        unmockkObject(InternalJobControlFactory)
    }

    @Test(expected = IllegalStateException::class)
    fun `only one announcement can be played at a time`() = runTest {
        val filePlayer = initFilePlayer()
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.currentPlay = anyAnnouncement

        filePlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)
    }

    @Test
    fun `announcement file from state can't be null media player is released`() = runTest {
        val filePlayer = initFilePlayer()
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify { mockedMediaPlayer.release() }
    }

    @Test
    fun `announcement file from state can't be null media player is null`() = runTest {
        val filePlayer = initFilePlayer()
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `announcement file from state can't be null current play is null`() = runTest {
        val filePlayer = initFilePlayer()
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `announcement file from state can't be null onDone is called`() = runTest {
        val filePlayer = initFilePlayer()
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify { anyVoiceInstructionsPlayerCallback.onDone(announcementWithNullFile) }
    }

    @Test
    fun `announcement file not accessible media player is released`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        every {
            FileInputStreamProvider.retrieveFileInputStream(any())
        } throws FileNotFoundException()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { mockedMediaPlayer.release() }
    }

    @Test
    fun `announcement file not accessible media player is null`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        every {
            FileInputStreamProvider.retrieveFileInputStream(any())
        } throws FileNotFoundException()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `announcement file not accessible current play is null`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        every {
            FileInputStreamProvider.retrieveFileInputStream(any())
        } throws FileNotFoundException()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `announcement file not accessible onDone is called`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        every {
            FileInputStreamProvider.retrieveFileInputStream(any())
        } throws FileNotFoundException()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { anyVoiceInstructionsPlayerCallback.onDone(announcement) }
    }

    @Test
    fun `player attributes applyOn media player is called when play`() = runTest {
        val playerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer = initFilePlayer(playerAttributes)
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { playerAttributes.applyOn(filePlayer.mediaPlayer!!) }
    }

    @Test
    fun `media player prepareAsync is called when play`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { filePlayer.mediaPlayer!!.prepareAsync() }
    }

    @Test
    fun `media player setVolume with default level is called when play`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { filePlayer.mediaPlayer!!.setVolume(1.0f, 1.0f) }
    }

    @Test
    fun `media player setOnErrorListener is added when play`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { filePlayer.mediaPlayer!!.setOnErrorListener(any()) }
    }

    @Test
    fun `media player setOnPreparedListener is added when play`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { filePlayer.mediaPlayer!!.setOnPreparedListener(any()) }
    }

    @Test
    fun `media player setOnCompletionListener is added when play`() = runTest {
        val filePlayer = initFilePlayer()
        val announcement = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcement.file } returns mockk()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcement, anyVoiceInstructionsPlayerCallback)

        verify { filePlayer.mediaPlayer!!.setOnCompletionListener(any()) }
    }

    @Test
    fun `volume level is updated when volume`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))
        val aSpeechVolume = SpeechVolume(0.5f)

        filePlayer.volume(aSpeechVolume)

        assertEquals(0.5f, filePlayer.volumeLevel)
    }

    @Test
    fun `media player setVolume is called when volume`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))
        val aSpeechVolume = SpeechVolume(0.5f)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.volume(aSpeechVolume)

        verify { mockedMediaPlayer.setVolume(0.5f, 0.5f) }
    }

    @Test
    fun `media player release is called when clear`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.clear()

        verify { mockedMediaPlayer.release() }
    }

    @Test
    fun `media player is null after clear`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))

        filePlayer.clear()

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `current play is null after clear`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))

        filePlayer.clear()

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `media player release is called when shutdown`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.shutdown()

        verify { mockedMediaPlayer.release() }
    }

    @Test
    fun `media player is null after shutdown`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))

        filePlayer.shutdown()

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `current play is null after shutdown`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))

        filePlayer.shutdown()

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `volume level is reset to default after shutdown`() {
        val filePlayer = VoiceInstructionsFilePlayer(mockk(relaxUnitFun = true))
        filePlayer.volumeLevel = 0.5f

        filePlayer.shutdown()

        assertEquals(1.0f, filePlayer.volumeLevel)
    }

    private fun TestScope.initFilePlayer(
        playerAttributes: VoiceInstructionsPlayerAttributes = mockk(relaxUnitFun = true),
    ): VoiceInstructionsFilePlayer {
        val jobScope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher())
        every {
            InternalJobControlFactory.createMainScopeJobControl()
        } answers {
            JobControl(jobScope.coroutineContext.job, jobScope)
        }
        return VoiceInstructionsFilePlayer(
            playerAttributes,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}
