package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class DeveloperMetadataAggregatorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val sessionIdFlow = MutableSharedFlow<String>(replay = 1)
    private val observer = mockk<DeveloperMetadataObserver>(relaxed = true)
    private lateinit var sut: DeveloperMetadataAggregator

    @Before
    fun setUp() {
        sut = DeveloperMetadataAggregator(sessionIdFlow, coroutineRule.coroutineScope)
    }

    @Test
    fun `observer receives saved value`() = coroutineRule.runBlockingTest {
        val savedValue = "456-654"
        sessionIdFlow.tryEmit(savedValue)

        sut.registerObserver(observer)

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(savedValue))
        }
    }

    @Test
    fun `observer receives changed value`() = coroutineRule.runBlockingTest {
        val newValue = "456-654"

        sut.registerObserver(observer)
        clearMocks(observer)
        sessionIdFlow.tryEmit(newValue)

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }
    }

    @Test
    fun `observer does not receive changed value after unregister`() =
        coroutineRule.runBlockingTest {
            val newValue = "456-654"
            sut.registerObserver(observer)
            clearMocks(observer)

            sut.unregisterObserver(observer)
            sessionIdFlow.tryEmit(newValue)

            verify(exactly = 0) {
                observer.onDeveloperMetadataChanged(any())
            }
        }

    @Test
    fun unregisterAllObservers() = coroutineRule.runBlockingTest {
        val newValue = "456-654"
        val secondObserver = mockk<DeveloperMetadataObserver>(relaxed = true)
        sut.registerObserver(observer)
        sut.registerObserver(secondObserver)
        sessionIdFlow.tryEmit(newValue)

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }
        verify(exactly = 1) {
            secondObserver.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }

        sut.unregisterAllObservers()
        clearMocks(observer, secondObserver)
        sessionIdFlow.tryEmit("789-987")

        verify(exactly = 0) {
            observer.onDeveloperMetadataChanged(any())
        }
        verify(exactly = 0) {
            secondObserver.onDeveloperMetadataChanged(any())
        }
    }

    @Test
    fun `observer unregisters itself from callback`() {
        val newId = "456-654"
        val observer = object : DeveloperMetadataObserver {
            override fun onDeveloperMetadataChanged(metadata: DeveloperMetadata) {
                if (metadata.copilotSessionId == newId) {
                    sut.unregisterObserver(this)
                }
            }
        }
        sut.registerObserver(observer)

        sessionIdFlow.tryEmit(newId)
        // verify no crash
    }
}
