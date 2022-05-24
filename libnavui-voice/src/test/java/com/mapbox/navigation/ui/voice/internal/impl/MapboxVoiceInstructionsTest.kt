package com.mapbox.navigation.ui.voice.internal.impl

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxVoiceInstructionsTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private val carAppVoiceInstructions = MapboxVoiceInstructions()

    @Test
    fun `should emit voice instruction`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(mockk(), mockk())
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }
        val observerSlot = slot<VoiceInstructionsObserver>()
        every {
            mapboxNavigation.registerVoiceInstructionsObserver(capture(observerSlot))
        } just Runs
        val voiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "Left on Broadway"
        }

        carAppVoiceInstructions.registerObservers(mapboxNavigation)
        val flow = carAppVoiceInstructions.voiceInstructions()
        val initialInstruction = flow.first()
        observerSlot.captured.onNewVoiceInstructions(voiceInstructions)
        val updatedInstruction = flow.first()

        // voiceInstructionsFlow has null voiceInstruction as initial state
        assertTrue(initialInstruction.isPlayable)
        assertEquals(null, initialInstruction.voiceInstructions)
        assertEquals("Left on Broadway", updatedInstruction.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit multiple voice instructions`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(mockk(), mockk())
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val observerSlot = slot<VoiceInstructionsObserver>()
        val firstVoiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "Left on Broadway"
        }
        val secondVoiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "Right on Pennsylvania"
        }
        every {
            mapboxNavigation.registerVoiceInstructionsObserver(capture(observerSlot))
        } just Runs

        val flow = carAppVoiceInstructions.voiceInstructions()
        val initialAnnouncement = flow.first().voiceInstructions?.announcement()

        carAppVoiceInstructions.registerObservers(mapboxNavigation)

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)
        val firstAnnouncement = flow.first().voiceInstructions?.announcement()

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)
        val secondAnnouncement = flow.first().voiceInstructions?.announcement()

        // voiceInstructionsFlow has null voiceInstruction as initial state
        assertEquals(null, initialAnnouncement)
        assertEquals("Left on Broadway", firstAnnouncement)
        assertEquals("Right on Pennsylvania", secondAnnouncement)
    }

    @Test
    fun `should emit null routes is empty`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val state = carAppVoiceInstructions.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit null when session is stopped`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STOPPED
            )
        }

        val state = carAppVoiceInstructions.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit voice language from the first route`() = coroutineRule.runBlockingTest {
        val language = "de"
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(
                    createRoute(language),
                    createRoute(voiceLanguage = "en")
                )
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val flow = carAppVoiceInstructions.voiceLanguage()
        val initialInstruction = flow.first()
        carAppVoiceInstructions.registerObservers(mapboxNavigation)
        val updatedInstruction = flow.first()

        // routesFlow() on start sends empty list to disable sound button  in FreeDrive
        assertEquals(null, initialInstruction)
        assertEquals(language, updatedInstruction)
    }

    @Test
    fun `should emit null voice language when routes is empty`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        assertNull(carAppVoiceInstructions.voiceLanguage().first())
    }

    @Test
    fun `should emit null voice language before routes are updated`() =
        coroutineRule.runBlockingTest {
            every { mapboxNavigation.registerRoutesObserver(any()) } just Runs

            assertNull(carAppVoiceInstructions.voiceLanguage().first())
        }

    private fun createRoute(voiceLanguage: String): NavigationRoute {
        return mockk {
            every { directionsRoute } returns mockk {
                every { voiceLanguage() } returns voiceLanguage
            }
        }
    }
}
