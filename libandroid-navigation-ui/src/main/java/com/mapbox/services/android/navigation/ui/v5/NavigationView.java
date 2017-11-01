package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.core.location.LocationEngine;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackBottomSheetListener;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.RouteViewModel;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

/**
 * View that creates the drop-in UI.
 * <p>
 * Once started, this view will check if launched with a {@link DirectionsRoute}.
 * Or, if not found, this activity will look for a set of {@link Point} coordinates.
 * In the latter case, a new {@link DirectionsRoute} will be retrieved from {@link NavigationRoute}.
 * </p><p>
 * Once valid data is obtained, this activity will immediately begin navigation
 * with {@link MapboxNavigation}.
 * If launched with the simulation boolean set to true, a {@link MockLocationEngine}
 * will be initialized and begin pushing updates.
 * <p>
 * This activity requires user permissions ACCESS_FINE_LOCATION
 * and ACCESS_COARSE_LOCATION have already been granted.
 * <p>
 * A Mapbox access token must also be set by the developer (to initialize navigation).
 *
 * @since 0.6.0
 * </p>
 */
public class NavigationView extends CoordinatorLayout implements OnMapReadyCallback, MapboxMap.OnScrollListener,
  NavigationContract.View, FeedbackBottomSheetListener {

  private MapView mapView;
  private InstructionView instructionView;
  private SummaryBottomSheet summaryBottomSheet;
  private BottomSheetBehavior summaryBehavior;
  private ImageButton cancelBtn;
  private RecenterButton recenterBtn;
  private FloatingActionButton soundFab;
  private FloatingActionButton feedbackFab;

  private NavigationPresenter navigationPresenter;
  private NavigationViewModel navigationViewModel;
  private RouteViewModel routeViewModel;
  private LocationViewModel locationViewModel;
  private MapboxMap map;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationLayerPlugin locationLayer;
  private NavigationViewListener navigationListener;
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
    ThemeSwitcher.setTheme(getContext());
    init();
  }

  public void onCreate(@Nullable Bundle savedInstanceState) {
    resumeState = savedInstanceState != null;
    mapView.onCreate(savedInstanceState);
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

  public void onLowMemory() {
    mapView.onLowMemory();
  }

  public void onStop() {
    mapView.onStop();
  }

  public void onDestroy() {
    mapView.onDestroy();
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

  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(getContext().getString(R.string.bottom_sheet_state),
      summaryBehavior.getState());
    outState.putBoolean(getContext().getString(R.string.recenter_btn_visible),
      recenterBtn.getVisibility() == View.VISIBLE);
    mapView.onSaveInstanceState(outState);
  }

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
    map.setOnScrollListener(this);
    map.setPadding(0, 0, 0, summaryBottomSheet.getHeight());
    ThemeSwitcher.setMapStyle(getContext(), map, new MapboxMap.OnStyleLoadedListener() {
      @Override
      public void onStyleLoaded(String style) {
        initRoute();
        initCamera();
        initLocationLayer();
        initLifecycleObservers();
        initNavigationPresenter();
        subscribeViews();
        navigationListener.onNavigationReady();
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
    if (!summaryBehavior.isHideable()) {
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

  @Override
  public void finishNavigationView() {
    navigationListener.onNavigationFinished();
  }

  @Override
  public void setMuted(boolean isMuted) {
    navigationViewModel.setMuted(isMuted);
  }

  @Override
  public void showFeedbackBottomSheet() {
    FeedbackBottomSheet.newInstance(this, 10000).show(
      ((FragmentActivity) getContext()).getSupportFragmentManager(), FeedbackBottomSheet.TAG);
  }

  @Override
  public void onFeedbackSelected(FeedbackItem feedbackItem) {
    navigationViewModel.updateFeedback(feedbackItem);
  }

  @Override
  public void onFeedbackDismissed() {
    navigationViewModel.cancelFeedback();
  }

  public void startNavigation(Activity activity) {
    routeViewModel.extractLaunchData(activity);
  }

  public void getNavigationAsync(NavigationViewListener navigationViewListener) {
    this.navigationListener = navigationViewListener;
    mapView.getMapAsync(this);
  }

  /**
   * Used when starting this {@link android.app.Activity}
   * for the first time.
   * <p>
   * Zooms to the beginning of the {@link DirectionsRoute}.
   *
   * @param directionsRoute where camera should move to
   */
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
  public void resumeCamera(Location location) {
    if (resumeState && recenterBtn.getVisibility() != View.VISIBLE) {
      camera.resume(location);
      resumeState = false;
    }
  }

  private void init() {
    inflate(getContext(), R.layout.navigation_view_layout, this);
    bind();
    initViewModels();
    initClickListeners();
    initSummaryBottomSheet();
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
    soundFab = findViewById(R.id.soundFab);
    feedbackFab = findViewById(R.id.feedbackFab);
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
   * Sets click listeners to all views that need them.
   */
  private void initClickListeners() {
    cancelBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onCancelBtnClick();
      }
    });
    recenterBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onRecenterClick();
      }
    });
    soundFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onMuteClick(instructionView.toggleMute());
      }
    });
    feedbackFab.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onFeedbackClick();
        navigationViewModel.recordFeedback();
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
        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
          navigationPresenter.onSummaryBottomSheetHidden();
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      }
    });
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
    mapRoute = new NavigationMapRoute(mapView, map, NavigationConstants.ROUTE_BELOW_LAYER);
  }

  /**
   * Initializes the {@link NavigationCamera} that will be used to follow
   * the {@link Location} updates from {@link MapboxNavigation}.
   */
  private void initCamera() {
    camera = new NavigationCamera(this, map, navigationViewModel.getNavigation());
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
   * Initialize a new presenter for this Activity.
   */
  private void initNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  /**
   * Initiate observing of ViewModels by Views.
   */
  private void subscribeViews() {
    instructionView.subscribe(navigationViewModel);
    summaryBottomSheet.subscribe(navigationViewModel);

    locationViewModel.rawLocation.observe((LifecycleOwner) getContext(), new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          routeViewModel.updateRawLocation(location);
        }
      }
    });

    locationViewModel.locationEngine.observe((LifecycleOwner) getContext(), new Observer<LocationEngine>() {
      @Override
      public void onChanged(@Nullable LocationEngine locationEngine) {
        if (locationEngine != null) {
          navigationViewModel.updateLocationEngine(locationEngine);
        }
      }
    });

    routeViewModel.route.observe((LifecycleOwner) getContext(), new Observer<DirectionsRoute>() {
      @Override
      public void onChanged(@Nullable DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationViewModel.updateRoute(directionsRoute);
          locationViewModel.updateRoute(directionsRoute);
          navigationPresenter.onRouteUpdate(directionsRoute);
          startCamera(directionsRoute);
        }
      }
    });

    routeViewModel.destination.observe((LifecycleOwner) getContext(), new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point point) {
        if (point != null) {
          navigationPresenter.onDestinationUpdate(point);
        }
      }
    });

    navigationViewModel.isRunning.observe((LifecycleOwner) getContext(), new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean isRunning) {
        if (isRunning != null) {
          if (!isRunning) {
            navigationListener.onNavigationFinished();
          }
        }
      }
    });

    navigationViewModel.navigationLocation.observe((LifecycleOwner) getContext(), new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null && location.getLongitude() != 0 && location.getLatitude() != 0) {
          locationLayer.forceLocationUpdate(location);
          resumeCamera(location);
        }
      }
    });

    navigationViewModel.newOrigin.observe((LifecycleOwner) getContext(), new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point newOrigin) {
        if (newOrigin != null) {
          routeViewModel.fetchRouteNewOrigin(newOrigin);
          // To prevent from firing on rotation
          navigationViewModel.newOrigin.setValue(null);
        }
      }
    });
  }
}
