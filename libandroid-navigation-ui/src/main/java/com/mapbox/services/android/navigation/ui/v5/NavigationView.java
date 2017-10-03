package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.RouteViewModel;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

/**
 * Activity that creates the drop-in UI.
 * <p>
 * Once started, this activity will check if launched with a {@link DirectionsRoute}.
 * Or, if not found, this activity will look for a set of {@link Position} coordinates.
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
public class NavigationView extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnScrollListener,
  NavigationContract.View {

  private MapView mapView;
  private InstructionView instructionView;
  private SummaryBottomSheet summaryBottomSheet;
  private BottomSheetBehavior summaryBehavior;
  private ImageButton cancelBtn;
  private ImageButton expandArrow;
  private View summaryDirections;
  private View summaryOptions;
  private View directionsOptionLayout;
  private View sheetShadow;
  private RecenterButton recenterBtn;
  private FloatingActionButton soundFab;

  private NavigationPresenter navigationPresenter;
  private NavigationViewModel navigationViewModel;
  private RouteViewModel routeViewModel;
  private LocationViewModel locationViewModel;
  private MapboxMap map;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationLayerPlugin locationLayer;
  private boolean resumeState;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    ThemeSwitcher.setTheme(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.navigation_view_layout);
    resumeState = savedInstanceState != null;
    bind();
    initViewModels();
    initClickListeners();
    initSummaryBottomSheet();
    initMap(savedInstanceState);
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
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
    initRoute();
    initCamera();
    initLocationLayer();
    initLifecycleObservers();
    initNavigationPresenter();
    subscribeViews();
    routeViewModel.extractLaunchData(this);
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
  public void setSummaryOptionsVisibility(boolean isVisible) {
    summaryOptions.setVisibility(isVisible ? View.VISIBLE : View.GONE);
  }

  @Override
  public void setSummaryDirectionsVisibility(boolean isVisible) {
    summaryDirections.setVisibility(isVisible ? View.VISIBLE : View.GONE);
  }

  @Override
  public boolean isSummaryDirectionsVisible() {
    return summaryDirections.getVisibility() == View.VISIBLE;
  }

  @Override
  public void setSheetShadowVisibility(boolean isVisible) {
    sheetShadow.setVisibility(isVisible ? View.VISIBLE : View.GONE);
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
    recenterBtn.show();
  }

  @Override
  public void hideRecenterBtn() {
    recenterBtn.hide();
  }

  @Override
  public void showInstructionView() {
    instructionView.show();
  }

  @Override
  public void drawRoute(DirectionsRoute directionsRoute) {
    mapRoute.addRoute(directionsRoute);
  }

  @Override
  public void setMuted(boolean isMuted) {
    navigationViewModel.setMuted(isMuted);
  }

  @Override
  public void setCancelBtnClickable(boolean isClickable) {
    cancelBtn.setClickable(isClickable);
  }

  @Override
  public void animateCancelBtnAlpha(float value) {
    cancelBtn.animate().alpha(value).setDuration(0).start();
  }

  @Override
  public void animateExpandArrowRotation(float value) {
    expandArrow.animate().rotation(value).setDuration(0).start();
  }

  @Override
  public void animateInstructionViewAlpha(float value) {
    instructionView.animate().alpha(value).setDuration(0).start();
  }

  /**
   * Creates a marker based on the
   * {@link Position} destination coordinate.
   *
   * @param position where the marker should be placed
   */
  @Override
  public void addMarker(Position position) {
    LatLng markerPosition = new LatLng(position.getLatitude(),
      position.getLongitude());
    map.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(ThemeSwitcher.retrieveMapMarker(this)));
  }

  @Override
  public void finishNavigationView() {
    finish();
  }

  public void startCamera(DirectionsRoute directionsRoute) {
    if (!resumeState) {
      camera.start(directionsRoute);
    }
  }

  public void resumeCamera(Location location) {
    if (resumeState) {
      camera.resume(location);
      resumeState = false;
    }
  }

  /**
   * Binds all necessary views.
   */
  private void bind() {
    mapView = findViewById(R.id.mapView);
    instructionView = findViewById(R.id.instructionView);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    cancelBtn = findViewById(R.id.cancelBtn);
    expandArrow = findViewById(R.id.expandArrow);
    summaryOptions = findViewById(R.id.summaryOptions);
    summaryDirections = findViewById(R.id.summaryDirections);
    directionsOptionLayout = findViewById(R.id.directionsOptionLayout);
    sheetShadow = findViewById(R.id.sheetShadow);
    recenterBtn = findViewById(R.id.recenterBtn);
    soundFab = findViewById(R.id.soundFab);
  }

  private void initViewModels() {
    locationViewModel = ViewModelProviders.of(this).get(LocationViewModel.class);
    routeViewModel = ViewModelProviders.of(this).get(RouteViewModel.class);
    navigationViewModel = ViewModelProviders.of(this).get(NavigationViewModel.class);
  }

  /**
   * Sets click listeners to all views that need them.
   */
  private void initClickListeners() {
    directionsOptionLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onDirectionsOptionClick();
      }
    });
    cancelBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onCancelBtnClick();
      }
    });
    expandArrow.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        navigationPresenter.onExpandArrowClick(summaryBehavior.getState());
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
        ThemeSwitcher.toggleTheme(NavigationView.this);
        navigationPresenter.onMuteClick(instructionView.toggleMute());
      }
    });
  }

  /**
   * Sets up the {@link MapboxMap}.
   *
   * @param savedInstanceState from onCreate()
   */
  private void initMap(Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
    ThemeSwitcher.setMapStyle(this, mapView);
  }

  /**
   * Initializes the {@link BottomSheetBehavior} for {@link SummaryBottomSheet}.
   */
  private void initSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        switch (newState) {
          case BottomSheetBehavior.STATE_EXPANDED:
            navigationPresenter.onSummaryBottomSheetExpanded();
            break;
          case BottomSheetBehavior.STATE_COLLAPSED:
            navigationPresenter.onSummaryBottomSheetCollapsed();
            break;
          case BottomSheetBehavior.STATE_HIDDEN:
            navigationPresenter.onSummaryBottomSheetHidden();
            break;
          default:
            break;
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        navigationPresenter.onBottomSheetSlide(slideOffset,
          sheetShadow.getVisibility() != View.VISIBLE);
      }
    });
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

  private void initLifecycleObservers() {
    getLifecycle().addObserver(locationLayer);
    getLifecycle().addObserver(locationViewModel);
    getLifecycle().addObserver(navigationViewModel);
  }

  private void initNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  private void subscribeViews() {
    instructionView.subscribe(navigationViewModel);
    summaryBottomSheet.subscribe(navigationViewModel);

    locationViewModel.rawLocation.observe(this, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          routeViewModel.updateRawLocation(location);
        }
      }
    });

    locationViewModel.locationEngine.observe(this, new Observer<LocationEngine>() {
      @Override
      public void onChanged(@Nullable LocationEngine locationEngine) {
        if (locationEngine != null) {
          navigationViewModel.updateLocationEngine(locationEngine);
        }
      }
    });

    routeViewModel.route.observe(this, new Observer<DirectionsRoute>() {
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

    routeViewModel.destination.observe(this, new Observer<Position>() {
      @Override
      public void onChanged(@Nullable Position position) {
        if (position != null) {
          navigationPresenter.onDestinationUpdate(position);
        }
      }
    });

    navigationViewModel.isRunning.observe(this, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean isRunning) {
        if (isRunning != null) {
          if (isRunning && !resumeState) {
            navigationPresenter.onNavigationRunning();
          }
        }
      }
    });

    navigationViewModel.navigationLocation.observe(this, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null && location.getLongitude() != 0 && location.getLatitude() != 0) {
          locationLayer.forceLocationUpdate(location);
          resumeCamera(location);
        }
      }
    });

    navigationViewModel.newOrigin.observe(this, new Observer<Position>() {
      @Override
      public void onChanged(@Nullable Position newOrigin) {
        if (newOrigin != null) {
          routeViewModel.fetchRouteNewOrigin(newOrigin);
          // To prevent from firing on rotation
          navigationViewModel.newOrigin.setValue(null);
        }
      }
    });
  }
}
