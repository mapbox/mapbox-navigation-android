package com.mapbox.navigation.qa_test_app.view.customnavview

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SpinnerAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.dropin.ActionButtonDescription
import com.mapbox.navigation.dropin.ActionButtonDescription.Position.END
import com.mapbox.navigation.dropin.ActionButtonDescription.Position.START
import com.mapbox.navigation.dropin.MapViewObserver
import com.mapbox.navigation.dropin.NavigationViewListener
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteLineOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultAudioGuidanceButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultCameraModeButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultDestinationMarker
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultEndNavigationButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelBackground
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelMarginEnd
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelMarginStart
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelPeekHeight
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultManeuverViewOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRecenterButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoadNameBackground
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoadNameTextAppearance
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoutePreviewButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultSpeedLimitStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultSpeedLimitTextAppearance
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultStartNavigationButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultTripProgressStyle
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewCustomBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutInfoPanelHeaderBinding
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewCustomizedActivity : DrawerActivity() {

    private val viewModel: CustomizedViewModel by viewModels()

    private val navViewListener =
        LoggingNavigationViewListener("MapboxNavigationViewCustomizedActivity")

    private val onMapLongClick = object : MapViewObserver(), OnMapLongClickListener {

        override fun onAttached(mapView: MapView) {
            mapView.gestures.addOnMapLongClickListener(this)
        }

        override fun onDetached(mapView: MapView) {
            mapView.gestures.removeOnMapLongClickListener(this)
        }

        override fun onMapLongClick(point: Point): Boolean {
            Toast.makeText(
                this@MapboxNavigationViewCustomizedActivity,
                "Long press handled by activity",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }

    private lateinit var binding: LayoutActivityNavigationViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewCustomBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewCustomBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    private fun updateTheme() {
        if (viewModel.fullScreen.value == true) {
            setTheme(R.style.Theme_Fullscreen)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            setTheme(R.style.Theme_AppCompat_NoActionBar)
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        updateTheme()
        super.onCreate(savedInstanceState)

        binding.navigationView.addListener(freeDriveInfoPanelInstaller)
        binding.navigationView.addListener(navViewListener)
        binding.navigationView.customizeViewOptions {
            routeArrowOptions = customRouteArrowOptions()
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())

        bindSwitch(
            menuBinding.toggleViewStyling,
            viewModel.useCustomStyles,
            ::customizeStyles
        )

        bindSwitch(
            menuBinding.toggleCustomViews,
            viewModel.useCustomViews,
            ::customizeViews
        )

        bindSwitch(
            menuBinding.toggleCustomMap,
            viewModel.showCustomMapView,
            ::customizeMap
        )

        bindSwitch(
            menuBinding.toggleFullscreen,
            getValue = { viewModel.fullScreen.value == true },
            setValue = {
                viewModel.fullScreen.value = it
                recreate()
            }
        )

        bindSwitch(
            menuBinding.toggleCustomInfoPanelContent,
            viewModel.showCustomInfoPanelContent,
            ::toggleCustomInfoPanelContent
        )
        bindSwitch(
            menuBinding.toggleCustomInfoPanel,
            viewModel.useCustomInfoPanelLayout,
            ::toggleCustomInfoPanelLayout
        )
        bindSwitch(
            menuBinding.toggleCustomInfoPanelStyles,
            viewModel.useCustomInfoPanelStyles,
            ::toggleCustomInfoPanelStyles
        )

        bindSwitch(
            menuBinding.toggleBottomSheetFD,
            viewModel.showBottomSheetInFreeDrive,
            ::toggleShowInfoPanelInFreeDrive
        )

        bindSwitch(
            menuBinding.toggleOnMapLongClick,
            viewModel.enableOnMapLongClick,
            ::toggleOnMapLongClick
        )

        bindSwitch(
            menuBinding.toggleIsInfoPanelHideable,
            viewModel.isInfoPanelHideable,
            ::toggleInfoPanelHiding
        )

        bindSwitch(
            menuBinding.useMetric,
            viewModel.distanceFormatterMetric,
            ::toggleUseMetric
        )

        bindSpinner(
            menuBinding.spinnerInfoPanelVisibilityOverride,
            viewModel.infoPanelStateOverride,
            ::overrideInfoPanelState
        )
    }

    override fun onResume() {
        super.onResume()

        bindSwitch(
            menuBinding.toggleTheme,
            getValue = resources.configuration::isNightMode,
            setValue = {
                val themeMode =
                    if (it) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                toggleTheme(themeMode)
            }
        )

        bindSwitch(
            menuBinding.toggleReplay,
            getValue = binding.navigationView.api::isReplayEnabled,
            setValue = { isChecked ->
                binding.navigationView.api.routeReplayEnabled(isChecked)
            }
        )
    }

    private fun bindSwitch(
        switch: SwitchCompat,
        getValue: () -> Boolean,
        setValue: (v: Boolean) -> Unit
    ) {
        switch.isChecked = getValue()
        switch.setOnCheckedChangeListener { _, isChecked -> setValue(isChecked) }
    }

    private fun bindSwitch(
        switch: SwitchCompat,
        liveData: MutableLiveData<Boolean>,
        onChange: (value: Boolean) -> Unit
    ) {
        liveData.observe(this) {
            switch.isChecked = it
            onChange(it)
        }
        switch.setOnCheckedChangeListener { _, isChecked ->
            liveData.value = isChecked
        }
    }

    private fun bindSpinner(
        spinner: AppCompatSpinner,
        liveData: MutableLiveData<String>,
        onChange: (value: String) -> Unit
    ) {
        liveData.observe(this) {
            if (spinner.selectedItem != it) {
                spinner.setSelection(spinner.adapter.findItemPosition(it) ?: 0)
            }
            onChange(it)
        }

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    liveData.value = parent.getItemAtPosition(position) as? String
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun overrideInfoPanelState(value: String) {
        val newState = when (value) {
            "HIDDEN" -> BottomSheetBehavior.STATE_HIDDEN
            "COLLAPSED" -> BottomSheetBehavior.STATE_COLLAPSED
            "HALF_EXPANDED" -> BottomSheetBehavior.STATE_HALF_EXPANDED
            "EXPANDED" -> BottomSheetBehavior.STATE_EXPANDED
            else -> 0
        }
        binding.navigationView.customizeViewOptions {
            infoPanelForcedState = newState
        }
    }

    private fun customizeStyles(customStyles: Boolean) {
        if (customStyles) {
            binding.navigationView.customizeViewStyles {
                tripProgressStyle = R.style.MyCustomTripProgressStyle
                speedLimitStyle = R.style.MyCustomSpeedLimitStyle
                speedLimitTextAppearance = R.style.MyCustomSpeedLimitTextAppearance
                destinationMarker = R.drawable.mapbox_ic_marker
                roadNameBackground = R.drawable.mapbox_bg_road_name
                roadNameTextAppearance = R.style.MyCustomRoadNameViewTextAppearance
                audioGuidanceButtonStyle = R.style.MyCustomAudioGuidanceButton
                recenterButtonStyle = R.style.MyCustomRecenterButton
                cameraModeButtonStyle = R.style.MyCustomCameraModeButton
                routePreviewButtonStyle = R.style.MyCustomRoutePreviewButton
                endNavigationButtonStyle = R.style.MyCustomEndNavigationButton
                startNavigationButtonStyle = R.style.MyCustomStartNavigationButton
                maneuverViewOptions = customManeuverOptions()
            }
        } else {
            binding.navigationView.customizeViewStyles {
                tripProgressStyle = defaultTripProgressStyle()
                speedLimitStyle = defaultSpeedLimitStyle()
                speedLimitTextAppearance = defaultSpeedLimitTextAppearance()
                maneuverViewOptions = defaultManeuverViewOptions()
                destinationMarker = defaultDestinationMarker()
                roadNameBackground = defaultRoadNameBackground()
                roadNameTextAppearance = defaultRoadNameTextAppearance()
                audioGuidanceButtonStyle = defaultAudioGuidanceButtonStyle()
                recenterButtonStyle = defaultRecenterButtonStyle()
                cameraModeButtonStyle = defaultCameraModeButtonStyle()
                routePreviewButtonStyle = defaultRoutePreviewButtonStyle()
                endNavigationButtonStyle = defaultEndNavigationButtonStyle()
                startNavigationButtonStyle = defaultStartNavigationButtonStyle()
            }
        }
    }

    private fun customizeViews(showCustomViews: Boolean) {
        // This demonstrates that you can customize views at any time. You can also reset to
        // the default views.
        if (showCustomViews) {
            binding.navigationView.customizeViewBinders {
                speedLimitBinder = CustomSpeedLimitViewBinder()
                customActionButtons = listOf(
                    ActionButtonDescription(customActionButton("button 1"), START),
                    ActionButtonDescription(customActionButton("button 2"), START),
                    ActionButtonDescription(customActionButton("button 3"), END),
                    ActionButtonDescription(customActionButton("button 4"), END)
                )
            }
            binding.navigationView.customizeViewOptions {
                routeLineOptions = customRouteLineOptions()
                mapStyleUriDay = Style.LIGHT
                mapStyleUriNight = Style.DARK
            }
        } else {
            // Reset defaults
            binding.navigationView.customizeViewBinders {
                speedLimitBinder = UIBinder.USE_DEFAULT
                customActionButtons = emptyList()
            }
            binding.navigationView.customizeViewOptions {
                routeLineOptions = defaultRouteLineOptions(applicationContext)
                mapStyleUriDay = NavigationStyles.NAVIGATION_DAY_STYLE
                mapStyleUriNight = NavigationStyles.NAVIGATION_NIGHT_STYLE
            }
        }
    }

    private fun customizeMap(showCustomMapView: Boolean) {
        // Demonstrate map customization
        if (showCustomMapView) {
            binding.navigationView.customizeMapView(customMapViewFromCode(this))
        } else {
            binding.navigationView.customizeMapView(null)
        }
    }

    private fun toggleCustomInfoPanelContent(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            if (enabled) {
                infoPanelContentBinder = UIBinder { viewGroup ->
                    supportFragmentManager.beginTransaction()
                        .replace(viewGroup.id, CustomInfoPanelDetailsFragment())
                        .commitAllowingStateLoss()
                    UIComponent()
                }
            } else {
                infoPanelContentBinder = UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomInfoPanelLayout(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            infoPanelBinder =
                if (enabled) CustomInfoPanelBinder()
                else InfoPanelBinder.defaultBinder()
        }
        binding.navigationView.customizeViewStyles {
            infoPanelPeekHeight =
                if (enabled) defaultInfoPanelPeekHeight(applicationContext) + 20.dp
                else defaultInfoPanelPeekHeight(applicationContext)
        }
    }

    private fun toggleCustomInfoPanelStyles(enabled: Boolean) {
        binding.navigationView.customizeViewStyles {
            if (enabled) {
                infoPanelBackground = R.drawable.bg_custom_info_panel2
                infoPanelMarginStart = 10.dp
                infoPanelMarginEnd = 10.dp
            } else {
                infoPanelBackground = defaultInfoPanelBackground()
                infoPanelMarginStart = defaultInfoPanelMarginStart()
                infoPanelMarginEnd = defaultInfoPanelMarginEnd()
            }
        }
    }

    private fun toggleShowInfoPanelInFreeDrive(showInFreeDrive: Boolean) {
        // Show Bottom Sheet in Free Drive
        binding.navigationView.customizeViewOptions {
            showInfoPanelInFreeDrive = showInFreeDrive
        }
    }

    private fun toggleOnMapLongClick(enable: Boolean) {
        // If enabled [NavigationView] will intercept map long clicks
        if (enable) {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
        } else {
            binding.navigationView.registerMapObserver(onMapLongClick)
        }
        binding.navigationView.customizeViewOptions {
            enableMapLongClickIntercept = enable
        }
    }

    private fun toggleInfoPanelHiding(isHideable: Boolean) {
        binding.navigationView.customizeViewOptions {
            isInfoPanelHideable = isHideable
        }
    }

    private fun toggleUseMetric(showMetric: Boolean) {
        val options = DistanceFormatterOptions
            .Builder(this@MapboxNavigationViewCustomizedActivity)
            .build()
        if (showMetric) {
            binding.navigationView.customizeViewOptions {
                distanceFormatterOptions = options.toBuilder().unitType(UnitType.METRIC).build()
            }
        } else {
            binding.navigationView.customizeViewOptions {
                distanceFormatterOptions = options.toBuilder().unitType(UnitType.IMPERIAL).build()
            }
        }
    }

    private fun customActionButton(text: String): View {
        return AppCompatTextView(this).apply {
            val w = resources.getDimensionPixelSize(R.dimen.mapbox_actionList_width)
            layoutParams = ViewGroup.MarginLayoutParams(
                72.dp,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setPadding(0, 20.dp, 0, 20.dp)
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setText(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setOnClickListener {
                Toast.makeText(context, "'$text' clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun customRouteLineOptions() =
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder()
                            .routeLowCongestionColor(Color.YELLOW)
                            .routeCasingColor(Color.RED)
                            .build()
                    )
                    .build()
            )
            .withRouteLineBelowLayerId("road-label") // for Style.LIGHT and Style.DARK
            .withVanishingRouteLineEnabled(true)
            .displaySoftGradientForTraffic(true)
            .build()

    private fun customRouteArrowOptions() =
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .withArrowColor(Color.RED)
            .build()

    private fun customManeuverOptions() = ManeuverViewOptions
        .Builder()
        .maneuverBackgroundColor(R.color.maneuver_main_background)
        .subManeuverBackgroundColor(R.color.maneuver_sub_background)
        .upcomingManeuverBackgroundColor(R.color.maneuver_sub_background)
        .turnIconManeuver(R.style.MyCustomTurnIconManeuver)
        .laneGuidanceTurnIconManeuver(R.style.MyCustomTurnIconManeuver)
        .stepDistanceTextAppearance(R.style.MyCustomStepDistance)
        .primaryManeuverOptions(
            ManeuverPrimaryOptions
                .Builder()
                .textAppearance(R.style.MyCustomPrimaryManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MyCustomExitTextForPrimary)
                        .build()
                )
                .build()
        )
        .secondaryManeuverOptions(
            ManeuverSecondaryOptions
                .Builder()
                .textAppearance(R.style.MyCustomSecondaryManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MyCustomExitTextForSecondary)
                        .build()
                )
                .build()
        )
        .subManeuverOptions(
            ManeuverSubOptions
                .Builder()
                .textAppearance(R.style.MyCustomSubManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MyCustomExitTextForSub)
                        .build()
                )
                .build()
        )
        .build()

    // NavigationViewListener that will install custom info panel header and content
    // only when in Free Drive state.
    private val freeDriveInfoPanelInstaller = object : NavigationViewListener() {
        override fun onFreeDrive() {
            binding.navigationView.customizeViewBinders {
                infoPanelHeaderBinder = UIBinder { viewGroup ->
                    viewGroup.removeAllViews()
                    val header = LayoutInfoPanelHeaderBinding
                        .inflate(layoutInflater, viewGroup, false).root
                    viewGroup.addView(header)
                    UIComponent()
                }
            }
        }

        override fun onDestinationPreview() {
            binding.navigationView.customizeViewBinders {
                infoPanelHeaderBinder = UIBinder.USE_DEFAULT
            }
        }
    }

    private fun SpinnerAdapter.findItemPosition(item: Any): Int? {
        for (pos in 0..count) {
            if (item == getItem(pos)) return pos
        }
        return null
    }
}
