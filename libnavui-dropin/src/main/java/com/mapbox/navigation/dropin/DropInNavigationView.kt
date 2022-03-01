package com.mapbox.navigation.dropin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.use
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.maps.extension.style.style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.coordinator.ActionListCoordinator
import com.mapbox.navigation.dropin.coordinator.BackPressManager
import com.mapbox.navigation.dropin.coordinator.GuidanceCoordinator
import com.mapbox.navigation.dropin.coordinator.InfoPanelCoordinator
import com.mapbox.navigation.dropin.coordinator.MapCoordinator
import com.mapbox.navigation.dropin.coordinator.NavigationStateManager
import com.mapbox.navigation.dropin.coordinator.RoadNameLabelCoordinator
import com.mapbox.navigation.dropin.coordinator.RouteManager
import com.mapbox.navigation.dropin.coordinator.SpeedLimitCoordinator
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.dropin.extensions.attachCreated
import com.mapbox.navigation.dropin.extensions.attachStarted
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    accessToken: String = attrs.navigationViewAccessToken(context),
) : FrameLayout(context, attrs), LifecycleOwner {

    private val binding: DropInNavigationViewBinding = DropInNavigationViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val viewLifecycleRegistry: ViewLifecycleRegistry = ViewLifecycleRegistry(
        view = this,
        localLifecycleOwner = this,
        hostingLifecycleOwner = context.toLifecycleOwner(),
    )

    private val viewModelProvider by lazy {
        ViewModelProvider(context.toViewModelStoreOwner())
    }

    /**
     * The one and only view model for [DropInNavigationView]. If you need state
     * to survive orientation changes, put it in the [DropInNavigationViewModel].
     */
    private val viewModel: DropInNavigationViewModel by lazyViewModel()

    /**
     * This is a top level object to share data with all state view holders. If you need state
     * to survive orientation changes, put it in the [DropInNavigationViewModel].
     */
    private val navigationContext = DropInNavigationViewContext(
        context = context,
        lifecycleOwner = this,
        viewModel = viewModel,
    )

    /**
     * Retrieve the [OnBackPressedCallback] that can be registered with OnBackPressedDispatcher
     * to allow [androidx.activity.ComponentActivity#onBackPressed()] handling in DropIn UI .
     */
    fun getOnBackPressedCallback(): OnBackPressedCallback = navigationContext.onBackPressedCallback

    /**
     * Customize the views by implementing your own [UIBinder] components.
     */
    fun customize(navigationUIBinders: NavigationUIBinders) {
        navigationContext.uiBinders.value = navigationUIBinders
    }

    /**
     * Provide custom route line options to override the default options. This must be called
     * before your activity or fragment's onStart() in order to take effect.
     *
     * @param options the [MapboxRouteLineOptions] to use.
     */
    fun customize(options: MapboxRouteLineOptions) {
        navigationContext.routeLineOptions = options
    }

    /**
     * Provide custom route arrow options to override the default options. This must be called
     * before your activity or fragment's onStart() in order to take effect.
     *
     * @param options the [RouteArrowOptions] to use.
     */
    fun customize(options: RouteArrowOptions) {
        navigationContext.routeArrowOptions = options
    }

    init {
        binding.mapView.getMapboxMap().loadStyle(
            style(NavigationStyles.NAVIGATION_DAY_STYLE) {
                // TODO allow for customization.
                // +skyLayer(...)
            }
        )

        /**
         * Default setup for MapboxNavigationApp. The developer can customize this by
         * setting up the MapboxNavigationApp before the view is constructed.
         */
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(context)
                    .accessToken(accessToken)
                    .build()
            )
        }

        /**
         * Attach the lifecycle to mapbox navigation. This ensures that all
         * MapboxNavigationObservers will be attached.
         */
        MapboxNavigationApp.attach(this)

        attachCreated(
            NavigationStateManager(navigationContext),
            RouteManager(navigationContext),
            BackPressManager(navigationContext)
        )

        /**
         * Single point of entry for the Mapbox Navigation View.
         */
        attachStarted(
            MapCoordinator(navigationContext, binding.mapView),
            GuidanceCoordinator(navigationContext, binding.guidanceLayout),
            InfoPanelCoordinator(
                navigationContext,
                binding.infoPanelLayout,
                binding.guidelineBottom
            ),
            ActionListCoordinator(navigationContext, binding.actionListLayout),
            SpeedLimitCoordinator(navigationContext, binding.speedLimitLayout),
            RoadNameLabelCoordinator(navigationContext, binding.roadNameLayout)
        )
    }

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry

    private inline fun <reified T : ViewModel> lazyViewModel(): Lazy<T> = lazy {
        viewModelProvider[T::class.java]
    }
}

private tailrec fun recursiveUnwrap(context: Context): Context =
    if (context !is Activity && context is ContextWrapper) {
        recursiveUnwrap(context.baseContext)
    } else {
        context
    }

private fun AttributeSet?.navigationViewAccessToken(context: Context): String {
    val accessToken = context.obtainStyledAttributes(
        this,
        R.styleable.NavigationView,
        0,
        0
    ).use { it.getString(R.styleable.NavigationView_accessToken) }
    checkNotNull(accessToken) {
        "Provide access token directly in the constructor or via 'accessToken' layout parameter"
    }
    return accessToken
}

private fun Context.toLifecycleOwner(): LifecycleOwner {
    val lifecycleOwner = recursiveUnwrap(this) as? LifecycleOwner
    checkNotNull(lifecycleOwner) {
        "Please ensure that the hosting Context is a valid LifecycleOwner"
    }
    return lifecycleOwner
}

private fun Context.toViewModelStoreOwner(): ViewModelStoreOwner {
    val viewModelStoreOwner = recursiveUnwrap(this) as? ViewModelStoreOwner
    checkNotNull(viewModelStoreOwner) {
        "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
    }
    return viewModelStoreOwner
}
