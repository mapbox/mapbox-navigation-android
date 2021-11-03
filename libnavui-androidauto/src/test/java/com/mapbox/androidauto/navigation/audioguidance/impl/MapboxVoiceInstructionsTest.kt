package com.mapbox.androidauto.navigation.audioguidance.impl

import com.mapbox.androidauto.testing.MainCoroutineRule
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

@ExperimentalCoroutinesApi
class MapboxVoiceInstructionsTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private val carAppVoiceInstructions = MapboxVoiceInstructions(mapboxNavigation)

    @Test
    fun `should emit voice instruction`() = coroutineRule.runTest {
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
        every { mapboxNavigation.registerVoiceInstructionsObserver(any()) } answers {
            val voiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Left on Broadway"
            }
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(voiceInstructions)
        }

        val state = carAppVoiceInstructions.voiceInstructions().take(3).toList()

        assertFalse(state[0].isPlayable) // routesFlow() on start sends empty list to disable sound button  in FreeDrive
        assertEquals(null, state[0].voiceInstructions)
        assertTrue(state[1].isPlayable) // voiceInstructionsFlow() sends null voiceInstruction before observer is fired
        assertEquals("Left on Broadway", state[2].voiceInstructions?.announcement())
    }

    @Test
    fun `should emit multiple voice instructions`() = coroutineRule.runTest {
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
        every { mapboxNavigation.registerVoiceInstructionsObserver(any()) } answers {
            val firstVoiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Left on Broadway"
            }
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(firstVoiceInstructions)
            val secondVoiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Right on Pennsylvania"
            }
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(secondVoiceInstructions)
        }

        val voiceInstruction = carAppVoiceInstructions.voiceInstructions().take(4).toList()

        val actual = voiceInstruction.map { it.voiceInstructions?.announcement() }
        assertEquals(null, actual[0]) // routesFlow() on start sends empty list to disable sound button  in FreeDrive
        assertEquals(null, actual[1]) // voiceInstructionsFlow() sends null voiceInstruction before observer is fired
        assertEquals("Left on Broadway", actual[2])
        assertEquals("Right on Pennsylvania", actual[3])
    }

    @Test
    fun `should emit null routes is empty`() = coroutineRule.runTest {
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
    fun `should emit null when session is stopped`() = coroutineRule.runTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STOPPED
            )
        }

        val state = carAppVoiceInstructions.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit voice language from the first route`() = coroutineRule.runTest {
        val language = "de"
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(createRoute(language), createRoute(voiceLanguage = "en"))
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val state = carAppVoiceInstructions.voiceLanguage().take(3).toList()

        assertEquals(null, state[0]) // routesFlow() on start sends empty list to disable sound button  in FreeDrive
        assertEquals(null, state[1]) // voiceInstructionsFlow() sends null voiceInstruction before observer is fired
        assertEquals(language, state[2])
    }

    @Test
    fun `should emit null voice language when routes is empty`() = coroutineRule.runTest {
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        assertNull(carAppVoiceInstructions.voiceLanguage().first())
    }

    @Test
    fun `should emit null voice language before routes are updated`() = coroutineRule.runTest {
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
