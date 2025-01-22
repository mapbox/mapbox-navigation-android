package com.mapbox.navigation.voice.impl

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.voice.internal.MapboxVoiceInstructions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxVoiceInstructionsTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private val sut = MapboxVoiceInstructions()

    @Test
    fun `should emit voice instruction`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
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

        sut.registerObservers(mapboxNavigation)
        val flow = sut.voiceInstructions()
        val initialInstruction = flow.first()
        observerSlot.captured.onNewVoiceInstructions(voiceInstructions)
        val updatedInstruction = flow.first()

        // voiceInstructionsFlow has null voiceInstruction as initial state
        assertTrue(initialInstruction.isPlayable)
        assertEquals(null, initialInstruction.voiceInstructions)
        assertEquals("Left on Broadway", updatedInstruction.voiceInstructions?.announcement())
        assertEquals(true, updatedInstruction.isFirst)
    }

    @Test
    fun `should emit multiple voice instructions`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
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

        val flow = sut.voiceInstructions()
        val initialAnnouncement = flow.first().voiceInstructions?.announcement()

        sut.registerObservers(mapboxNavigation)

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)
        val firstState = flow.first()

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)
        val secondState = flow.first()

        // voiceInstructionsFlow has null voiceInstruction as initial state
        assertEquals(null, initialAnnouncement)
        assertEquals("Left on Broadway", firstState.voiceInstructions?.announcement())
        assertEquals(true, firstState.isFirst)
        assertEquals("Right on Pennsylvania", secondState.voiceInstructions?.announcement())
        assertEquals(false, secondState.isFirst)
    }

    @Test
    fun `should emit null routes is empty`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val state = sut.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit null when session is stopped`() = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STOPPED,
            )
        }

        val state = sut.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit voice language from the first route`() = coroutineRule.runBlockingTest {
        val language = "de"
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(
                    createRoute(language),
                    createRoute(voiceLanguage = "en"),
                )
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            }
            firstArg<RoutesObserver>().onRoutesChanged(result)
        }

        val flow = sut.voiceLanguage()
        val initialInstruction = flow.first()
        sut.registerObservers(mapboxNavigation)
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

        assertNull(sut.voiceLanguage().first())
    }

    @Test
    fun `should emit null voice language before routes are updated`() =
        coroutineRule.runBlockingTest {
            every { mapboxNavigation.registerRoutesObserver(any()) } just Runs

            assertNull(sut.voiceLanguage().first())
        }

    @Test
    fun `should reset voiceInstructions when empty routes are set`() = runBlockingTest {
        val firstVoiceInstructions = mockk<VoiceInstructions>()
        val routesObserver = slot<RoutesObserver>()
        val instructionsObserver = slot<VoiceInstructionsObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture((routesObserver))) } returns Unit
        every {
            mapboxNavigation.registerVoiceInstructionsObserver(capture(instructionsObserver))
        } returns Unit
        sut.registerObservers(mapboxNavigation)

        instructionsObserver.captured.onNewVoiceInstructions(firstVoiceInstructions)
        val firstState = sut.voiceInstructions().first()
        routesObserver.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns emptyList()
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
            },
        )
        val secondState = sut.voiceInstructions().first()

        assertNotNull(firstState.voiceInstructions)
        assertNull(secondState.voiceInstructions)
    }

    @Test
    fun `should reset voiceInstructions when TripSession is STOPPED`() = runBlockingTest {
        val firstVoiceInstructions = mockk<VoiceInstructions>()
        val instructionsObserver = slot<VoiceInstructionsObserver>()
        val tripSessionObserver = slot<TripSessionStateObserver>()
        every {
            mapboxNavigation.registerVoiceInstructionsObserver(capture(instructionsObserver))
        } returns Unit
        every {
            mapboxNavigation.registerTripSessionStateObserver(capture(tripSessionObserver))
        } returns Unit
        sut.registerObservers(mapboxNavigation)

        instructionsObserver.captured.onNewVoiceInstructions(firstVoiceInstructions)
        val firstState = sut.voiceInstructions().first()
        tripSessionObserver.captured.onSessionStateChanged(TripSessionState.STOPPED)
        val secondState = sut.voiceInstructions().first()

        assertNotNull(firstState.voiceInstructions)
        assertNull(secondState.voiceInstructions)
    }

    @Test
    fun `voiceInstruction should be first after routes changed to new`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertTrue(flow.first().isFirst)
    }

    @Test
    fun `voiceInstruction should be first after routes changed due to reroute`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
            },
        )

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertTrue(flow.first().isFirst)
    }

    @Test
    fun `voiceInstruction should not be first after routes changed to alternatives`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE
            },
        )

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertFalse(flow.first().isFirst)
    }

    @Test
    fun `voiceInstruction should not be first after routes changed due to refresh`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
            },
        )

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertFalse(flow.first().isFirst)
    }

    @Test
    fun `voiceInstruction should not be first after routes changed due to clean up`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns emptyList()
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
            },
        )

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertFalse(flow.first().isFirst)
    }

    @Test
    fun `voiceInstruction should be first after onDetached`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)

        sut.unregisterObservers(mapboxNavigation)
        sut.registerObservers(mapboxNavigation)

        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)

        assertTrue(flow.first().isFirst)
    }

    @Test
    fun `same voiceInstruction should not be first`() = runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED,
            )
        }
        val routesObserverSlot = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) } just Runs

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

        val flow = sut.voiceInstructions()

        sut.registerObservers(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
                every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
            },
        )

        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)
        observerSlot.captured.onNewVoiceInstructions(secondVoiceInstructions)
        observerSlot.captured.onNewVoiceInstructions(firstVoiceInstructions)
        val newState = flow.first()

        assertFalse(newState.isFirst)
    }

    private fun createRoute(voiceLanguage: String): NavigationRoute {
        return mockk {
            every { directionsRoute } returns mockk {
                every { voiceLanguage() } returns voiceLanguage
            }
        }
    }
}
