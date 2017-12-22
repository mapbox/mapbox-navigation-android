package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
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
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.location.LocationViewModel;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.RouteViewModel;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.utils.ViewUtils;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

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
  private RouteViewModel routeViewModel;
  private LocationViewModel locationViewModel;
  private MapboxMap map;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationLayerPlugin locationLayer;
  private OnNavigationReadyCallback onNavigationReadyCallback;
  private boolean resumeState;

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
    resumeState = savedInstanceState != null;
    mapView.onCreate(savedInstanceState);
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
    map.setPadding(0, 0, 0, summaryBottomSheet.getHeight());
    ThemeSwitcher.setMapStyle(getContext(), map, new MapboxMap.OnStyleLoadedListener() {
      @Override
      public void onStyleLoaded(String style) {
        initRoute();
        initLocationLayer();
        initLifecycleObservers();
        initNavigationPresenter();
        initClickListeners();
        map.setOnScrollListener(NavigationView.this);
        onNavigationReadyCallback.onNavigationReady();
      }
    });
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
    camera.setCameraTrackingLocation(isEnabled);
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
    map.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(ThemeSwitcher.retrieveMapMarker(getContext())));
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
    // Initialize navigation with options from NavigationViewOptions
    navigationViewModel.initializeNavigationOptions(getContext().getApplicationContext(),
      options.navigationOptions().toBuilder().isFromNavigationUi(true).build());
    // Initialize the camera (listens to MapboxNavigation)
    initCamera();

    setupListeners(options);

    locationViewModel.updateShouldSimulateRoute(options.shouldSimulateRoute());
    routeViewModel.extractRouteOptions(options);
    // Everything is setup, subscribe to the view models
    subscribeViewModels();
  }

  /**
   * Should be called after {@link NavigationView#onCreate(Bundle)}.
   * <p>
   * This method adds the {@link OnNavigationReadyCallback},
   * which will fire ready / cancel events for this view.
   *
   * @param onNavigationReadyCallback to be set to this view
   */
  public void getNavigationAsync(OnNavigationReadyCallback onNavigationReadyCallback) {
    this.onNavigationReadyCallback = onNavigationReadyCallback;
    mapView.getMapAsync(this);
  }

  private void init() {
    inflate(getContext(), R.layout.navigation_view_layout, this);
    bind();
    initViewModels();
    initNavigationViewObserver();
    initSummaryBottomSheet();
    initNavigationEventDispatcher();
  }

  /**
   * Binds all necessary views.
   */
  private void bind() {
    mapView = findViewById(R.id.mapView);
    instructionView = findViewById(R.id.instructionView);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    cancelBtn = findViewById(R.id.cancelBtn);
    recenterBtn = findViewById(R.id.recenterBtn);
  }

  private void initViewModels() {
    try {
      locationViewModel = ViewModelProviders.of((FragmentActivity) getContext()).get(LocationViewModel.class);
      routeViewModel = ViewModelProviders.of((FragmentActivity) getContext()).get(RouteViewModel.class);
      navigationViewModel = ViewModelProviders.of((FragmentActivity) getContext()).get(NavigationViewModel.class);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
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

  /**
   * Initializes the {@link NavigationMapRoute} to be used to draw the
   * route.
   */
  private void initRoute() {
    int routeStyleRes = ThemeSwitcher.retrieveNavigationViewRouteStyle(getContext());
    mapRoute = new NavigationMapRoute(null, mapView, map, routeStyleRes);
  }

  /**
   * Initializes the {@link NavigationCamera} that will be used to follow
   * the {@link Location} updates from {@link MapboxNavigation}.
   */
  private void initCamera() {
    camera = new NavigationCamera(this, map, navigationViewModel.getNavigation());
  }

  /**
   * Subscribes the {@link InstructionView} and {@link SummaryBottomSheet} to the {@link NavigationViewModel}.
   * <p>
   * Then, creates an instance of {@link NavigationViewSubscriber}, which takes a presenter and listener.
   * <p>
   * The subscriber then subscribes to the view models, setting up the appropriate presenter / listener
   * method calls based on the {@link android.arch.lifecycle.LiveData} updates.
   */
  private void subscribeViewModels() {
    instructionView.subscribe(navigationViewModel);
    summaryBottomSheet.subscribe(navigationViewModel);

    NavigationViewSubscriber subscriber = new NavigationViewSubscriber(navigationPresenter,
      navigationViewEventDispatcher);
    subscriber.subscribe(((LifecycleOwner) getContext()), locationViewModel, routeViewModel, navigationViewModel);
  }

  /**
   * Initializes the {@link LocationLayerPlugin} to be used to draw the current
   * location.
   */
  @SuppressWarnings( {"MissingPermission"})
  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, map, null);
    locationLayer.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);
  }

  /**
   * Adds this view as a lifecycle observer.
   * This needs to be done earlier than the other observers (prior to the style loading).
   */
  private void initNavigationViewObserver() {
    try {
      ((LifecycleOwner) getContext()).getLifecycle().addObserver(this);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid LifecycleOwner");
    }
  }

  /**
   * Add lifecycle observers to ensure these objects properly
   * start / stop based on the Android lifecycle.
   */
  private void initLifecycleObservers() {
    try {
      ((LifecycleOwner) getContext()).getLifecycle().addObserver(locationLayer);
      ((LifecycleOwner) getContext()).getLifecycle().addObserver(locationViewModel);
      ((LifecycleOwner) getContext()).getLifecycle().addObserver(navigationViewModel);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid LifecycleOwner");
    }
  }

  /**
   * Initialize a new event dispatcher in charge of firing all navigation
   * listener updates to the classes that have implemented these listeners.
   */
  private void initNavigationEventDispatcher() {
    navigationViewEventDispatcher = new NavigationViewEventDispatcher();
  }

  /**
   * Sets up the listeners in the dispatcher, as well as the listeners in the {@link MapboxNavigation}
   * @param navigationViewOptions that contains all listeners to attach
   */
  private void setupListeners(NavigationViewOptions navigationViewOptions) {
    navigationViewEventDispatcher.setFeedbackListener(navigationViewOptions.feedbackListener());
    navigationViewEventDispatcher.setNavigationListener(navigationViewOptions.navigationListener());
    navigationViewEventDispatcher.setRouteListener(navigationViewOptions.routeListener());

    if (navigationViewOptions.progressChangeListener() != null) {
      navigationViewModel.getNavigation().addProgressChangeListener(navigationViewOptions.progressChangeListener());
    }

    if (navigationViewOptions.milestoneEventListener() != null) {
      navigationViewModel.getNavigation().addMilestoneEventListener(navigationViewOptions.milestoneEventListener());
    }
  }

  /**
   * Initialize a new presenter for this Activity.
   */
  private void initNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  /**
   * Sets click listeners to all views that need them.
   */
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

  /**
   * Initializes the {@link BottomSheetBehavior} for {@link SummaryBottomSheet}.
   */
  private void initSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setHideable(false);
    summaryBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if (newState == BottomSheetBehavior.STATE_HIDDEN && navigationPresenter != null) {
          navigationPresenter.onSummaryBottomSheetHidden();
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      }
    });
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    mapView.onStart();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  public void onResume() {
    mapView.onResume();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  public void onPause() {
    mapView.onPause();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    mapView.onStop();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    mapView.onDestroy();
  }
}
