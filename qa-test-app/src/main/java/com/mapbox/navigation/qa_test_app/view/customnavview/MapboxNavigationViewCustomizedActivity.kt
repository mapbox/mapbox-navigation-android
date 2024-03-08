package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.dump.MapboxDumpRegistry
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.internal.extensions.attachResumed
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultBuildingHighlightOptions
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteArrowOptions
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteLineOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultAudioGuidanceButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultCameraModeButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultCompassButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultDestinationMarkerAnnotationOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultEndNavigationButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelBackground
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelMarginEnd
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelMarginStart
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultLocationPuckOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultManeuverViewOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRecenterButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoadNameBackground
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoadNameTextAppearance
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultRoutePreviewButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultSpeedInfoOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultSpeedLimitStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultSpeedLimitTextAppearance
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultStartNavigationButtonStyle
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultTripProgressStyle
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription.Position.END
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription.Position.START
import com.mapbox.navigation.dropin.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.map.MapViewBinder
import com.mapbox.navigation.dropin.map.MapViewObserver
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewCustomBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutInfoPanelHeaderBinding
import com.mapbox.navigation.qa_test_app.dump.DistanceFormatterDumpInterceptor
import com.mapbox.navigation.qa_test_app.dump.NavigationViewApiDumpInterceptor
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions.Builder.Companion.regularPuck
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoStyle
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.utils.internal.toPoint

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewCustomizedActivity : DrawerActivity() {

    private val viewModel: CustomizedViewModel by viewModels()

    private lateinit var binding: LayoutActivityNavigationViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewCustomBinding

    private var lastLocation: Location? = null
    private lateinit var customInfoPanel: CustomInfoPanelBinder
    private lateinit var customInfoPanelFixedHeight: CustomInfoPanelBinderWithFixedHeight

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewCustomBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        updateTheme()
        super.onCreate(savedInstanceState)

        attachResumed(dumpCommands)
        attachCreated(
            object : MapboxNavigationObserver {

                override fun onAttached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.registerLocationObserver(locationObserver)
                }

                override fun onDetached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.unregisterLocationObserver(locationObserver)
                }
            },
        )

        customInfoPanel = CustomInfoPanelBinder(binding.navigationView)
        customInfoPanelFixedHeight = CustomInfoPanelBinderWithFixedHeight(binding.navigationView)

        binding.navigationView.addListener(freeDriveInfoPanelInstaller)
        binding.navigationView.addListener(navViewListener)
        binding.navigationView.customizeViewBinders {
            infoPanelHeaderArrivalBinder = CustomInfoPanelHeaderArrivalBinder(binding.navigationView)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
    }

    override fun onResume() {
        super.onResume()
        initDebugOptions()
        initActivityOptions()
        initGeneralOptions()
        initSpeedLimitOptions()
        initMapOptions()
        initActionsOptions()
        initInfoPanelOptions()
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

    //region Debug Options

    private fun initDebugOptions() {
        bindSwitch(
            menuBinding.toggleReplay,
            getValue = binding.navigationView.api::isReplayEnabled,
            setValue = { isChecked ->
                binding.navigationView.api.routeReplayEnabled(isChecked)
            }
        )
        bindSwitch(
            menuBinding.showCameraDebugInfo,
            viewModel.showCameraDebugInfo,
            ::toggleShowCameraDebugInfo
        )
        menuBinding.buttonVoiceInstruction.setOnClickListener {
            binding.navigationView.api.getCurrentVoiceInstructionsPlayer()?.play(
                SpeechAnnouncement.Builder("This is a test voice instruction").build()
            ) {
                // no op
            }
        }
    }

    private fun toggleShowCameraDebugInfo(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showCameraDebugInfo = enabled
        }
    }

    //endregion

    //region Activity Options

    private fun initActivityOptions() {
        bindSwitch(
            menuBinding.toggleTheme,
            getValue = resources.configuration::isNightMode,
            setValue = {
                val themeMode =
                    if (it) {
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }
                toggleTheme(themeMode)
            }
        )

        bindSwitch(
            menuBinding.toggleFullscreen,
            getValue = { viewModel.fullScreen.value == true },
            setValue = {
                viewModel.fullScreen.value = it
                recreate()
            }
        )
    }

    //endregion

    //region Speed limit

    private fun initSpeedLimitOptions() {
        bindSwitch(
            menuBinding.toggleLegacySpeed,
            viewModel.useLegacy,
            ::toggleLegacySpeedLimit
        )

        bindSwitch(
            menuBinding.toggleShowSpeedInfoLegend,
            viewModel.showSpeedInfoLegend
        ) { updateSpeedInfoOptions() }

        bindSpinner(
            menuBinding.spinnerSpeedInfoSign,
            viewModel.speedInfoSign
        ) { updateSpeedInfoOptions() }
    }

    private fun updateSpeedInfoOptions() {
        val speedLimitSign = when (viewModel.speedInfoSign.value) {
            "MUTCD" -> SpeedLimitSign.MUTCD
            "VIENNA" -> SpeedLimitSign.VIENNA
            else -> null
        }
        val showLegend = viewModel.showSpeedInfoLegend.value ?: false

        binding.navigationView.customizeViewStyles {
            speedInfoOptions = MapboxSpeedInfoOptions.Builder()
                .renderWithSpeedSign(speedLimitSign)
                .showLegend(showLegend)
                .build()
        }
    }

    //endregion

    //region General Options

    private fun initGeneralOptions() {
        bindSwitch(
            menuBinding.toggleShowManeuver,
            viewModel.showManeuver,
            ::toggleShowManeuver
        )
        bindSwitch(
            menuBinding.toggleShowSpeedLimit,
            viewModel.showSpeedLimit,
            ::toggleShowSpeedLimit
        )
        bindSwitch(
            menuBinding.toggleShowRoadName,
            viewModel.showRoadName,
            ::toggleShowRoadName
        )
        bindSwitch(
            menuBinding.toggleShowActionButtons,
            viewModel.showActionButtons,
            ::toggleActionButtons
        )
        bindSpinner(
            menuBinding.spinnerProfile,
            viewModel.routingProfile,
            ::toggleRoutingProfile,
        )
        bindSwitch(
            menuBinding.toggleViewStyling,
            viewModel.useCustomStyles,
            ::toggleCustomStyles
        )
        bindSwitch(
            menuBinding.toggleCustomViews,
            viewModel.useCustomViews,
            ::toggleCustomViews
        )
        bindSwitch(
            menuBinding.useMetric,
            viewModel.distanceFormatterMetric,
            ::toggleUseMetric
        )
    }

    private fun toggleLegacySpeedLimit(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            speedLimitBinder = if (enabled) {
                legacySpeedLimitBinder()
            } else {
                defaultSpeedInfoBinder()
            }
        }
    }

    private fun toggleShowManeuver(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showManeuver = enabled
        }
    }

    private fun toggleShowSpeedLimit(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showSpeedLimit = enabled
        }
    }

    private fun toggleShowRoadName(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showRoadName = enabled
        }
    }

    private fun toggleActionButtons(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showActionButtons = enabled
        }
    }

    private fun toggleCustomStyles(enabled: Boolean) {
        if (enabled) {
            binding.navigationView.customizeViewStyles {
                maneuverViewOptions = customManeuverOptions()
                tripProgressStyle = R.style.MyCustomTripProgressStyle
                speedLimitStyle = R.style.MyCustomSpeedLimitStyle
                speedLimitTextAppearance = R.style.MyCustomSpeedLimitTextAppearance
                speedInfoOptions = MapboxSpeedInfoOptions
                    .Builder()
                    .speedInfoStyle(
                        SpeedInfoStyle().apply {
                            postedSpeedMutcdTextAppearance = R.style.MyCustomPostedSpeedAppearance
                            postedSpeedMutcdLayoutBackground = R.drawable.bg_custom_speed_info_mutcd
                        }
                    )
                    .build()
                destinationMarkerAnnotationOptions = PointAnnotationOptions().apply {
                    withIconImage(
                        ContextCompat.getDrawable(
                            this@MapboxNavigationViewCustomizedActivity,
                            R.drawable.mapbox_ic_marker
                        )!!.toBitmap()
                    )
                    withIconAnchor(IconAnchor.BOTTOM)
                }
                roadNameBackground = R.drawable.mapbox_bg_road_name
                roadNameTextAppearance = R.style.MyCustomRoadNameViewTextAppearance

                compassButtonStyle = R.style.MyCustomCompassButton
                cameraModeButtonStyle = R.style.MyCustomCameraModeButton
                audioGuidanceButtonStyle = R.style.MyCustomAudioGuidanceButton
                recenterButtonStyle = R.style.MyCustomRecenterButton

                routePreviewButtonStyle = R.style.MyCustomRoutePreviewButton
                startNavigationButtonStyle = R.style.MyCustomStartNavigationButton
                endNavigationButtonStyle = R.style.MyCustomEndNavigationButton
            }
        } else {
            binding.navigationView.customizeViewStyles {
                val context = this@MapboxNavigationViewCustomizedActivity
                maneuverViewOptions = defaultManeuverViewOptions()
                tripProgressStyle = defaultTripProgressStyle()
                speedLimitStyle = defaultSpeedLimitStyle()
                speedLimitTextAppearance = defaultSpeedLimitTextAppearance()
                speedInfoOptions = defaultSpeedInfoOptions()
                destinationMarkerAnnotationOptions =
                    defaultDestinationMarkerAnnotationOptions(context)
                roadNameBackground = defaultRoadNameBackground()
                roadNameTextAppearance = defaultRoadNameTextAppearance()

                compassButtonStyle = defaultCompassButtonStyle()
                cameraModeButtonStyle = defaultCameraModeButtonStyle()
                audioGuidanceButtonStyle = defaultAudioGuidanceButtonStyle()
                recenterButtonStyle = defaultRecenterButtonStyle()

                routePreviewButtonStyle = defaultRoutePreviewButtonStyle()
                endNavigationButtonStyle = defaultEndNavigationButtonStyle()
                startNavigationButtonStyle = defaultStartNavigationButtonStyle()
            }
        }
    }

    private fun toggleCustomViews(enabled: Boolean) {
        // This demonstrates that you can customize views at any time. You can also reset to
        // the default views.
        if (enabled) {
            binding.navigationView.customizeViewBinders {
                speedLimitBinder = CustomSpeedLimitViewBinder()
            }
            binding.navigationView.customizeViewOptions {
                routeLineOptions = customRouteLineOptions(applicationContext)
                routeArrowOptions = customRouteArrowOptions(applicationContext)
                mapStyleUriDay = Style.LIGHT
                mapStyleUriNight = Style.DARK
            }
            binding.navigationView.customizeViewStyles {
                locationPuckOptions = LocationPuckOptions
                    .Builder(applicationContext)
                    .freeDrivePuck(
                        LocationPuck2D(
                            bearingImage = ContextCompat.getDrawable(
                                this@MapboxNavigationViewCustomizedActivity,
                                R.drawable.ic_sv_puck
                            )
                        )
                    )
                    .build()
            }
        } else {
            // Reset defaults
            binding.navigationView.customizeViewBinders {
                speedLimitBinder = UIBinder.USE_DEFAULT
            }
            binding.navigationView.customizeViewOptions {
                routeLineOptions = defaultRouteLineOptions(applicationContext)
                routeArrowOptions = defaultRouteArrowOptions(applicationContext)
                mapStyleUriDay = NavigationStyles.NAVIGATION_DAY_STYLE
                mapStyleUriNight = NavigationStyles.NAVIGATION_NIGHT_STYLE
            }
            binding.navigationView.customizeViewStyles {
                locationPuckOptions = defaultLocationPuckOptions(applicationContext)
            }
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

    private fun toggleRoutingProfile(profile: String) {
        val routingProfile = when (profile) {
            "DRIVING-TRAFFIC" -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            "DRIVING" -> DirectionsCriteria.PROFILE_DRIVING
            "WALKING" -> DirectionsCriteria.PROFILE_WALKING
            "CYCLING" -> DirectionsCriteria.PROFILE_CYCLING
            else -> {
                binding.navigationView.setRouteOptionsInterceptor(null)
                return
            }
        }
        binding.navigationView.setRouteOptionsInterceptor { defaultBuilder ->
            defaultBuilder.layers(null).applyDefaultNavigationOptions(routingProfile)
        }
    }

    //endregion

    //region Map Options

    private fun initMapOptions() {
        bindSwitch(
            menuBinding.toggleCustomMap,
            viewModel.showCustomMapView,
            ::toggleCustomMap
        )

        bindSwitch(
            menuBinding.toggleEnableScalebar,
            viewModel.enableScalebar,
            ::toggleEnableScalebar
        )

        bindSwitch(
            menuBinding.toggleBuildingHighlight,
            viewModel.enableBuildingHighlight,
            ::toggleBuildingHighlight
        )

        bindSwitch(
            menuBinding.toggleBuildingHighlightCustomization,
            viewModel.enableBuildingHighlightCustomization,
            ::toggleBuildingHighlightCustomization
        )

        bindSpinner(
            menuBinding.spinnerNavPuck,
            viewModel.navigationPuck,
            ::toggleNavigationPuck
        )
    }

    private fun toggleNavigationPuck(name: String) {
        val context = this
        binding.navigationView.customizeViewStyles {
            locationPuckOptions = if (name == "--") {
                LocationPuckOptions.Builder(context).build()
            } else {
                LocationPuckOptions.Builder(context)
                    .defaultPuck(NavPuck.valueOf(name).getLocationPuck(context))
                    .idlePuck(regularPuck(context))
                    .build()
            }
        }
    }

    private fun toggleCustomMap(enabled: Boolean) {
        // Demonstrate map customization
        if (enabled) {
            binding.navigationView.customizeViewBinders {
                mapViewBinder = CustomMapViewBinder()
            }
        } else {
            binding.navigationView.customizeViewBinders {
                mapViewBinder = MapViewBinder.defaultBinder()
            }
        }
    }

    private fun toggleEnableScalebar(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showMapScalebar = enabled
        }
    }

    private fun toggleBuildingHighlight(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            enableBuildingHighlightOnArrival = enabled
        }
    }

    private fun toggleBuildingHighlightCustomization(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            buildingHighlightOptions = if (enabled) {
                MapboxBuildingHighlightOptions.Builder()
                    .fillExtrusionColor(Color.parseColor("#FF0000"))
                    .fillExtrusionOpacity(0.8)
                    .build()
            } else {
                defaultBuildingHighlightOptions(this@MapboxNavigationViewCustomizedActivity)
            }
        }
    }

    //endregion

    //region Actions Options

    private fun initActionsOptions() {
        bindSwitch(
            menuBinding.toggleShowCompassButton,
            viewModel.actionsShowCompassButton,
            ::toggleShowCompassButton
        )
        bindSwitch(
            menuBinding.toggleShowCameraButton,
            viewModel.actionsShowCameraButton,
            ::toggleShowCameraModeButton
        )
        bindSwitch(
            menuBinding.toggleShowAudioButton,
            viewModel.actionsShowAudioButton,
            ::toggleShowAudioButton
        )
        bindSwitch(
            menuBinding.toggleShowRecenterButton,
            viewModel.actionsShowRecenterButton,
            ::toggleShowRecenterButton
        )
        bindSwitch(
            menuBinding.toggleAdditionalActionButtons,
            viewModel.actionsAdditionalButtons,
            ::toggleAdditionalActionButtons
        )
        bindSwitch(
            menuBinding.toggleCustomCompassButton,
            viewModel.actionsCustomCompassButton,
            ::toggleCustomCompassButton
        )
        bindSwitch(
            menuBinding.toggleCustomCameraModeButton,
            viewModel.actionsCustomCameraButton,
            ::toggleCustomCameraButton
        )
        bindSwitch(
            menuBinding.toggleCustomAudioButton,
            viewModel.actionsCustomAudioButton,
            ::toggleCustomAudioButton
        )
        bindSwitch(
            menuBinding.toggleCustomRecenterButton,
            viewModel.actionsCustomRecenterButton,
            ::toggleCustomRecenterButton
        )
        bindSwitch(
            menuBinding.toggleCustomActionsLayout,
            viewModel.actionsCustomLayout,
            ::toggleCustomActionsLayout
        )
    }

    private fun toggleShowCompassButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showCompassActionButton = enabled
        }
    }

    private fun toggleShowCameraModeButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showCameraModeActionButton = enabled
        }
    }

    private fun toggleShowAudioButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showToggleAudioActionButton = enabled
        }
    }

    private fun toggleShowRecenterButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showRecenterActionButton = enabled
        }
    }

    private fun toggleAdditionalActionButtons(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            customActionButtons = if (enabled) {
                listOf(
                    ActionButtonDescription(customActionButton("button 1"), START),
                    ActionButtonDescription(customActionButton("button 2"), START),
                    ActionButtonDescription(customActionButton("button 3"), END),
                    ActionButtonDescription(customActionButton("button 4"), END)
                )
            } else {
                emptyList()
            }
        }
    }

    private fun toggleCustomCompassButton(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            actionCompassButtonBinder = if (enabled) {
                UIBinder {
                    it.removeAllViews()
                    it.addView(
                        customActionButton("Compass").apply {
                            updateMargins(top = 10.dp, bottom = 10.dp)
                        }
                    )
                    UIComponent()
                }
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomCameraButton(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            actionCameraModeButtonBinder = if (enabled) {
                UIBinder {
                    it.removeAllViews()
                    it.addView(
                        customActionButton("Camera").apply {
                            updateMargins(top = 10.dp, bottom = 10.dp)
                        }
                    )
                    UIComponent()
                }
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomAudioButton(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            actionToggleAudioButtonBinder = if (enabled) {
                UIBinder {
                    it.removeAllViews()
                    it.addView(
                        customActionButton("Audio").apply {
                            updateMargins(top = 10.dp, bottom = 10.dp)
                        }
                    )
                    UIComponent()
                }
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomRecenterButton(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            actionRecenterButtonBinder = if (enabled) {
                UIBinder {
                    it.removeAllViews()
                    it.addView(
                        customActionButton("Recenter").apply {
                            updateMargins(top = 10.dp, bottom = 10.dp)
                        }
                    )
                    UIComponent()
                }
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomActionsLayout(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            actionButtonsBinder = if (enabled) {
                CustomActionButtonsBinder()
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    //endregion

    //region Info Panel Options

    private fun initInfoPanelOptions() {
        bindSwitch(
            menuBinding.toggleShowTripProgress,
            viewModel.infoPanelShowTripProgress,
            ::toggleShowTripProgress
        )
        bindSwitch(
            menuBinding.toggleShowRoutePreviewButton,
            viewModel.infoPanelShowRoutePreviewButton,
            ::toggleShowRoutePreviewButton
        )
        bindSwitch(
            menuBinding.toggleShowStartNavButton,
            viewModel.infoPanelShowStartNavigationButton,
            ::toggleShowStartNavButton
        )
        bindSwitch(
            menuBinding.toggleShowEndNavButton,
            viewModel.infoPanelShowEndNavigationButton,
            ::toggleShowEndNavButton
        )
        bindSpinner(
            menuBinding.spinnerCustomInfoPanelContent,
            viewModel.customInfoPanelContent,
            ::toggleCustomInfoPanelContent
        )
        bindSpinner(
            menuBinding.spinnerCustomInfoPanelPeekHeight,
            viewModel.customInfoPanelPeekHeight,
            ::toggleCustomInfoPanelPeekHeight,
        )
        bindSwitch(
            menuBinding.toggleCustomInfoPanelEndNavButton,
            viewModel.showCustomInfoPanelEndNavButton,
            ::toggleCustomInfoPanelEndNavButton
        )
        bindSpinner(
            menuBinding.spinnerCustomInfoPanelLayout,
            viewModel.customInfoPanelLayout,
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
            menuBinding.toggleEnableDestinationPreview,
            viewModel.enableDestinationPreview,
            ::toggleEnableDestinationPreview,
        )

        bindSwitch(
            menuBinding.toggleIsInfoPanelHideable,
            viewModel.isInfoPanelHideable,
            ::toggleInfoPanelHiding
        )

        bindSpinner(
            menuBinding.spinnerInfoPanelVisibilityOverride,
            viewModel.infoPanelStateOverride,
            ::toggleInfoPanelState
        )

        bindSwitch(
            menuBinding.toggleShowPoiName,
            viewModel.infoPanelShowPoiName,
            ::toggleShowPoiName
        )

        bindSwitch(
            menuBinding.toggleShowArrivalText,
            viewModel.infoPanelShowArrivalText,
            ::toggleShowArrivalText
        )
    }

    private fun toggleShowTripProgress(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showTripProgress = enabled
        }
    }

    private fun toggleShowRoutePreviewButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showRoutePreviewButton = enabled
        }
    }

    private fun toggleShowStartNavButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showStartNavigationButton = enabled
        }
    }

    private fun toggleShowEndNavButton(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showEndNavigationButton = enabled
        }
    }

    private fun toggleInfoPanelState(state: String) {
        val newState = when (state) {
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

    private fun toggleCustomInfoPanelContent(contentSize: String) {
        binding.navigationView.customizeViewBinders {
            when (contentSize) {
                "SMALL",
                "LARGE" -> {
                    infoPanelContentBinder = UIBinder { viewGroup ->
                        supportFragmentManager.beginTransaction()
                            .replace(
                                viewGroup.id,
                                CustomInfoPanelDetailsFragment.create(contentSize)
                            )
                            .commitAllowingStateLoss()
                        UIComponent()
                    }
                }
                else -> {
                    infoPanelContentBinder = UIBinder.USE_DEFAULT
                }
            }
        }
    }

    private fun toggleCustomInfoPanelPeekHeight(peekHeight: String) {
        val defaultPeekHeight = ViewStyleCustomization.defaultInfoPanelPeekHeight(context = this)
        binding.navigationView.customizeViewStyles {
            infoPanelPeekHeight = when (peekHeight) {
                "LARGE" -> defaultPeekHeight * 2
                else -> defaultPeekHeight
            }
        }
    }

    private fun toggleCustomInfoPanelEndNavButton(enabled: Boolean) {
        binding.navigationView.customizeViewBinders {
            infoPanelEndNavigationButtonBinder = if (enabled) {
                CustomInfoPanelEndNavButtonBinder(binding.navigationView.api)
            } else {
                UIBinder.USE_DEFAULT
            }
        }
    }

    private fun toggleCustomInfoPanelLayout(selection: String) {
        when (selection) {
            "CUSTOM" -> {
                customInfoPanel.setEnabled(true)
                customInfoPanelFixedHeight.setEnabled(false)
            }
            "CUSTOM WITH FIXED HEIGHT" -> {
                customInfoPanel.setEnabled(false)
                customInfoPanelFixedHeight.setEnabled(true)
            }
            else -> {
                customInfoPanel.setEnabled(false)
                customInfoPanelFixedHeight.setEnabled(false)
                binding.navigationView.customizeViewBinders {
                    infoPanelBinder = InfoPanelBinder.defaultBinder()
                }
                binding.navigationView.customizeViewStyles {
                    val context = this@MapboxNavigationViewCustomizedActivity
                    infoPanelPeekHeight = ViewStyleCustomization.defaultInfoPanelPeekHeight(context)
                }
            }
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

    private fun toggleEnableDestinationPreview(enable: Boolean) {
        if (enable) {
            binding.navigationView.removeListener(navigationStateListener)
            binding.navigationView.unregisterMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = false
        } else {
            binding.navigationView.addListener(navigationStateListener)
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

    private fun toggleShowPoiName(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showPoiName = enabled
        }
    }

    private fun toggleShowArrivalText(enabled: Boolean) {
        binding.navigationView.customizeViewOptions {
            showArrivalText = enabled
        }
    }

    //endregion

    private fun customActionButton(text: String): View {
        return AppCompatTextView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(72.dp, WRAP_CONTENT)
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
            val lastLocation = lastLocation ?: return false
            val mapboxNavigation = MapboxNavigationApp.current() ?: return false
            val routeOptions = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this@MapboxNavigationViewCustomizedActivity)
                .coordinatesList(listOf(lastLocation.toPoint(), point))
                .build()
            val callback = object : NavigationRouterCallback {

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin,
                ) {
                    binding.navigationView.api.startRoutePreview(routes)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }

            mapboxNavigation.requestRoutes(routeOptions, callback)
            return false
        }
    }

    /**
     * This is a feature for Mapbox Navigation development. When the notification service is
     * available you are able to send commands and handle state changes with dumpsys.
     *
     * $ adb shell dumpsys activity service com.mapbox.navigation.core.trip.service.NavigationNotificationService help
     */
    private val dumpCommands = object : MapboxNavigationObserver {

        private val interceptors by lazy {
            arrayOf(
                DistanceFormatterDumpInterceptor(),
                NavigationViewApiDumpInterceptor(binding.navigationView.api)
                // Add your interceptors here
            )
        }

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            MapboxDumpRegistry.addInterceptors(*interceptors)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            MapboxDumpRegistry.removeInterceptors(*interceptors)
        }
    }

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {
            // no impl
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            lastLocation = locationMatcherResult.enhancedLocation
        }
    }

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
            restoreDefaultHeaderBinder()
        }

        override fun onRoutePreview() {
            restoreDefaultHeaderBinder()
        }

        override fun onActiveNavigation() {
            restoreDefaultHeaderBinder()
        }

        override fun onArrival() {
            restoreDefaultHeaderBinder()
        }

        private fun restoreDefaultHeaderBinder() {
            binding.navigationView.customizeViewBinders {
                infoPanelHeaderBinder = UIBinder.USE_DEFAULT
            }
        }
    }

    private val navigationStateListener = object : NavigationViewListener() {

        override fun onFreeDrive() {
            binding.navigationView.registerMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = false
        }

        override fun onDestinationPreview() {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = false
        }

        override fun onRoutePreview() {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = true
        }

        override fun onActiveNavigation() {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = false
        }

        override fun onArrival() {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
            onBackPressCallback.isEnabled = false
        }
    }

    private val onBackPressCallback =
        onBackPressedDispatcher.addCallback(owner = this, enabled = false) {
            binding.navigationView.api.startFreeDrive()
        }

    private enum class NavPuck(
        val getLocationPuck: (context: Context) -> LocationPuck
    ) {
        NAV_PUCK_2A({ context ->
            LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon2a,
                )
            )
        }),
        NAV_PUCK_2B({ context ->
            LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon2b,
                )
            )
        }),
        LEGACY({ context ->
            LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon,
                )
            )
        }),
        QUACK_QUACK({
            LocationPuck3D(
                modelUri = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Embedded/Duck.gltf", // ktlint-disable
                modelScaleExpression = literal(listOf(30, 30, 30)).toJson(),
                modelRotation = listOf(0f, 0f, -90f)
            )
        });
    }
}
