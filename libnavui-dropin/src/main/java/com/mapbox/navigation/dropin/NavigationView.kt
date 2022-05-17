package com.mapbox.navigation.dropin

import android.Manifest
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.backpress.OnKeyListenerComponent
import com.mapbox.navigation.dropin.component.tripsession.LocationPermissionComponent
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterAction
import com.mapbox.navigation.dropin.coordinator.ActionButtonsCoordinator
import com.mapbox.navigation.dropin.coordinator.InfoPanelCoordinator
import com.mapbox.navigation.dropin.coordinator.ManeuverCoordinator
import com.mapbox.navigation.dropin.coordinator.MapLayoutCoordinator
import com.mapbox.navigation.dropin.coordinator.RoadNameLabelCoordinator
import com.mapbox.navigation.dropin.coordinator.SpeedLimitCoordinator
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.attachCreated
import com.mapbox.navigation.dropin.internal.extensions.navigationViewAccessToken
import com.mapbox.navigation.dropin.internal.extensions.toComponentActivityRef
import com.mapbox.navigation.dropin.internal.extensions.toLifecycleOwner
import com.mapbox.navigation.dropin.internal.extensions.toViewModelStoreOwner
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * An Android [View] that creates the drop-in UI.
 *
 * [NavigationView] when started will ask for [Manifest.permission.ACCESS_FINE_LOCATION] and
 * [Manifest.permission.ACCESS_COARSE_LOCATION].
 * If denied the view will point to a null island.
 * If accepted, this view will always start in Free Drive state with the puck pointing to
 * users current location.
 *
 * The [NavigationView] is spread across the following states:
 * - Free Drive: A user will be in free drive state when there is no destination or route set.
 * - DestinationPreview: A user will be in destination preview when a destination is set.
 * - RoutePreview: A user will be in route preview when a destination is set along with a route
 * joining the origin/destination pair.
 * - Active Guidance: A user will be in active guidance when a single destination or multi waypoint
 * route is set and the user has explicitly initiated start navigation action.
 * - Arrival: A user will be in arrival state when they are about to reach the final destination.
 *
 * [NavigationView] by default does not uses [MapboxReplayer], rather it uses the actual location
 * engine and hooks onto the users current location derived from device GPS. However, this can be
 * overridden and instead you can simulate active guidance by enabling `isReplayEnabled`.
 *
 * Note: [NavigationView] is `Experimental` and the API(s) are subject to breaking changes.
 *
 * A Mapbox access token must also be set by the developer (to initialize navigation).
 */
@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationView constructor(
    context: Context,
    attrs: AttributeSet?,
    accessToken: String,
    hostingLifecycle: Lifecycle?,
    viewModelStoreOwner: ViewModelStoreOwner?,
) : FrameLayout(context, attrs), LifecycleOwner {

    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        accessToken: String = attrs.navigationViewAccessToken(context),
        hostingFragment: Fragment? = null
    ): this(
        context,
        attrs,
        accessToken,
        hostingFragment?.viewLifecycleOwner?.lifecycle,
        hostingFragment
    )

    private val binding: MapboxNavigationViewLayoutBinding =
        MapboxNavigationViewLayoutBinding.inflate(
            LayoutInflater.from(context),
            this
        )

    private val lifecycle: Lifecycle = (hostingLifecycle ?: ViewLifecycleRegistry(
        view = this,
        localLifecycleOwner = this,
        hostingLifecycleOwner =  context.toLifecycleOwner(),
    )).also {
        it.addObserver(LifecycleLogger(this@NavigationView.hashCode().toString()))
    }

    private val viewModelProvider by lazy {
        ViewModelProvider(viewModelStoreOwner ?: context.toViewModelStoreOwner())
    }

    private val viewModel: NavigationViewModel by lazyViewModel()

    private val navigationContext = NavigationViewContext(
        context = context,
        lifecycleOwner = this,
        viewModel = viewModel,
    )

    init {
        keepScreenOn = true

        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(context)
                    .accessToken(accessToken)
                    .build()
            )
        }

        MapboxNavigationApp.attach(this)

        attachCreated(
            LocationPermissionComponent(context.toComponentActivityRef(), viewModel.store),
            MapLayoutCoordinator(navigationContext, binding),
            OnKeyListenerComponent(viewModel.store, this),
            ManeuverCoordinator(navigationContext, binding.guidanceLayout),
            InfoPanelCoordinator(
                navigationContext,
                binding.infoPanelLayout,
                binding.guidelineBottom
            ),
            ActionButtonsCoordinator(navigationContext, binding.actionListLayout),
            SpeedLimitCoordinator(navigationContext, binding.speedLimitLayout),
            RoadNameLabelCoordinator(navigationContext, binding.roadNameLayout)
        )
    }

    /**
     * Enable/Disable Trip Session Replay
     */
    var isReplayEnabled: Boolean
        get() = viewModel.store.state.value.tripSession.isReplayEnabled
        set(value) {
            if (value) {
                viewModel.store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
            } else {
                viewModel.store.dispatch(TripSessionStarterAction.EnableTripSession)
            }
        }

    /**
     * Provides access to [ViewLifecycleRegistry]
     */
    override fun getLifecycle(): Lifecycle = lifecycle

    /**
     * Customize the views by implementing your own [UIBinder] components.
     */
    fun customizeMapView(mapView: MapView?) {
        navigationContext.mapView.value = mapView
    }

    /**
     * Customize view by providing your own [UIBinder] components.
     */
    fun customizeViewBinders(action: ViewBinderCustomization.() -> Unit) {
        navigationContext.applyBinderCustomization(action)
    }

    /**
     * Customize standalone UI components styles by providing your own custom styles.
     */
    fun customizeViewStyles(action: ViewStyleCustomization.() -> Unit) {
        navigationContext.applyStyleCustomization(action)
    }

    /**
     * Provide custom map styles, route line and arrow options.
     */
    fun customizeViewOptions(action: ViewOptionsCustomization.() -> Unit) {
        navigationContext.applyOptionsCustomization(action)
    }

    private inline fun <reified T : ViewModel> lazyViewModel(): Lazy<T> = lazy {
        viewModelProvider[T::class.java]
    }
}

