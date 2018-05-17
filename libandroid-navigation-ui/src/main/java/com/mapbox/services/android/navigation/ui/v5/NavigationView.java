package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.app.Fragment;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionLoader;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.utils.ViewUtils;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.List;
import java.util.Locale;

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
public class NavigationView extends CoordinatorLayout implements LifecycleObserver,
  OnMapReadyCallback, MapboxMap.OnScrollListener, NavigationContract.View {

  private MapView mapView;
  private InstructionView instructionView;
  private SummaryBottomSheet summaryBottomSheet;
  private BottomSheetBehavior summaryBehavior;
  private ImageButton cancelBtn;
  private RecenterButton recenterBtn;

  private NavigationPresenter navigationPresenter;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private NavigationViewModel navigationViewModel;
  private MapboxMap map;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationLayerPlugin locationLayer;
  private OnNavigationReadyCallback onNavigationReadyCallback;
  private boolean resumeState;
  private boolean isInitialized;

  public NavigationView(Context context) {
    this(context, null);
  }

  public NavigationView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public NavigationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    ThemeSwitcher.setTheme(context, attrs);
    init();
  }

  /**
   * Uses savedInstanceState as a cue to restore state (if not null).
   *
   * @param savedInstanceState to restore state if not null
   */
  public void onCreate(@Nullable Bundle savedInstanceState) {
    mapView.setStyleUrl(ThemeSwitcher.retrieveMapStyle(getContext()));
    mapView.onCreate(savedInstanceState);
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
    if (instructionView.isShowingInstructionList()) {
      instructionView.hideInstructionList();
      return true;
    }
    return false;
  }

  /**
   * Used to store the bottomsheet state and re-center
   * button visibility.  As well as anything the {@link MapView}
   * needs to store in the bundle.
   *
   * @param outState to store state variables
   */
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(getContext().getString(R.string.bottom_sheet_state),
      summaryBehavior.getState());
    outState.putBoolean(getContext().getString(R.string.recenter_btn_visible),
      recenterBtn.getVisibility() == View.VISIBLE);
    outState.putBoolean(getContext().getString(R.string.navigation_running), navigationViewModel.isRunning());
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
    resumeState = savedInstanceState.getBoolean(getContext().getString(R.string.navigation_running));
    resetBottomSheetState(bottomSheetState);
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
    mapView.onDestroy();
    navigationViewModel.onDestroy(isChangingConfigurations());
    InstructionLoader.getInstance().shutdown();
    if (camera != null) {
      camera.onDestroy();
    }
  }

  public void onStart() {
    mapView.onStart();
  }

  public void onResume() {
    mapView.onResume();
  }

  public void onPause() {
    mapView.onPause();
  }

  public void onStop() {
    mapView.onStop();
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
    map = mapboxMap;
    initRoute();
    initLocationLayer();
    initMapPadding();
    initLocationLayerObserver();
    initNavigationPresenter();
    initClickListeners();
    map.addOnScrollListener(NavigationView.this);
    onNavigationReadyCallback.onNavigationReady();
  }

  /**
   * Listener this activity sets on the {@link MapboxMap}.
   * <p>
   * Used as a cue to hide the {@link SummaryBottomSheet} and stop the
   * camera from following location updates.
   *
   * @since 0.6.0
   */
  @Override
  public void onScroll() {
    if (summaryBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
      navigationPresenter.onMapScroll();
    }
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
  public void setCameraTrackingEnabled(boolean isEnabled) {
    if (camera != null) {
      camera.setCameraTrackingLocation(isEnabled);
    }
  }

  @Override
  public void resetCameraPosition() {
    camera.resetCameraPosition();
  }

  @Override
  public void showRecenterBtn() {
    if (summaryBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
      recenterBtn.show();
    }
  }

  @Override
  public void hideRecenterBtn() {
    recenterBtn.hide();
  }

  @Override
  public void drawRoute(DirectionsRoute directionsRoute) {
    mapRoute.addRoute(directionsRoute);
  }

  /**
   * Creates a marker based on the
   * {@link Point} destination coordinate.
   *
   * @param position where the marker should be placed
   */
  @Override
  public void addMarker(Point position) {
    LatLng markerPosition = new LatLng(position.latitude(),
      position.longitude());
    Icon marker = ThemeSwitcher.retrieveMapMarker(getContext());
    map.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(marker));
  }

  /**
   * Called when the navigation session is finished.
   * Can either be from a cancel event or if the user has arrived at their destination.
   */
  @Override
  public void finishNavigationView() {
    navigationViewEventDispatcher.onNavigationFinished();
  }

  @Override
  public void takeScreenshot() {
    map.snapshot(new MapboxMap.SnapshotReadyCallback() {
      @Override
      public void onSnapshotReady(Bitmap snapshot) {
        // Make the image visible
        ImageView screenshotView = findViewById(R.id.screenshotView);
        screenshotView.setVisibility(View.VISIBLE);
        screenshotView.setImageBitmap(snapshot);

        // Take a screenshot without the map
        mapView.setVisibility(View.INVISIBLE);
        Bitmap capture = ViewUtils.captureView(mapView);
        String encoded = ViewUtils.encodeView(capture);
        navigationViewModel.updateFeedbackScreenshot(encoded);

        // Restore visibility
        screenshotView.setVisibility(View.INVISIBLE);
        mapView.setVisibility(View.VISIBLE);
      }
    });
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
    if (!resumeState) {
      camera.start(directionsRoute);
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
    if (resumeState && recenterBtn.getVisibility() != View.VISIBLE) {
      camera.resume(location);
      resumeState = false;
    }
  }

  @Override
  public void updateLocationLayer(Location location) {
    locationLayer.forceLocationUpdate(location);
  }

  /**
   * Should be called when this view is completely initialized.
   *
   * @param options with containing route / coordinate data
   */
  public void startNavigation(NavigationViewOptions options) {
    if (!isInitialized) {
      establish(options);
      navigationViewModel.initializeNavigation(options, navigationViewEventDispatcher);
      initNavigationListeners(options, navigationViewModel.getNavigation());
      initNavigationCamera();
      subscribeViewModels();
      isInitialized = true;
    } else {
      clearMarkers();
      navigationViewModel.updateNavigation(options);
    }
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
  public MapboxMap getMapboxMap() {
    return map;
  }

  private void init() {
    InstructionLoader.getInstance().initialize(getContext());
    inflate(getContext(), R.layout.navigation_view_layout, this);
    bind();
    initViewModels();
    initSummaryBottomSheet();
    initNavigationEventDispatcher();
  }

  private void bind() {
    mapView = findViewById(R.id.mapView);
    instructionView = findViewById(R.id.instructionView);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    cancelBtn = findViewById(R.id.cancelBtn);
    recenterBtn = findViewById(R.id.recenterBtn);
  }

  private void initViewModels() {
    try {
      navigationViewModel = ViewModelProviders.of((FragmentActivity) getContext()).get(NavigationViewModel.class);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  private void initSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setHideable(false);
    summaryBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        navigationViewEventDispatcher.onBottomSheetStateChanged(bottomSheet, newState);

        if (newState == BottomSheetBehavior.STATE_HIDDEN && navigationPresenter != null) {
          navigationPresenter.onSummaryBottomSheetHidden();
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      }
    });
  }

  private void initNavigationEventDispatcher() {
    navigationViewEventDispatcher = new NavigationViewEventDispatcher();
  }

  /**
   * Sets the {@link BottomSheetBehavior} based on the last state stored
   * in {@link Bundle} savedInstanceState.
   *
   * @param bottomSheetState retrieved from savedInstanceState
   */
  private void resetBottomSheetState(int bottomSheetState) {
    boolean isShowing = bottomSheetState == BottomSheetBehavior.STATE_EXPANDED;
    summaryBehavior.setHideable(!isShowing);
    summaryBehavior.setState(bottomSheetState);
  }

  private boolean isChangingConfigurations() {
    try {
      return ((FragmentActivity) getContext()).isChangingConfigurations();
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  /**
   * Create a top map padding value that pushes the focal point
   * of the map to the bottom of the screen (above the bottom sheet).
   */
  private void initMapPadding() {
    int mapViewHeight = mapView.getHeight();
    int bottomSheetHeight = summaryBottomSheet.getHeight();
    int topPadding = mapViewHeight - (bottomSheetHeight * 4);
    map.setPadding(0, topPadding, 0, 0);
  }

  /**
   * Initializes the {@link NavigationMapRoute} to be used to draw the
   * route.
   */
  private void initRoute() {
    int routeStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(getContext(), R.attr.navigationViewRouteStyle);
    mapRoute = new NavigationMapRoute(null, mapView, map, routeStyleRes);
  }

  /**
   * Initializes the {@link LocationLayerPlugin} to be used to draw the current
   * location.
   */
  @SuppressWarnings( {"MissingPermission"})
  private void initLocationLayer() {
    int locationLayerStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(getContext(),
      R.attr.navigationViewLocationLayerStyle);
    locationLayer = new LocationLayerPlugin(mapView, map, null, locationLayerStyleRes);
    locationLayer.setRenderMode(RenderMode.GPS);
  }

  private void initLocationLayerObserver() {
    try {
      ((LifecycleOwner) getContext()).getLifecycle().addObserver(locationLayer);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid LifecycleOwner");
    }
  }

  private void initNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  private void initClickListeners() {
    cancelBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onCancelBtnClick();
        navigationViewEventDispatcher.onCancelNavigation();
      }
    });
    recenterBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onRecenterClick();
      }
    });
  }

  private void clearMarkers() {
    List<Marker> mapMarkers = map.getMarkers();
    for (Marker marker : mapMarkers) {
      map.removeMarker(marker);
    }
  }

  private void establish(NavigationViewOptions options) {
    establishLocale(options);
    establishUnitType(options);
    establishTimeFormat(options);
  }

  private void establishLocale(NavigationViewOptions options) {
    Locale locale = LocaleUtils.getNonNullLocale(getContext(), options.navigationOptions().locale());
    instructionView.setLocale(locale);
    summaryBottomSheet.setLocale(locale);
  }

  private void establishUnitType(NavigationViewOptions options) {
    @NavigationUnitType.UnitType
    int unitType = options.navigationOptions().unitType();
    instructionView.setUnitType(unitType);
    summaryBottomSheet.setUnitType(unitType);
  }

  private void establishTimeFormat(NavigationViewOptions options) {
    @NavigationTimeFormat.Type
    int timeFormatType = options.navigationOptions().timeFormatType();
    summaryBottomSheet.setTimeFormat(timeFormatType);
  }

  private void initNavigationListeners(NavigationViewOptions options, MapboxNavigation navigation) {
    navigationViewEventDispatcher.initializeListeners(options, navigation);
  }

  private void initNavigationCamera() {
    camera = new NavigationCamera(map, navigationViewModel.getNavigation());
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
}
