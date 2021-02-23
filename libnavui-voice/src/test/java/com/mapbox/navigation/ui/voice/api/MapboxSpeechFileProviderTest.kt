package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class MapboxSpeechFileProviderTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun delete() = coroutineRule.runBlockingTest {
        val mockedCacheDir: File = mockk(relaxed = true)
        val mapboxSpeechFileProvider = MapboxSpeechFileProvider(mockedCacheDir)
        val mockedFile: File = mockk(relaxed = true)

        mapboxSpeechFileProvider.delete(mockedFile)

        coVerify(exactly = 1) {
            mockedFile.delete()
        }
    }
}
