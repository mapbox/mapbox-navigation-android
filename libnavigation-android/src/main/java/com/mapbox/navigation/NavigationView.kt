package com.mapbox.navigation

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.databinding.MapboxNavigationViewBinding
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView

/**
 * TODO: document your custom view class.
 */
class NavigationView : ConstraintLayout {

    private val binding = MapboxNavigationViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private lateinit var configuration: NavigationViewConfiguration

    val mapView: MapView
    val mapboxNavigation: MapboxNavigation
    val navigationViewOptions: NavigationViewOptions
    val state: NavigationState = NavigationState.Empty

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(
        context,
        attrs,
        defStyle,
        NavigationViewOptions(),
        mapView = null,
        mapboxNavigation = null,
        accessToken = null
    )

    @JvmOverloads
    constructor(
        context: Context,
        navigationViewOptions: NavigationViewOptions,
        mapboxNavigation: MapboxNavigation,
        mapView: MapView? = null,
    ) : this(
        context,
        null,
        0,
        navigationViewOptions,
        mapView,
        mapboxNavigation,
        accessToken = mapboxNavigation.navigationOptions.accessToken
    )

    constructor(
        context: Context,
        navigationViewOptions: NavigationViewOptions,
        accessToken: String?
    ) : this(
        context,
        null,
        0,
        navigationViewOptions,
        mapView = null,
        mapboxNavigation = null,
        accessToken
    )

    internal constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
        navigationViewOptions: NavigationViewOptions,
        mapView: MapView?,
        mapboxNavigation: MapboxNavigation?,
        accessToken: String?
    ) : super(context, attrs, defStyle) {
        check(mapView?.isAttachedToWindow != true) {
            "The provided Map View cannot be attached to a window"
        }
        this.navigationViewOptions = navigationViewOptions
        this.mapView = mapView ?: MapView(context, MapInitOptions(context))

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.NavigationView, defStyle, 0
        )
        val attrsToken = a.getString(R.styleable.NavigationView_accessToken)
        a.recycle()

        this.mapboxNavigation = mapboxNavigation ?: MapboxNavigation(
            NavigationOptions.Builder(
                context.applicationContext
            ).accessToken(accessToken ?: attrsToken).build()
        )
    }

    fun configure(configuration: NavigationViewConfiguration) {
        this.configuration = configuration

        this.mapView.getMapboxMap().loadStyleUri(Style.LIGHT)
        binding.dropinContainerMap.addView(this.mapView)

        if (tripProgress.isEnabled) {
            binding.dropinContainerTripProgress.addView(configuration.tripProgressProvider?.invoke() ?: MapboxTripProgressView())
        }
        transitionTo(NavigationState.Empty)
    }

    fun transitionTo(state: NavigationState) {
        when(state) {
            NavigationState.ActiveGuidance -> TODO()
            NavigationState.Arrival -> TODO()
            NavigationState.Empty -> {
                binding.dropinContainerRecenterButton.visibility = GONE
                binding.dropinContainerTripProgress.visibility = GONE
            }
            NavigationState.FreeDrive -> {
                binding.dropinContainerRecenterButton.visibility = VISIBLE

            }
            NavigationState.RoutePreview -> TODO()
        }
    }

    fun style(navigationViewStyle: NavigationViewStyle) {

    }

    fun doAction()
}

data class NavigationViewOptions(
    val tripProgressEnabled: Boolean = true
)

sealed class NavigationState {
    /**
     * Only the Map is visible.
     */
    object Empty: NavigationState()
    object FreeDrive : NavigationState()
    object RoutePreview : NavigationState()
    object ActiveGuidance : NavigationState()
    object Arrival : NavigationState()
}

class NavigationViewConfiguration(
    val tripProgressProvider: (() -> View)?
)

class NavigationViewStyle(
    val themeColor1: Int = Color.RED,
    val themeColor2: Int = Color.GREEN,
    val mapboxRouteLineOptions: MapboxRouteLineOptions
) {
    class RecenterButton(
        val tintColor: Int?
    )
    class RouteLine(
    )

}
