package com.mapbox.navigation.voice.api

import android.content.Context
import android.media.MediaPlayer
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechVolume
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream

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
    }

    @After
    fun tearDown() {
        unmockkObject(MediaPlayerProvider)
        unmockkObject(FileInputStreamProvider)
    }

    @Test(expected = IllegalStateException::class)
    fun `only one announcement can be played at a time`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.currentPlay = anyAnnouncement

        filePlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)
    }

    @Test
    fun `announcement file from state can't be null media player is released`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            mockedMediaPlayer.release()
        }
    }

    @Test
    fun `announcement file from state can't be null media player is null`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `announcement file from state can't be null current play is null`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `announcement file from state can't be null onDone is called`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        every { announcementWithNullFile.file } returns null
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            anyVoiceInstructionsPlayerCallback.onDone(announcementWithNullFile)
        }
    }

    @Test
    fun `announcement file from state needs to be accessible media player is released`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>()
        every { mockedFile.canRead() } returns false
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            mockedMediaPlayer.release()
        }
    }

    @Test
    fun `announcement file from state needs to be accessible media player is null`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>()
        every { mockedFile.canRead() } returns false
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `announcement file from state needs to be accessible current play is null`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>()
        every { mockedFile.canRead() } returns false
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `announcement file from state needs to be accessible onDone is called`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>()
        every { mockedFile.canRead() } returns false
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            anyVoiceInstructionsPlayerCallback.onDone(announcementWithNullFile)
        }
    }

    @Test
    fun `player attributes applyOn media player is called when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            anyPlayerAttributes.applyOn(filePlayer.mediaPlayer!!)
        }
    }

    @Test
    fun `media player prepareAsync is called when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            filePlayer.mediaPlayer!!.prepareAsync()
        }
    }

    @Test
    fun `media player setVolume with default level is called when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            filePlayer.mediaPlayer!!.setVolume(1.0f, 1.0f)
        }
    }

    @Test
    fun `media player setOnErrorListener is added when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            filePlayer.mediaPlayer!!.setOnErrorListener(any())
        }
    }

    @Test
    fun `media player setOnPreparedListener is added when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithNullFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithNullFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithNullFile, anyVoiceInstructionsPlayerCallback)

        verify {
            filePlayer.mediaPlayer!!.setOnPreparedListener(any())
        }
    }

    @Test
    fun `media player setOnCompletionListener is added when play`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val announcementWithAnyFile = mockk<SpeechAnnouncement>(relaxed = true)
        val mockedFile = mockk<File>(relaxed = true)
        every { mockedFile.canRead() } returns true
        every { announcementWithAnyFile.file } returns mockedFile
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val mockedFileDescriptor = mockk<FileDescriptor>()
        every { mockedFileInputStream.fd } returns mockedFileDescriptor

        filePlayer.play(announcementWithAnyFile, anyVoiceInstructionsPlayerCallback)

        verify {
            filePlayer.mediaPlayer!!.setOnCompletionListener(any())
        }
    }

    @Test
    fun `volume level is updated when volume`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val aSpeechVolume = SpeechVolume(0.5f)

        filePlayer.volume(aSpeechVolume)

        assertEquals(0.5f, filePlayer.volumeLevel)
    }

    @Test
    fun `media player setVolume is called when volume`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        val aSpeechVolume = SpeechVolume(0.5f)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.volume(aSpeechVolume)

        verify {
            mockedMediaPlayer.setVolume(0.5f, 0.5f)
        }
    }

    @Test
    fun `media player release is called when clear`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.clear()

        verify {
            mockedMediaPlayer.release()
        }
    }

    @Test
    fun `media player is null after clear`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)

        filePlayer.clear()

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `current play is null after clear`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)

        filePlayer.clear()

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `media player release is called when shutdown`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        filePlayer.mediaPlayer = mockedMediaPlayer

        filePlayer.shutdown()

        verify {
            mockedMediaPlayer.release()
        }
    }

    @Test
    fun `media player is null after shutdown`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)

        filePlayer.shutdown()

        assertEquals(null, filePlayer.mediaPlayer)
    }

    @Test
    fun `current play is null after shutdown`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)

        filePlayer.shutdown()

        assertEquals(null, filePlayer.currentPlay)
    }

    @Test
    fun `volume level is reset to default after shutdown`() {
        val anyContext = mockk<Context>()
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val filePlayer =
            VoiceInstructionsFilePlayer(anyContext, anyPlayerAttributes)
        filePlayer.volumeLevel = 0.5f

        filePlayer.shutdown()

        assertEquals(1.0f, filePlayer.volumeLevel)
    }
}
