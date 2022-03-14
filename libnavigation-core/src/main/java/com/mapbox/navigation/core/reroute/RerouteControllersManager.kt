package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.reroute.RerouteControllersManager.CollectionRerouteInterfaces.Internal
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RerouteControllerInterface

private typealias NativeRerouteControllerInterface = RerouteControllerInterface

internal class RerouteControllersManager private constructor(
    private val accessToken: String?,
    private val navigator: MapboxNativeNavigator,
    private val reroutesObserver: RerouteControllersManager.Observer,
) {

    // hold initial NN Reroute controller to have option re-set it in runtime
    private var initialNativeRerouteControllerInterface: NativeRerouteControllerInterface

    private var _rerouteOptionsAdapter: RerouteOptionsAdapter? = null
        private set(value) {
            field = value
            (rerouteInterfaceSet.sdkRerouteController as? MapboxRerouteControllerFacade)
                ?.setRerouteOptionsAdapter(field)
        }

    @VisibleForTesting
    internal var rerouteInterfaceSet: CollectionRerouteInterfaces =
        CollectionRerouteInterfaces.Disabled()
        set(value) {
                field = value
                navigator.setRerouteControllerInterface(value.nativeRerouteControllerInterface)
            }

    val rerouteControllerInterface: NavigationRerouteController?
        get() = rerouteInterfaceSet.sdkRerouteController

    init {
        initialNativeRerouteControllerInterface = navigator.getRerouteControllerInterface()
        resetToDefaultRerouteController()
    }

    internal companion object {
        /**
         * Provides Reroute Controllers Manager with initial
         * [CollectionRerouteInterfaces.Internal]
         */
        internal operator fun invoke(
            accessToken: String?,
            reroutesObserver: RerouteControllersManager.Observer,
            navigator: MapboxNativeNavigator
        ): RerouteControllersManager =
            RerouteControllersManager(accessToken, navigator, reroutesObserver)

        private fun wrapNativeRerouteControllerInterface(
            accessToken: String?,
            initialNativeRerouteControllerInterface: NativeRerouteControllerInterface,
            navigator: MapboxNativeNavigator,
        ): NativeExtendedRerouteControllerInterface =
            NativeRerouteControllerWrapper(
                accessToken,
                initialNativeRerouteControllerInterface,
                navigator,
            )
    }

    fun setOuterRerouteController(customerRerouteController: NavigationRerouteController) {
        rerouteInterfaceSet = CollectionRerouteInterfaces.Outer(
            accessToken,
            reroutesObserver,
            customerRerouteController,
        )
    }

    fun disableReroute() {
        rerouteInterfaceSet = CollectionRerouteInterfaces.Disabled()
    }

    fun resetToDefaultRerouteController() {
        rerouteInterfaceSet = CollectionRerouteInterfaces.Internal(
            accessToken,
            initialNativeRerouteControllerInterface,
            navigator,
            reroutesObserver,
        )
        setRerouteOptionsAdapter(_rerouteOptionsAdapter)
    }

    fun interruptReroute() {
        rerouteControllerInterface?.interrupt()
    }

    fun onNavigatorRecreated() {
        initialNativeRerouteControllerInterface = navigator.getRerouteControllerInterface()

        when (val legacyInterfaceSet = rerouteInterfaceSet) {
            is CollectionRerouteInterfaces.Outer -> {
                setOuterRerouteController(legacyInterfaceSet._sdkRerouteController)
            }
            is CollectionRerouteInterfaces.Internal -> {
                resetToDefaultRerouteController()
            }
            is CollectionRerouteInterfaces.Disabled -> {
                disableReroute()
            }
        }
    }

    fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        this._rerouteOptionsAdapter = rerouteOptionsAdapter
    }

    /**
     * Collection of SDK and Native reroute controller. They must be in sync. For instance, if
     * default([Internal]) reroute controller always must be set default Native
     * Reroute controller.
     */
    @VisibleForTesting
    internal sealed class CollectionRerouteInterfaces(
        val sdkRerouteController: NavigationRerouteController?,
        val nativeRerouteControllerInterface: NativeRerouteControllerInterface,
    ) {
        /**
         * Internal Reroute Interfaces collection. Includes Default NN reroute controller and
         * SDK wrapper
         */
        class Internal private constructor(
            mapboxRerouteControllerFacade: MapboxRerouteControllerFacade,
            rerouteControllerInterface: NativeExtendedRerouteControllerInterface,
        ) : CollectionRerouteInterfaces(
            mapboxRerouteControllerFacade,
            rerouteControllerInterface
        ) {
            companion object {
                operator fun invoke(
                    accessToken: String?,
                    initialNativeRerouteControllerInterface: NativeRerouteControllerInterface,
                    navigator: MapboxNativeNavigator,
                    rerouteObserver: RerouteControllersManager.Observer,
                ): Internal {

                    val nativeRerouteControllerWrapper = wrapNativeRerouteControllerInterface(
                        accessToken,
                        initialNativeRerouteControllerInterface,
                        navigator,
                    )

                    val platformRouteController = MapboxRerouteControllerFacade(
                        rerouteObserver,
                        nativeRerouteControllerWrapper,
                    )

                    return Internal(
                        platformRouteController,
                        nativeRerouteControllerWrapper
                    )
                }
            }
        }

        class Outer private constructor(
            internal val _sdkRerouteController: NavigationRerouteController,
            rerouteControllerInterface: NativeRerouteControllerInterface,
        ) : CollectionRerouteInterfaces(_sdkRerouteController, rerouteControllerInterface) {
            companion object {
                operator fun invoke(
                    accessToken: String?,
                    reroutesObserver: RerouteControllersManager.Observer,
                    customerRerouteController: NavigationRerouteController,
                ): Outer =
                    Outer(
                        customerRerouteController,
                        RerouteControllerAdapter(
                            accessToken, reroutesObserver, customerRerouteController
                        ),
                    )
            }
        }

        class Disabled private constructor() : CollectionRerouteInterfaces(
            null,
            DisabledRerouteControllerInterface(),
        ) {
            companion object {
                operator fun invoke(): Disabled = Disabled()
            }
        }
    }

    internal fun interface Observer {
        fun onNewRoutes(routes: List<NavigationRoute>)
    }
}
