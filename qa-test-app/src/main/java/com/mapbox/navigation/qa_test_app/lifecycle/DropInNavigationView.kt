package com.mapbox.navigation.qa_test_app.lifecycle

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.extensions.attachCreated
import com.mapbox.navigation.dropin.extensions.attachResumed
import com.mapbox.navigation.dropin.extensions.attachStarted
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.qa_test_app.lifecycle.backstack.BackStackBinding
import com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet.BottomSheetCoordinator
import com.mapbox.navigation.qa_test_app.lifecycle.topbanner.TopBannerCoordinator
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInLocationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), LifecycleOwner {

    private val accessToken = attrs.navigationViewAccessToken(context)

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

    private val cameraViewModel: DropInNavigationViewModel by lazyViewModel()
    private val locationViewModel: DropInLocationViewModel by lazyViewModel()

    init {
        // Load the map.
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) {
            // no op
        }

        /**
         * Default setup for MapboxNavigationApp. The developer can customize this by
         * setting up the MapboxNavigationApp before this view is constructed.
         */
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(context)
                    .accessToken(accessToken)
                    .build()
            )
        }

        /**
         * Attach this lifecycle to the MapboxNavigationApp. If the developer wants to remember
         * MapboxNavigation state over orientation changes they need to attach the Fragment or
         * Activity lifecycle owner.
         */
        MapboxNavigationApp.attach(this)

        // This should have some state available from a ViewModel
        attachCreated(BackStackBinding(binding.root))

        // Attach coordinators
        attachStarted(
            TopBannerCoordinator(binding.topBanner),
            BottomSheetCoordinator(binding.bottomSheet),
        )

        /**
         * Features for this view. Observers that are part of the created event, can be seen by
         * the components that are part of the started and resumed states.
         */
        attachResumed(
            DropInLocationPuck(locationViewModel, binding.mapView),
            DropInRoutesInteractor(locationViewModel, binding.mapView),
            DropInNavigationCamera(cameraViewModel, locationViewModel, this, binding.mapView),
            DropInRecenterButton(cameraViewModel, this, binding.recenter),
            DropInContinuousRoutes(),
        )
    }

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry

    private inline fun <reified T : ViewModel> lazyViewModel(): Lazy<T> = lazy {
        viewModelProvider[T::class.java]
    }
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

private fun recursiveUnwrap(context: Context): Context =
    if (context !is Activity && context is ContextWrapper) {
        recursiveUnwrap(context.baseContext)
    } else {
        context
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
