package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.app.Fragment;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.instruction.ImageCoordinator;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.instruction.NavigationAlertView;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMapInstanceState;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

/**
 * View that creates the drop-in UI.
 * <p>
 * Once started, this view will check if the {@link Activity} that inflated
 * it was launched with a {@link DirectionsRoute}.
 * <p>
 * Or, if not found, this view will look for a set of {@link Point} coordinates.
 * In the latter case, a new {@link DirectionsRoute} will be retrieved from {@link NavigationRoute}.
 * <p>
 * Once valid data is obtained, this activity will immediately begin navigation
 * with {@link MapboxNavigation}.
 * <p>
 * If launched with the simulation boolean set to true, a {@link ReplayRouteLocationEngine}
 * will be initialized and begin pushing updates.
 * <p>
 * This activity requires user permissions ACCESS_FINE_LOCATION
 * and ACCESS_COARSE_LOCATION have already been granted.
 * <p>
 * A Mapbox access token must also be set by the developer (to initialize navigation).
 *
 * @since 0.7.0
 */
public class NavigationView extends CoordinatorLayout implements LifecycleObserver, OnMapReadyCallback,
  NavigationContract.View {

  private static final String MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state";
  private static final int INVALID_STATE = 0;
  private MapView mapView;
  private InstructionView instructionView;
  private SummaryBottomSheet summaryBottomSheet;
  private BottomSheetBehavior summaryBehavior;
  private ImageButton cancelBtn;
  private RecenterButton recenterBtn;
  private ImageButton routeOverviewBtn;

  private NavigationPresenter navigationPresenter;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private NavigationViewModel navigationViewModel;
  private NavigationMapboxMap navigationMap;
  private OnNavigationReadyCallback onNavigationReadyCallback;
  private NavigationOnCameraTrackingChangedListener onTrackingChangedListener;
  private NavigationMapboxMapInstanceState mapInstanceState;
  private CameraPosition initialMapCameraPosition;
  private boolean isMapInitialized;
  private boolean isSubscribed;
  private boolean isOffline;

  public NavigationView(Context context) {
    this(context, null);
  }

  public NavigationView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public NavigationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    ThemeSwitcher.setTheme(context, attrs);
    initializeView();
  }

  /**
   * Uses savedInstanceState as a cue to restore state (if not null).
   *
   * @param savedInstanceState to restore state if not null
   */
  public void onCreate(@Nullable Bundle savedInstanceState) {
    updateSavedInstanceStateMapStyle(savedInstanceState);
    mapView.onCreate(savedInstanceState);
    updatePresenterState(savedInstanceState);
    navigationViewModel.onCreate();
  }

  /**
   * Low memory must be reported so the {@link MapView}
   * can react appropriately.
   */
  public void onLowMemory() {
    mapView.onLowMemory();
  }

  /**
   * If the instruction list is showing and onBackPressed is called,
   * hide the instruction list and do not hide the activity or fragment.
   *
   * @return true if back press handled, false if not
   */
  public boolean onBackPressed() {
    return instructionView.handleBackPressed();
  }

  /**
   * Used to store the bottomsheet state and re-center
   * button visibility.  As well as anything the {@link MapView}
   * needs to store in the bundle.
   *
   * @param outState to store state variables
   */
  public void onSaveInstanceState(Bundle outState) {
    int bottomSheetBehaviorState = summaryBehavior == null ? INVALID_STATE : summaryBehavior.getState();
    NavigationViewInstanceState navigationViewInstanceState = new NavigationViewInstanceState(bottomSheetBehaviorState,
      recenterBtn.getVisibility(), instructionView.isShowingInstructionList());
    String instanceKey = getContext().getString(R.string.navigation_view_instance_state);
    outState.putParcelable(instanceKey, navigationViewInstanceState);
    outState.putBoolean(getContext().getString(R.string.navigation_running), navigationViewModel.isRunning());
    mapView.onSaveInstanceState(outState);
    saveNavigationMapInstanceState(outState);
  }

  /**
   * Used to restore the bottomsheet state and re-center
   * button visibility.  As well as the {@link MapView}
   * position prior to rotation.
   *
   * @param savedInstanceState to extract state variables
   */
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    String instanceKey = getContext().getString(R.string.navigation_view_instance_state);
    NavigationViewInstanceState navigationViewInstanceState = savedInstanceState.getParcelable(instanceKey);
    recenterBtn.setVisibility(navigationViewInstanceState.getRecenterButtonVisibility());
    resetBottomSheetState(navigationViewInstanceState.getBottomSheetBehaviorState());
    updateInstructionListState(navigationViewInstanceState.isInstructionViewVisible());
    mapInstanceState = savedInstanceState.getParcelable(MAP_INSTANCE_STATE_KEY);
  }

  /**
   * Called to ensure the {@link MapView} is destroyed
   * properly.
   * <p>
   * In an {@link Activity} this should be in {@link Activity#onDestroy()}.
   * <p>
   * In a {@link android.app.Fragment}, this should be in {@link Fragment#onDestroyView()}.
   */
  public void onDestroy() {
    shutdown();
  }

  public void onStart() {
    mapView.onStart();
    if (navigationMap != null) {
      navigationMap.onStart();
    }
  }

  public void onResume() {
    mapView.onResume();
  }

  public void onPause() {
    mapView.onPause();
  }

  public void onStop() {
    mapView.onStop();
    if (navigationMap != null) {
      navigationMap.onStop();
    }
  }

  /**
   * Fired after the map is ready, this is our cue to finish
   * setting up the rest of the plugins / location engine.
   * <p>
   * Also, we check for launch data (coordinates or route).
   *
   * @param mapboxMap used for route, camera, and location UI
   * @since 0.6.0
   */
  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    initializeNavigationMap(mapView, mapboxMap);
    onNavigationReadyCallback.onNavigationReady(navigationViewModel.isRunning());
    isMapInitialized = true;
  }

  @Override
  public void setSummaryBehaviorState(int state) {
    summaryBehavior.setState(state);
  }

  @Override
  public void setSummaryBehaviorHideable(boolean isHideable) {
    summaryBehavior.setHideable(isHideable);
  }

  @Override
  public boolean isSummaryBottomSheetHidden() {
    return summaryBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN;
  }


  @Override
  public void updateCameraTrackingMode(int trackingMode) {
    if (navigationMap != null) {
      navigationMap.updateCameraTrackingMode(trackingMode);
    }
  }

  @Override
  public void resetCameraPosition() {
    if (navigationMap != null) {
      navigationMap.resetPadding();
      navigationMap.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    }
  }

  @Override
  public void showRecenterBtn() {
    recenterBtn.show();
  }

  @Override
  public void hideRecenterBtn() {
    recenterBtn.hide();
  }

  @Override
  public boolean isRecenterButtonVisible() {
    return recenterBtn.getVisibility() == View.VISIBLE;
  }

  @Override
  public void drawRoute(DirectionsRoute directionsRoute) {
    if (navigationMap != null) {
      navigationMap.drawRoute(directionsRoute);
    }
  }

  private void initializeNavigationMap(MapView mapView, MapboxMap map) {
    navigationMap = new NavigationMapboxMap(mapView, map);
    if (mapInstanceState != null) {
      navigationMap.restoreFrom(mapInstanceState);
      return;
    }
    if (initialMapCameraPosition != null) {
      map.setCameraPosition(initialMapCameraPosition);
    }
  }

  @Override
  public void addMarker(Point position) {
    if (navigationMap != null) {
      navigationMap.addMarker(getContext(), position);
    }
  }

  public void clearMarkers() {
    if (navigationMap != null) {
      navigationMap.clearMarkers();
    }
  }

  public void updateWaynameView(String wayname) {
    if (navigationMap != null) {
      navigationMap.updateWaynameView(wayname);
    }
  }

  @Override
  public void updateWaynameVisibility(boolean isVisible) {
    if (navigationMap != null) {
      navigationMap.updateWaynameVisibility(isVisible);
    }
  }

  public void updateWaynameQueryMap(boolean isEnabled) {
    if (navigationMap != null) {
      navigationMap.updateWaynameQueryMap(isEnabled);
    }
  }

  @Override
  public void takeScreenshot() {
    if (navigationMap != null) {
      navigationMap.takeScreenshot(new NavigationSnapshotReadyCallback(this, navigationViewModel));
    }
  }

  /**
   * Used when starting this {@link android.app.Activity}
   * for the first time.
   * <p>
   * Zooms to the beginning of the {@link DirectionsRoute}.
   *
   * @param directionsRoute where camera should move to
   */
  @Override
  public void startCamera(DirectionsRoute directionsRoute) {
    if (navigationMap != null) {
      navigationMap.startCamera(directionsRoute);
    }
  }

  /**
   * Used after configuration changes to resume the camera
   * to the last location update from the Navigation SDK.
   *
   * @param location where the camera should move to
   */
  @Override
  public void resumeCamera(Location location) {
    if (navigationMap != null) {
      navigationMap.resumeCamera(location);
    }
  }

  @Override
  public void updateNavigationMap(Location location) {
    if (navigationMap != null) {
      navigationMap.updateLocation(location);
    }
  }

  @Override
  public void updateCameraRouteOverview() {
    if (navigationMap != null) {
      int[] padding = buildRouteOverviewPadding(getContext());
      navigationMap.showRouteOverview(padding);
    }
  }

  /**
   * Should be called when this view is completely initialized.
   *
   * @param options with containing route / coordinate data
   */
  public void startNavigation(NavigationViewOptions options) {
    initializeNavigation(options);
  }

  /**
   * Call this when the navigation session needs to end navigation without finishing the whole view
   *
   * @since 0.16.0
   */
  public void stopNavigation() {
    navigationViewModel.stopNavigation();
  }

  /**
   * Should be called after {@link NavigationView#onCreate(Bundle)}.
   * <p>
   * This method adds the {@link OnNavigationReadyCallback},
   * which will fire the ready events for this view.
   *
   * @param onNavigationReadyCallback to be set to this view
   */
  public void initialize(OnNavigationReadyCallback onNavigationReadyCallback) {
    this.onNavigationReadyCallback = onNavigationReadyCallback;
    if (!isMapInitialized) {
      mapView.getMapAsync(this);
    } else {
      onNavigationReadyCallback.onNavigationReady(navigationViewModel.isRunning());
    }
  }

  /**
   * Should be called after {@link NavigationView#onCreate(Bundle)}.
   * <p>
   * This method adds the {@link OnNavigationReadyCallback},
   * which will fire the ready events for this view.
   * <p>
   * This method also accepts a {@link CameraPosition} that will be set as soon as the map is
   * ready.  Note, this position is ignored during rotation in favor of the last known map position.
   *
   * @param onNavigationReadyCallback to be set to this view
   * @param initialMapCameraPosition  to be shown once the map is ready
   */
  public void initialize(OnNavigationReadyCallback onNavigationReadyCallback,
                         @NonNull CameraPosition initialMapCameraPosition) {
    this.onNavigationReadyCallback = onNavigationReadyCallback;
    this.initialMapCameraPosition = initialMapCameraPosition;
    if (!isMapInitialized) {
      mapView.getMapAsync(this);
    } else {
      onNavigationReadyCallback.onNavigationReady(navigationViewModel.isRunning());
    }
  }

  /**
   * Gives the ability to manipulate the map directly for anything that might not currently be
   * supported. This returns null until the view is initialized.
   * <p>
   * The {@link NavigationMapboxMap} gives direct access to the map UI (location marker, route, etc.).
   *
   * @return navigation mapbox map object, or null if view has not been initialized
   */
  @Nullable
  public NavigationMapboxMap retrieveNavigationMapboxMap() {
    return navigationMap;
  }

  /**
   * Returns the instance of {@link MapboxNavigation} powering the {@link NavigationView}
   * once navigation has started.  Will return null if navigation has not been started with
   * {@link NavigationView#startNavigation(NavigationViewOptions)}.
   *
   * @return mapbox navigation, or null if navigation has not started
   */
  @Nullable
  public MapboxNavigation retrieveMapboxNavigation() {
    return navigationViewModel.retrieveNavigation();
  }

  /**
   * Returns the sound button used for muting instructions
   *
   * @return sound button
   */
  public NavigationButton retrieveSoundButton() {
    return instructionView.retrieveSoundButton();
  }

  /**
   * Returns the feedback button for sending feedback about navigation
   *
   * @return feedback button
   */
  public NavigationButton retrieveFeedbackButton() {
    return instructionView.retrieveFeedbackButton();
  }

  /**
   * Returns the re-center button for recentering on current location
   *
   * @return recenter button
   */
  public NavigationButton retrieveRecenterButton() {
    return recenterBtn;
  }

  /**
   * Returns the {@link NavigationAlertView} that is shown during off-route events with
   * "Report a Problem" text.
   *
   * @return alert view that is used in the instruction view
   */
  public NavigationAlertView retrieveAlertView() {
    return instructionView.retrieveAlertView();
  }

  /**
   * Initializes the offline data used for fetching offline routes.
   * <p>
   * This method must be called before {@link MapboxNavigation#findOfflineRouteFor(Point, Point, Point[])} /
   * {@link MapboxNavigation#findOfflineRouteFor(Location, Point, Point[])}.
   *
   * @param tileFilePath        path to directory containing tile data
   * @param translationsDirPath path to directory containing OSRMTI translations
   */
  public void initializeOfflineData(String tileFilePath, String translationsDirPath) {
    navigationViewModel.initializeOfflineData(tileFilePath, translationsDirPath);
  }

  /**
   * Sets the NavigationView to use or not use offline data. This call should be followed by a call
   * to initializeOfflineData.
   *
   * @param isOffline whether the map should load offline or not
   */
  public void setOffline(boolean isOffline) {
    this.isOffline = isOffline;
    if (navigationViewModel != null) {
      navigationViewModel.setOffline(isOffline);
    }
  }

  private void initializeView() {
    inflate(getContext(), R.layout.navigation_view_layout, this);
    bind();
    initializeNavigationViewModel();
    initializeNavigationEventDispatcher();
    initializeNavigationPresenter();
    initializeInstructionListListener();
    initializeSummaryBottomSheet();
  }

  private void bind() {
    mapView = findViewById(R.id.navigationMapView);
    instructionView = findViewById(R.id.instructionView);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    cancelBtn = findViewById(R.id.cancelBtn);
    recenterBtn = findViewById(R.id.recenterBtn);
    routeOverviewBtn = findViewById(R.id.routeOverviewBtn);
  }

  private void initializeNavigationViewModel() {
    try {
      navigationViewModel = ViewModelProviders.of((FragmentActivity) getContext()).get(NavigationViewModel.class);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  private void initializeSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setHideable(false);
    summaryBehavior.setBottomSheetCallback(new SummaryBottomSheetCallback(navigationPresenter,
      navigationViewEventDispatcher));
  }

  private void initializeNavigationEventDispatcher() {
    navigationViewEventDispatcher = new NavigationViewEventDispatcher();
    navigationViewModel.initializeEventDispatcher(navigationViewEventDispatcher);
  }

  private void initializeInstructionListListener() {
    instructionView.setInstructionListListener(new NavigationInstructionListListener(navigationPresenter,
      navigationViewEventDispatcher));
  }

  private void updateSavedInstanceStateMapStyle(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      String mapStyleUrl = ThemeSwitcher.retrieveMapStyle(getContext());
      savedInstanceState.putString(MapboxConstants.STATE_STYLE_URL, mapStyleUrl);
    }
  }

  private void saveNavigationMapInstanceState(Bundle outState) {
    if (navigationMap != null) {
      navigationMap.saveStateWith(MAP_INSTANCE_STATE_KEY, outState);
    }
  }

  private void resetBottomSheetState(int bottomSheetState) {
    if (bottomSheetState > INVALID_STATE) {
      boolean isShowing = bottomSheetState == BottomSheetBehavior.STATE_EXPANDED;
      summaryBehavior.setHideable(!isShowing);
      summaryBehavior.setState(bottomSheetState);
    }
  }

  private void updateInstructionListState(boolean visible) {
    if (visible) {
      instructionView.showInstructionList();
    } else {
      instructionView.hideInstructionList();
    }
  }

  private int[] buildRouteOverviewPadding(Context context) {
    Resources resources = context.getResources();
    int leftRightPadding = (int) resources.getDimension(R.dimen.route_overview_left_right_padding);
    int paddingBuffer = (int) resources.getDimension(R.dimen.route_overview_buffer_padding);
    int instructionHeight = (int) (resources.getDimension(R.dimen.instruction_layout_height) + paddingBuffer);
    int summaryHeight = (int) resources.getDimension(R.dimen.summary_bottomsheet_height);
    return new int[] {leftRightPadding, instructionHeight, leftRightPadding, summaryHeight};
  }

  private boolean isChangingConfigurations() {
    try {
      return ((FragmentActivity) getContext()).isChangingConfigurations();
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  private void initializeNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  private void updatePresenterState(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      String navigationRunningKey = getContext().getString(R.string.navigation_running);
      boolean resumeState = savedInstanceState.getBoolean(navigationRunningKey);
      navigationPresenter.updateResumeState(resumeState);
    }
  }

  private void initializeNavigation(NavigationViewOptions options) {
    establish(options);
    MapboxNavigation navigation = navigationViewModel.initialize(options);
    setOffline(isOffline);
    initializeNavigationListeners(options, navigation);
    setupNavigationMapboxMap(options);

    if (!isSubscribed) {
      initializeClickListeners();
      initializeOnCameraTrackingChangedListener();
      subscribeViewModels();
    }
  }

  private void initializeClickListeners() {
    cancelBtn.setOnClickListener(new CancelBtnClickListener(navigationViewEventDispatcher));
    recenterBtn.addOnClickListener(new RecenterBtnClickListener(navigationPresenter));
    routeOverviewBtn.setOnClickListener(new RouteOverviewBtnClickListener(navigationPresenter));
  }

  private void initializeOnCameraTrackingChangedListener() {
    onTrackingChangedListener = new NavigationOnCameraTrackingChangedListener(navigationPresenter, summaryBehavior);
    navigationMap.addOnCameraTrackingChangedListener(onTrackingChangedListener);
  }

  private void establish(NavigationViewOptions options) {
    LocaleUtils localeUtils = new LocaleUtils();
    establishDistanceFormatter(localeUtils, options);
    establishTimeFormat(options);
  }

  private void establishDistanceFormatter(LocaleUtils localeUtils, NavigationViewOptions options) {
    String unitType = establishUnitType(localeUtils, options);
    String language = establishLanguage(localeUtils, options);
    int roundingIncrement = establishRoundingIncrement(options);
    DistanceFormatter distanceFormatter = new DistanceFormatter(getContext(), language, unitType, roundingIncrement);

    instructionView.setDistanceFormatter(distanceFormatter);
    summaryBottomSheet.setDistanceFormatter(distanceFormatter);
  }

  private int establishRoundingIncrement(NavigationViewOptions navigationViewOptions) {
    MapboxNavigationOptions mapboxNavigationOptions = navigationViewOptions.navigationOptions();
    return mapboxNavigationOptions.roundingIncrement();
  }

  private String establishLanguage(LocaleUtils localeUtils, NavigationViewOptions options) {
    return localeUtils.getNonEmptyLanguage(getContext(), options.directionsRoute().voiceLanguage());
  }

  private String establishUnitType(LocaleUtils localeUtils, NavigationViewOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    String voiceUnits = routeOptions == null ? null : routeOptions.voiceUnits();
    return localeUtils.retrieveNonNullUnitType(getContext(), voiceUnits);
  }

  private void establishTimeFormat(NavigationViewOptions options) {
    @NavigationTimeFormat.Type
    int timeFormatType = options.navigationOptions().timeFormatType();
    summaryBottomSheet.setTimeFormat(timeFormatType);
  }

  private void initializeNavigationListeners(NavigationViewOptions options, MapboxNavigation navigation) {
    navigationMap.addProgressChangeListener(navigation);
    navigationViewEventDispatcher.initializeListeners(options, navigation);
  }

  private void setupNavigationMapboxMap(NavigationViewOptions options) {
    navigationMap.updateWaynameQueryMap(options.waynameChipEnabled());
  }

  /**
   * Subscribes the {@link InstructionView} and {@link SummaryBottomSheet} to the {@link NavigationViewModel}.
   * <p>
   * Then, creates an instance of {@link NavigationViewSubscriber}, which takes a presenter.
   * <p>
   * The subscriber then subscribes to the view models, setting up the appropriate presenter / listener
   * method calls based on the {@link android.arch.lifecycle.LiveData} updates.
   */
  private void subscribeViewModels() {
    instructionView.subscribe(navigationViewModel);
    summaryBottomSheet.subscribe(navigationViewModel);

    NavigationViewSubscriber subscriber = new NavigationViewSubscriber(navigationPresenter);
    subscriber.subscribe(((LifecycleOwner) getContext()), navigationViewModel);
    isSubscribed = true;
  }

  private void shutdown() {
    if (navigationMap != null) {
      navigationMap.removeOnCameraTrackingChangedListener(onTrackingChangedListener);
    }
    navigationViewEventDispatcher.onDestroy(navigationViewModel.retrieveNavigation());
    mapView.onDestroy();
    navigationViewModel.onDestroy(isChangingConfigurations());
    ImageCoordinator.getInstance().shutdown();
    navigationMap = null;
  }
}