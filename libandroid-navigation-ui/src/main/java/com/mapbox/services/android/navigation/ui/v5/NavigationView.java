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
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.instruction.ImageCoordinator;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
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
 * If launched with the simulation boolean set to true, a {@link MockLocationEngine}
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
  private MapboxMap.OnMoveListener onMoveListener;
  private boolean isInitialized;

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
    if (summaryBehavior != null) {
      outState.putInt(getContext().getString(R.string.bottom_sheet_state), summaryBehavior.getState());
    }
    outState.putBoolean(getContext().getString(R.string.recenter_btn_visible),
      recenterBtn.getVisibility() == View.VISIBLE);
    outState.putBoolean(getContext().getString(R.string.navigation_running), navigationViewModel.isRunning());
    outState.putBoolean(getContext().getString(R.string.instruction_view_visible),
      instructionView.isShowingInstructionList());
    mapView.onSaveInstanceState(outState);
  }

  /**
   * Used to restore the bottomsheet state and re-center
   * button visibility.  As well as the {@link MapView}
   * position prior to rotation.
   *
   * @param savedInstanceState to extract state variables
   */
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    boolean isVisible = savedInstanceState.getBoolean(getContext().getString(R.string.recenter_btn_visible));
    recenterBtn.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    int bottomSheetState = savedInstanceState.getInt(getContext().getString(R.string.bottom_sheet_state));
    resetBottomSheetState(bottomSheetState);
    boolean instructionViewVisible = savedInstanceState.getBoolean(getContext().getString(
      R.string.instruction_view_visible));
    updateInstructionListState(instructionViewVisible);
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
    navigationViewEventDispatcher.onDestroy(navigationViewModel.getNavigation());
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
      navigationMap.removeOnMoveListener(onMoveListener);
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
  public void updateCameraTrackingEnabled(boolean isEnabled) {
    if (navigationMap != null) {
      navigationMap.updateCameraTrackingEnabled(isEnabled);
    }
  }

  @Override
  public void resetCameraPosition() {
    if (navigationMap != null) {
      navigationMap.resetCameraPosition();
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
    if (!isInitialized) {
      initializeNavigation(options);
    } else {
      navigationViewModel.updateNavigation(options);
    }
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
   * which will fire ready / cancel events for this view.
   *
   * @param onNavigationReadyCallback to be set to this view
   */
  public void initialize(OnNavigationReadyCallback onNavigationReadyCallback) {
    this.onNavigationReadyCallback = onNavigationReadyCallback;
    mapView.getMapAsync(this);
  }

  /**
   * Gives the ability to manipulate the map directly for anything that might not currently be
   * supported. This returns null until the view is initialized
   *
   * @return mapbox map object, or null if view has not been initialized
   */
  @Nullable
  public MapboxMap retrieveMapboxMap() {
    if (navigationMap == null) {
      return null;
    }
    return navigationMap.retrieveMap();
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

  /**
   * Sets the {@link BottomSheetBehavior} based on the last state stored
   * in {@link Bundle} savedInstanceState.
   *
   * @param bottomSheetState retrieved from savedInstanceState
   */
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
    initializeClickListeners();
    initializeOnMoveListener();
    establish(options);
    MapboxNavigation navigation = navigationViewModel.initializeNavigation(options);
    initializeNavigationListeners(options, navigation);
    setupNavigationMapboxMap(options);

    subscribeViewModels();
    isInitialized = true;
  }

  private void initializeClickListeners() {
    cancelBtn.setOnClickListener(new CancelBtnClickListener(navigationViewEventDispatcher));
    recenterBtn.setOnClickListener(new RecenterBtnClickListener(navigationPresenter));
    routeOverviewBtn.setOnClickListener(new RouteOverviewBtnClickListener(navigationPresenter));
  }

  private void initializeOnMoveListener() {
    onMoveListener = new NavigationOnMoveListener(navigationPresenter, summaryBehavior);
    navigationMap.addOnMoveListener(onMoveListener);
  }

  private void establish(NavigationViewOptions options) {
    establishLanguage(options);
    establishUnitType(options);
    establishTimeFormat(options);
  }

  private void establishLanguage(NavigationViewOptions options) {
    LocaleUtils localeUtils = new LocaleUtils();
    String language = localeUtils.getNonEmptyLanguage(getContext(), options.directionsRoute().voiceLanguage());
    instructionView.setLanguage(language);
    summaryBottomSheet.setLanguage(language);
  }

  private void establishUnitType(NavigationViewOptions options) {
    String unitType = options.directionsRoute().routeOptions().voiceUnits();
    instructionView.setUnitType(unitType);
    summaryBottomSheet.setUnitType(unitType);
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
  }

  private void shutdown() {
    mapView.onDestroy();
    navigationViewModel.onDestroy(isChangingConfigurations());
    ImageCoordinator.getInstance().shutdown();
    navigationMap = null;
  }
}