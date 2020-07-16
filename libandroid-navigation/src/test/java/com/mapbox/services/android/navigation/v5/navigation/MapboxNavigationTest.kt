package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigator.Navigator
import com.mapbox.services.android.navigation.v5.BaseTest
import com.mapbox.services.android.navigation.v5.internal.navigation.FreeDriveLocationUpdater
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigator
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.BANNER_INSTRUCTION_MILESTONE_ID
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID
import com.mapbox.services.android.navigation.v5.offroute.OffRoute
import com.mapbox.services.android.navigation.v5.snap.Snap
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MapboxNavigationTest : BaseTest() {

    @Test
    fun sanityTest() {
        val navigation = buildMapboxNavigation()

        assertNotNull(navigation)
    }

    @Test
    fun sanityTestWithOptions() {
        val options = MapboxNavigationOptions.Builder().build()
        val navigationWithOptions = buildMapboxNavigationWith(options)

        assertNotNull(navigationWithOptions)
    }

    @Test
    fun voiceInstructionMilestone_onInitializationDoesGetAdded() {
        val navigation = buildMapboxNavigation()

        val identifier = searchForVoiceInstructionMilestone(navigation)

        assertEquals(VOICE_INSTRUCTION_MILESTONE_ID, identifier)
    }

    @Test
    fun bannerInstructionMilestone_onInitializationDoesGetAdded() {
        val navigation = buildMapboxNavigation()

        val identifier = searchForBannerInstructionMilestone(navigation)

        assertEquals(BANNER_INSTRUCTION_MILESTONE_ID, identifier)
    }

    @Test
    fun defaultMilestones_onInitializationDoNotGetAdded() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)

        assertEquals(0, navigationWithOptions.milestones.size)
    }

    @Test
    fun defaultEngines_offRouteEngineDidGetInitialized() {
        val navigation = buildMapboxNavigation()

        assertNotNull(navigation.offRouteEngine)
    }

    @Test
    fun defaultEngines_snapEngineDidGetInitialized() {
        val navigation = buildMapboxNavigation()

        assertNotNull(navigation.snapEngine)
    }

    @Test
    fun addMilestone_milestoneDidGetAdded() {
        val navigation = buildMapboxNavigation()
        val milestone = StepMilestone.Builder().build()

        navigation.addMilestone(milestone)

        assertTrue(navigation.milestones.contains(milestone))
    }

    @Test
    fun addMilestone_milestoneOnlyGetsAddedOnce() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)

        val milestone = StepMilestone.Builder().build()
        navigationWithOptions.addMilestone(milestone)
        navigationWithOptions.addMilestone(milestone)

        assertEquals(1, navigationWithOptions.milestones.size)
    }

    @Test
    fun removeMilestone_milestoneDidGetRemoved() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)

        val milestone = StepMilestone.Builder().build()
        navigationWithOptions.addMilestone(milestone)
        navigationWithOptions.removeMilestone(milestone)

        assertEquals(0, navigationWithOptions.milestones.size)
    }

    @Test
    fun removeMilestone_milestoneDoesNotExist() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)

        val milestone = StepMilestone.Builder().build()
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())
        navigationWithOptions.removeMilestone(milestone)

        assertEquals(1, navigationWithOptions.milestones.size)
    }

    @Test
    fun removeMilestone_nullRemovesAllMilestones() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())

        navigationWithOptions.removeMilestone(null)

        assertEquals(0, navigationWithOptions.milestones.size)
    }

    @Test
    fun removeMilestone_correctMilestoneWithIdentifierGetsRemoved() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)
        val removedMilestoneIdentifier = 5678
        val milestone = StepMilestone.Builder().setIdentifier(removedMilestoneIdentifier).build()
        navigationWithOptions.addMilestone(milestone)

        navigationWithOptions.removeMilestone(removedMilestoneIdentifier)

        assertEquals(0, navigationWithOptions.milestones.size)
    }

    @Test
    fun removeMilestone_noMilestoneWithIdentifierFound() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)
        navigationWithOptions.addMilestone(StepMilestone.Builder().build())
        val removedMilestoneIdentifier = 5678

        navigationWithOptions.removeMilestone(removedMilestoneIdentifier)

        assertEquals(1, navigationWithOptions.milestones.size)
    }

    @Test
    fun addMilestoneList_duplicateIdentifiersAreIgnored() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)
        val milestoneIdentifier = 5678
        val milestone = StepMilestone.Builder().setIdentifier(milestoneIdentifier).build()
        navigationWithOptions.addMilestone(milestone)
        val milestones = arrayListOf(milestone, milestone, milestone)

        navigationWithOptions.addMilestones(milestones)

        assertEquals(1, navigationWithOptions.milestones.size)
    }

    @Test
    fun addMilestoneList_allMilestonesAreAdded() {
        val options = MapboxNavigationOptions.Builder().defaultMilestonesEnabled(false).build()
        val navigationWithOptions = buildMapboxNavigationWith(options)
        val firstMilestoneId = 5678
        val secondMilestoneId = 5679
        val firstMilestone = StepMilestone.Builder().setIdentifier(firstMilestoneId).build()
        val secondMilestone = StepMilestone.Builder().setIdentifier(secondMilestoneId).build()
        val milestones = arrayListOf(firstMilestone, secondMilestone)

        navigationWithOptions.addMilestones(milestones)

        assertEquals(2, navigationWithOptions.milestones.size)
    }

    @Test
    fun getLocationEngine_returnsCorrectLocationEngine() {
        val navigation = buildMapboxNavigation()
        val locationEngine = mockk<LocationEngine>(relaxed = true)
        val locationEngineInstanceNotUsed = mockk<LocationEngine>(relaxed = true)

        navigation.locationEngine = locationEngine

        assertNotSame(locationEngineInstanceNotUsed, navigation.locationEngine)
        assertEquals(locationEngine, navigation.locationEngine)
    }

    @Test
    @Ignore
    fun startNavigation_doesSendTrueToNavigationEvent() {
        val navigation = buildMapboxNavigation()
        val navigationEventListener = mockk<NavigationEventListener>(relaxed = true)

        navigation.addNavigationEventListener(navigationEventListener)
        navigation.startNavigation(buildTestDirectionsRoute())

        verify(exactly = 1) { navigationEventListener.onRunning(true) }
    }

    @Test
    fun setSnapEngine_doesReplaceDefaultEngine() {
        val navigation = buildMapboxNavigation()

        val snap = mockk<Snap>(relaxed = true)
        navigation.snapEngine = snap

        assertTrue(navigation.snapEngine !is SnapToRoute)
    }

    @Test
    fun setOffRouteEngine_doesReplaceDefaultEngine() {
        val navigation = buildMapboxNavigation()

        val offRoute = mockk<OffRoute>(relaxed = true)
        navigation.offRouteEngine = offRoute

        assertEquals(offRoute, navigation.offRouteEngine)
    }

    @Test
    fun updateRouteLegIndex_negativeIndexIsIgnored() {
        val navigator = mockk<MapboxNavigator>(relaxed = true)
        val navigation = buildMapboxNavigationWith(navigator)
        val directionsRoute = buildTestDirectionsRoute()
        navigation.startNavigation(directionsRoute)

        val didUpdate = navigation.updateRouteLegIndex(-1)

        assertFalse(didUpdate)
    }

    @Test
    fun updateRouteLegIndex_invalidIndexIsIgnored() {
        val navigator = mockk<MapboxNavigator>(relaxed = true)
        val navigation = buildMapboxNavigationWith(navigator)
        val directionsRoute = buildTestDirectionsRoute()
        navigation.startNavigation(directionsRoute)

        val didUpdate = navigation.updateRouteLegIndex(100)

        assertFalse(didUpdate)
    }

    @Test
    fun updateRouteLegIndex_validIndexIsUpdated() {
        val navigator = mockk<MapboxNavigator>(relaxed = true)
        val navigation = buildMapboxNavigationWith(navigator)
        val directionsRoute = buildTestDirectionsRoute()
        navigation.startNavigation(directionsRoute)
        val legIndex = 0

        val didUpdate = navigation.updateRouteLegIndex(legIndex)

        assertTrue(didUpdate)
    }

    @Test
    fun updateLocationEngine_engineIsSet() {
        val locationEngine = mockk<LocationEngine>(relaxed = true)
        val navigation = buildMapboxNavigationWith(locationEngine)
        val newLocationEngine = mockk<LocationEngine>(relaxed = true)

        navigation.locationEngine = newLocationEngine

        val currentLocationEngine = navigation.locationEngine
        assertNotSame(locationEngine, currentLocationEngine)
    }

    @Test
    fun updateLocationEngine_freeDriveLocationUpdaterUpdateLocationEngineIsCalled() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val anotherLocationEngine = mockk<LocationEngine>(relaxed = true)

        navigation.locationEngine = anotherLocationEngine

        verify {
            mockedFreeDriveLocationUpdater.updateLocationEngine(eq(anotherLocationEngine))
        }
    }

    @Test
    fun updateLocationEngineRequest_freeDriveLocationUpdaterUpdateLocationEngineRequestIsCalled() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val anotherLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)

        navigation.setLocationEngineRequest(anotherLocationEngineRequest)

        verify {
            mockedFreeDriveLocationUpdater.updateLocationEngineRequest(
                eq(
                    anotherLocationEngineRequest
                )
            )
        }
    }

    @Test
    fun defaultLocationEngineRequest_createdOnInitialization() {
        val locationEngine = mockk<LocationEngine>(relaxed = true)
        val navigation = buildMapboxNavigationWith(locationEngine)

        val request = navigation.retrieveLocationEngineRequest()

        assertNotNull(request)
    }

    @Test
    fun stopNavigation_enableFreeDriveIsNotStartedIfFreeDriveNotEnabled() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )

        navigation.stopNavigation()

        verify(exactly = 0) {
            mockedFreeDriveLocationUpdater.configure(
                any<String>(),
                any<OnOfflineTilesConfiguredCallback>()
            )
        }
        verify(exactly = 0) {
            mockedFreeDriveLocationUpdater.start()
        }
    }

    @Test
    fun stopNavigation_enableFreeDriveIsStartedIfFreeDriveEnabled() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )

        navigation.enableFreeDrive()
        navigation.stopNavigation()

        verify(exactly = 2) {
            mockedFreeDriveLocationUpdater.configure(
                any<String>(),
                any<OnOfflineTilesConfiguredCallback>()
            )
        }
    }

    @Test
    fun enableFreeDrive_enableFreeDriveIsStarted() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val tilePath = slot<String>()

        navigation.enableFreeDrive()

        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.configure(
                capture(tilePath),
                any<OnOfflineTilesConfiguredCallback>()
            )
        }
        assertTrue(tilePath.captured.contains("2019_04_13-00_00_11"))
    }

    @Test
    fun enableFreeDrive_freeDriveLocationUpdaterStartIsCalledWhenOnConfigured() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val onOfflineTilesConfiguredCallback = slot<OnOfflineTilesConfiguredCallback>()

        navigation.enableFreeDrive()

        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.configure(
                any(),
                capture(onOfflineTilesConfiguredCallback)
            )
        }
        onOfflineTilesConfiguredCallback.captured.onConfigured()
        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.start()
        }
    }

    @Test
    fun enableFreeDrive_freeDriveLocationUpdaterStartIsCalledRightAwayIfAlreadyConfigured() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val onOfflineTilesConfiguredCallback = slot<OnOfflineTilesConfiguredCallback>()
        navigation.enableFreeDrive()
        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.configure(
                any(),
                capture(onOfflineTilesConfiguredCallback)
            )
        }
        onOfflineTilesConfiguredCallback.captured.onConfigured()

        navigation.enableFreeDrive()

        verify(exactly = 2) {
            mockedFreeDriveLocationUpdater.start()
        }
    }

    @Test
    fun disableFreeDrive_freeDriveLocationUpdaterStopIsCalledIfAlreadyConfigured() {
        val mockedFreeDriveLocationUpdater = mockk<FreeDriveLocationUpdater>(relaxed = true)
        val navigation = MapboxNavigation(
            RuntimeEnvironment.application,
            ACCESS_TOKEN,
            mockk<MapboxNavigationOptions>(relaxed = true),
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockedFreeDriveLocationUpdater
        )
        val onOfflineTilesConfiguredCallback = slot<OnOfflineTilesConfiguredCallback>()
        navigation.enableFreeDrive()
        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.configure(
                any(),
                capture(onOfflineTilesConfiguredCallback)
            )
        }
        onOfflineTilesConfiguredCallback.captured.onConfigured()
        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.start()
        }

        navigation.disableFreeDrive()

        verify(exactly = 1) {
            mockedFreeDriveLocationUpdater.stop()
        }
    }

    private fun buildMapboxNavigationWith(mapboxNavigator: MapboxNavigator): MapboxNavigation {
        val context = RuntimeEnvironment.application
        return MapboxNavigation(
            context,
            ACCESS_TOKEN,
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mapboxNavigator
        )
    }

    private fun buildMapboxNavigationWith(locationEngine: LocationEngine): MapboxNavigation {
        val context = RuntimeEnvironment.application
        return MapboxNavigation(
            context,
            ACCESS_TOKEN,
            mockk<NavigationTelemetry>(relaxed = true),
            locationEngine,
            mockk<MapboxNavigator>(relaxed = true)
        )
    }

    private fun buildMapboxNavigation(): MapboxNavigation {
        val context = RuntimeEnvironment.application
        return MapboxNavigation(
            context,
            ACCESS_TOKEN,
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<MapboxNavigator>(relaxed = true)
        )
    }

    private fun buildMapboxNavigationWith(options: MapboxNavigationOptions): MapboxNavigation {
        val context = RuntimeEnvironment.application
        return MapboxNavigation(
            context,
            ACCESS_TOKEN,
            options,
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockk<FreeDriveLocationUpdater>(relaxed = true)
        )
    }

    private fun searchForBannerInstructionMilestone(navigation: MapboxNavigation): Int {
        var identifier = -1
        navigation.milestones.forEach {
            if (it is BannerInstructionMilestone) {
                identifier = it.identifier
            }
        }
        return identifier
    }

    private fun searchForVoiceInstructionMilestone(navigation: MapboxNavigation): Int {
        var identifier = -1
        navigation.milestones.forEach {
            if (it is VoiceInstructionMilestone) {
                identifier = it.identifier
            }
        }
        return identifier
    }
}
