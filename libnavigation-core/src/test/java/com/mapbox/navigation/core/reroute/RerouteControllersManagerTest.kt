package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RerouteControllerInterface
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RerouteControllersManagerTest {

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(MapboxRerouteControllerFacade.Companion)
    }

    @After
    fun cleanup() {
        unmockkObject(MapboxRerouteControllerFacade.Companion)
    }

    private companion object {
        private fun provideRerouteControllerManagerAndMockedObjects(
            nativeDefaultRerouteController: RerouteControllerInterface = mockk(),
            accessToken: String = "pk.***",
            platformRerouteObserver: RerouteControllersManager.Observer = mockk(),
            navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true) {
                every { getRerouteControllerInterface() } returns nativeDefaultRerouteController
            },
            mockMapboxRerouteControllerFacade: MapboxRerouteControllerFacade =
                mockk(relaxUnitFun = true),
        ): Pair<RerouteControllersManager, Wrapper> {
            every {
                MapboxRerouteControllerFacade.invoke(any(), any())
            } returns mockMapboxRerouteControllerFacade

            return RerouteControllersManager(
                accessToken, platformRerouteObserver, navigator
            ) to Wrapper(
                platformRerouteObserver,
                navigator,
                nativeDefaultRerouteController,
                mockMapboxRerouteControllerFacade,
            )
        }

        private data class Wrapper(
            val platformRerouteObserver: RerouteControllersManager.Observer,
            val mockNavigator: MapboxNativeNavigator,
            val mockNativeDefaultRerouteController: RerouteControllerInterface,
            val mockMapboxRerouteControllerFacade: MapboxRerouteControllerFacade,
        )
    }

    @Test
    fun sanity() {
        val (rerouteManager, wrapper) = provideRerouteControllerManagerAndMockedObjects()

        verify(exactly = 1) { wrapper.mockNavigator.getRerouteControllerInterface() }
        assertTrue(
            rerouteManager.rerouteInterfaceSet
            is RerouteControllersManager.CollectionRerouteInterfaces.Internal
        )
        assertTrue(rerouteManager.rerouteControllerInterface!! is MapboxRerouteControllerFacade)
        verify(exactly = 1) {
            (rerouteManager.rerouteControllerInterface as MapboxRerouteControllerFacade)
                .setRerouteOptionsAdapter(null)
        }
    }

    @Test
    fun `set outer reroute controller`() {
        val (rerouteManager, _) = provideRerouteControllerManagerAndMockedObjects()
        val outerRerouteController: NavigationRerouteController = mockk()

        rerouteManager.setOuterRerouteController(outerRerouteController)

        assertEquals(
            outerRerouteController,
            rerouteManager.rerouteControllerInterface
        )
        assertTrue(
            rerouteManager.rerouteInterfaceSet
            is RerouteControllersManager.CollectionRerouteInterfaces.Outer
        )
    }

    @Test
    fun `disable reroute`() {
        val (rerouteManager, _) = provideRerouteControllerManagerAndMockedObjects()

        rerouteManager.disableReroute()

        assertNull(rerouteManager.rerouteControllerInterface)
        assertTrue(
            rerouteManager.rerouteInterfaceSet
            is RerouteControllersManager.CollectionRerouteInterfaces.Disabled
        )
    }

    @Test
    fun `reset to default reroute controller`() {
        val (rerouteManager, _) = provideRerouteControllerManagerAndMockedObjects()

        rerouteManager.disableReroute()
        rerouteManager.resetToDefaultRerouteController()

        assertTrue(
            rerouteManager.rerouteInterfaceSet
            is RerouteControllersManager.CollectionRerouteInterfaces.Internal
        )
        assertTrue(
            rerouteManager.rerouteControllerInterface!!
            is MapboxRerouteControllerFacade
        )
    }

    @Test
    fun `set reroute options adapter`() {
        data class Case(
            val description: String,
            val preActions: RerouteControllersManager.() -> Unit,
            val afterActions: RerouteControllersManager.() -> Unit,
            val checks: (
                String,
                RerouteOptionsAdapter,
                RerouteControllersManager,
                CapturingSlot<RerouteOptionsAdapter>
            ) -> Unit,
        )
        listOf(
            Case(
                "set route options with default reroute manager",
                preActions = {},
                afterActions = {},
                { description, mockRerouteOptionsAdapter, manager, slotAdapter ->

                    assertTrue(description, slotAdapter.isCaptured)
                    assertEquals(description, mockRerouteOptionsAdapter, slotAdapter.captured)
                }
            ),
            Case(
                "set route options to disabled reroute is not set to any",
                preActions = {
                    disableReroute()
                },
                afterActions = {},
                { description, mockRerouteOptionsAdapter, manager, slotAdapter ->
                    assertFalse(description, slotAdapter.isCaptured)
                }
            ),
            Case(
                "set route options to outer reroute controller is not set to any",
                preActions = {
                    setOuterRerouteController(mockk())
                },
                afterActions = {},
                { description, mockRerouteOptionsAdapter, manager, slotAdapter ->
                    assertFalse(description, slotAdapter.isCaptured)
                }
            ),
            Case(
                "set route options and reset reroute controller to default reset " +
                    "reroute option adapter",
                preActions = {
                    setRerouteOptionsAdapter(mockk())
                    resetToDefaultRerouteController()
                },
                afterActions = {
                    disableReroute()
                    resetToDefaultRerouteController()
                },
                { description, mockRerouteOptionsAdapter, manager, slotAdapter ->
                    assertTrue(description, slotAdapter.isCaptured)
                    assertEquals(description, mockRerouteOptionsAdapter, slotAdapter.captured)
                }
            ),
        ).forEach { (description, preActions, afterActions, checks) ->
            val (rerouteManager, wrapper) = provideRerouteControllerManagerAndMockedObjects()
            val slotRerouteOptionsAdapter = slot<RerouteOptionsAdapter>()
            every {
                wrapper.mockMapboxRerouteControllerFacade
                    .setRerouteOptionsAdapter(capture(slotRerouteOptionsAdapter))
            } just runs
            val mockRerouteOptionsAdapter: RerouteOptionsAdapter = mockk()

            preActions(rerouteManager)

            rerouteManager.setRerouteOptionsAdapter(mockRerouteOptionsAdapter)

            afterActions(rerouteManager)

            checks.invoke(
                description,
                mockRerouteOptionsAdapter,
                rerouteManager,
                slotRerouteOptionsAdapter,
            )
        }
    }

    @Test
    fun `interrupt reroute`() {
        val (rerouteManager, wrapper) = provideRerouteControllerManagerAndMockedObjects()

        rerouteManager.interruptReroute()

        verify(exactly = 1) {
            rerouteManager.rerouteControllerInterface!!.interrupt()
        }
    }

    @Test
    fun `on navigator recreated`() {
        data class Case(
            val descriptions: String,
            val preAction: RerouteControllersManager.() -> Unit,
            val afterAction: RerouteControllersManager.() -> Unit,
            val checks: (String, RerouteControllersManager, Wrapper) -> Unit,
        )
        listOf(
            Case(
                "navigator recreated with default reroute controller",
                preAction = {},
                afterAction = {},
                { descriptions, rerouteControllersManager, wrapper ->
                    verify(ordering = Ordering.SEQUENCE) {
                        wrapper.mockMapboxRerouteControllerFacade.setRerouteOptionsAdapter(null)
                        wrapper.mockMapboxRerouteControllerFacade.setRerouteOptionsAdapter(null)
                    }
                    assertTrue(
                        descriptions,
                        rerouteControllersManager.rerouteInterfaceSet is
                        RerouteControllersManager.CollectionRerouteInterfaces.Internal
                    )
                }
            ),
            run {
                val mockRerouteOptionsAdapter: RerouteOptionsAdapter = mockk()
                Case(
                    "navigator recreated with default reroute and reroute options adapter",
                    preAction = {
                        setRerouteOptionsAdapter(mockRerouteOptionsAdapter)
                    },
                    afterAction = {},
                    { descriptions, rerouteControllersManager, wrapper ->
                        verify(ordering = Ordering.SEQUENCE) {
                            wrapper.mockMapboxRerouteControllerFacade.setRerouteOptionsAdapter(null)
                            wrapper.mockMapboxRerouteControllerFacade
                                .setRerouteOptionsAdapter(mockRerouteOptionsAdapter)
                            wrapper.mockMapboxRerouteControllerFacade
                                .setRerouteOptionsAdapter(mockRerouteOptionsAdapter)
                        }
                        assertTrue(
                            descriptions,
                            rerouteControllersManager.rerouteInterfaceSet is
                            RerouteControllersManager.CollectionRerouteInterfaces.Internal
                        )
                    }
                )
            },
            run {
                val mockOuterRerouteController: NavigationRerouteController = mockk()
                Case(
                    "navigator recreated with outer reroute controller",
                    preAction = {
                        setOuterRerouteController(mockOuterRerouteController)
                    },
                    afterAction = {},
                    { descriptions, rerouteControllersManager, wrapper ->
                        verify(exactly = 1) {
                            wrapper.mockMapboxRerouteControllerFacade
                                .setRerouteOptionsAdapter(null)
                        }
                        assertTrue(
                            descriptions,
                            rerouteControllersManager.rerouteInterfaceSet is
                            RerouteControllersManager.CollectionRerouteInterfaces.Outer
                        )
                        assertEquals(
                            descriptions,
                            mockOuterRerouteController,
                            rerouteControllersManager.rerouteControllerInterface!!
                        )
                    }
                )
            },
            Case(
                "navigator recreated with outer reroute controller",
                preAction = {
                    disableReroute()
                },
                afterAction = {},
                { descriptions, rerouteControllersManager, wrapper ->
                    verify(exactly = 1) {
                        wrapper.mockMapboxRerouteControllerFacade
                            .setRerouteOptionsAdapter(null)
                    }
                    assertTrue(
                        descriptions,
                        rerouteControllersManager.rerouteInterfaceSet is
                        RerouteControllersManager.CollectionRerouteInterfaces.Disabled
                    )
                    assertNull(
                        descriptions,
                        rerouteControllersManager.rerouteControllerInterface
                    )
                }
            ),
        ).forEach { (descriptions, preAction, afterAction, checks) ->
            val (rerouteManager, wrapper) = provideRerouteControllerManagerAndMockedObjects()

            preAction(rerouteManager)

            rerouteManager.onNavigatorRecreated()

            afterAction(rerouteManager)

            checks(descriptions, rerouteManager, wrapper)
        }
    }
}
