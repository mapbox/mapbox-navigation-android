package com.mapbox.services.android.navigation.ui.v5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
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
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationInstructionPlayer;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.commons.models.Position;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
  LocationEngineListener, ProgressChangeListener, OffRouteListener,
  MilestoneEventListener, Callback<DirectionsResponse> {

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

  private MapboxMap map;
  private MapboxNavigation navigation;
  private InstructionPlayer instructionPlayer;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationEngine locationEngine;
  private LocationLayerPlugin locationLayer;
  private SharedPreferences preferences;

  private Location location;
  private Position destination;
  private boolean checkLaunchData;
  private boolean navigationRunning;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.navigation_view_layout);
    checkLaunchData = savedInstanceState == null;
    bind();
    initClickListeners();
    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    initMap(savedInstanceState);
    initSummaryBottomSheet();
    initNavigation();
    initVoiceInstructions();
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
    if (locationLayer != null) {
      locationLayer.onStart();
    }
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
    if (locationLayer != null) {
      locationLayer.onStop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    shutdownNavigation();
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
    initLocation();
    checkLaunchData(getIntent());
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
      summaryBehavior.setHideable(true);
      summaryBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
      camera.setCameraTrackingLocation(false);
    }
  }

  /**
   * Called after the {@link LocationEngine} is activated.
   * Good to request location updates at this point.
   *
   * @since 0.6.0
   */
  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  /**
   * Fired when the {@link LocationEngine} updates.
   * <p>
   * This activity will check launch data here (if we didn't have a location when the map was ready).
   * Once the first location update is received, a new route can be retrieved from {@link NavigationRoute}.
   *
   * @param location used to retrieve route with bearing
   */
  @Override
  public void onLocationChanged(Location location) {
    this.location = location;
    checkLaunchData(getIntent());
  }

  /**
   * Listener used to update the {@link LocationLayerPlugin}.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location      given to the {@link LocationLayerPlugin} to show our current location
   * @param routeProgress ignored in this scenario
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (location.getLongitude() != 0 && location.getLatitude() != 0) {
      locationLayer.forceLocationUpdate(location);
    }
  }

  /**
   * Listener used to update the {@link LocationLayerPlugin}.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location given to the {@link LocationLayerPlugin} to show our current location
   * @since 0.6.0
   */
  @Override
  public void userOffRoute(Location location) {
    Position newOrigin = Position.fromLngLat(location.getLongitude(), location.getLatitude());
    fetchRoute(newOrigin, destination);
  }

  /**
   * Listener used to play instructions and finish this activity
   * when the arrival milestone is triggered.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param routeProgress ignored in this scenario
   * @param instruction   to be voiced by the {@link InstructionPlayer}
   * @param identifier    used to determine the type of milestone
   * @since 0.6.0
   */
  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    instructionPlayer.play(instruction);
  }

  /**
   * A new directions response has been received.
   * <p>
   * The {@link DirectionsResponse} is validated.
   * If navigation is running, {@link MapboxNavigation} is updated and reroute state is dismissed.
   * If not, navigation is started.
   *
   * @param call     used to request the new {@link DirectionsRoute}
   * @param response contains the new {@link DirectionsRoute}
   * @since 0.6.0
   */
  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      if (navigationRunning) {
        updateNavigation(response.body().getRoutes().get(0));
        instructionView.hideRerouteState();
        summaryBottomSheet.hideRerouteState();
      } else {
        startNavigation(response.body().getRoutes().get(0));
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable t) {
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

  /**
   * Sets click listeners to all views that need them.
   */
  private void initClickListeners() {
    directionsOptionLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        sheetShadow.setVisibility(View.GONE);
        summaryOptions.setVisibility(View.GONE);
        summaryDirections.setVisibility(View.VISIBLE);
      }
    });
    cancelBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        finish();
      }
    });
    expandArrow.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        summaryBehavior.setState(summaryBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED
          ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
      }
    });
    recenterBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        summaryBehavior.setHideable(false);
        summaryBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        camera.resetCameraPosition();
        recenterBtn.hide();
      }
    });
    soundFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        instructionPlayer.setMuted(instructionView.toggleMute());
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
  }

  /**
   * Initializes the {@link BottomSheetBehavior} for {@link SummaryBottomSheet}.
   */
  private void initSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setHideable(false);
    summaryBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    summaryBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        switch (newState) {
          case BottomSheetBehavior.STATE_EXPANDED:
            cancelBtn.setClickable(false);
            if (summaryDirections.getVisibility() == View.VISIBLE) {
              sheetShadow.setVisibility(View.GONE);
            }
            break;
          case BottomSheetBehavior.STATE_COLLAPSED:
            cancelBtn.setClickable(true);
            summaryOptions.setVisibility(View.VISIBLE);
            summaryDirections.setVisibility(View.GONE);
            break;
          case BottomSheetBehavior.STATE_HIDDEN:
            if (!camera.isTrackingEnabled()) {
              recenterBtn.show();
            }
            break;
          default:
            break;
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        if (slideOffset < 1f && sheetShadow.getVisibility() != View.VISIBLE) {
          sheetShadow.setVisibility(View.VISIBLE);
        }
        if (summaryDirections.getVisibility() == View.VISIBLE) {
          instructionView.animate().alpha(1 - slideOffset).setDuration(0).start();
        }
        cancelBtn.animate().alpha(1 - slideOffset).setDuration(0).start();
        expandArrow.animate().rotation(180 * slideOffset).setDuration(0).start();
      }
    });
  }

  /**
   * Initializes {@link MapboxNavigation} and adds all views that implement listeners.
   */
  private void initNavigation() {
    navigation = new MapboxNavigation(this, Mapbox.getAccessToken(),
      MapboxNavigationOptions.builder().isFromNavigationUi(true).build());
    navigation.addProgressChangeListener(this);
    navigation.addProgressChangeListener(instructionView);
    navigation.addProgressChangeListener(summaryBottomSheet);
    navigation.addMilestoneEventListener(this);
    navigation.addOffRouteListener(this);
    navigation.addOffRouteListener(summaryBottomSheet);
    navigation.addOffRouteListener(instructionView);
  }

  /**
   * Initializes the {@link InstructionPlayer}.
   */
  private void initVoiceInstructions() {
    instructionPlayer = new NavigationInstructionPlayer(this,
      preferences.getString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, null));
  }

  /**
   * Initializes the {@link LocationEngine} based on whether or not
   * simulation is enabled.
   */
  @SuppressWarnings( {"MissingPermission"})
  private void initLocation() {
    if (!shouldSimulateRoute()) {
      locationEngine = navigation.getLocationEngine();
      locationEngine.addLocationEngineListener(this);
      locationEngine.activate();

      if (locationEngine.getLastLocation() != null) {
        onLocationChanged(locationEngine.getLastLocation());
      }
    } else {
      onLocationChanged(null);
    }
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
    camera = new NavigationCamera(this, map, navigation);
  }

  /**
   * Initializes the {@link LocationLayerPlugin} to be used to draw the current
   * location.
   */
  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, map, null);
  }

  /**
   * Checks the intent used to launch this activity.
   * Will start navigation based on the data found in the {@link Intent}
   *
   * @param intent holds either a set of {@link Position} coordinates or a {@link DirectionsRoute}
   */
  private void checkLaunchData(Intent intent) {
    if (checkLaunchData) {
      if (launchWithRoute(intent)) {
        startRouteNavigation();
      } else {
        startCoordinateNavigation();
      }
    }
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && response.body().getRoutes() != null
      && response.body().getRoutes().size() > 0;
  }

  /**
   * Requests a new {@link DirectionsRoute}.
   * <p>
   * Will use {@link Location} bearing if we have a location with bearing.
   *
   * @param origin      start point
   * @param destination end point
   */
  private void fetchRoute(Position origin, Position destination) {
    NavigationRoute.Builder routeBuilder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination);

    if (locationHasBearing()) {
      fetchRouteWithBearing(routeBuilder);
    } else {
      routeBuilder.build().getRoute(this);
    }
  }

  /**
   * Check if the given {@link Intent} has been launched with a {@link DirectionsRoute}.
   *
   * @param intent possibly containing route
   * @return true if route found, false if not
   */
  private boolean launchWithRoute(Intent intent) {
    return intent.getBooleanExtra(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, false);
  }

  /**
   * Checks if the route should be simualted with a {@link MockLocationEngine}.
   *
   * @return true if simulation enabled, false if not
   */
  private boolean shouldSimulateRoute() {
    return preferences.getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false);
  }

  /**
   * Extracts the {@link DirectionsRoute}, adds a destination marker,
   * and starts navigation.
   */
  private void startRouteNavigation() {
    DirectionsRoute route = NavigationLauncher.extractRoute(this);
    if (route != null) {
      RouteLeg lastLeg = route.getLegs().get(route.getLegs().size() - 1);
      LegStep lastStep = lastLeg.getSteps().get(lastLeg.getSteps().size() - 1);
      destination = lastStep.getManeuver().asPosition();
      addDestinationMarker(destination);
      startNavigation(route);
      checkLaunchData = false;
    }
  }

  /**
   * Extracts the {@link Position} coordinates, adds a destination marker,
   * and fetches a route with the coordinates.
   */
  private void startCoordinateNavigation() {
    HashMap<String, Position> coordinates = NavigationLauncher.extractCoordinates(this);
    if (coordinates.size() > 0) {
      Position origin = coordinates.get(NavigationConstants.NAVIGATION_VIEW_ORIGIN);
      destination = coordinates.get(NavigationConstants.NAVIGATION_VIEW_DESTINATION);
      addDestinationMarker(destination);
      fetchRoute(origin, destination);
      checkLaunchData = false;
    }
  }

  /**
   * Sets up everything needed to begin navigation.
   * <p>
   * This includes drawing the route on the map, starting camera
   * tracking, giving {@link MapboxNavigation} a location engine,
   * enabling the {@link LocationLayerPlugin}, and showing the {@link InstructionView}.
   *
   * @param route used to start navigation for the first time
   */
  @SuppressWarnings( {"MissingPermission"})
  private void startNavigation(DirectionsRoute route) {
    if (shouldSimulateRoute()) {
      activateMockLocationEngine(route);
    }
    mapRoute.addRoute(route);
    camera.start(route);
    navigation.setLocationEngine(locationEngine);
    navigation.startNavigation(route);
    locationLayer.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);
    instructionView.show();
    navigationRunning = true;
  }

  /**
   * Updates the {@link NavigationMapRoute} and {@link MapboxNavigation} with
   * a new {@link DirectionsRoute}.
   *
   * @param route new route
   */
  private void updateNavigation(DirectionsRoute route) {
    mapRoute.addRoute(route);
    navigation.startNavigation(route);
  }

  /**
   * Creates the destination marker based on the
   * {@link Position} destination coordinate.
   *
   * @param destination where the marker should be placed
   */
  private void addDestinationMarker(Position destination) {
    IconFactory iconFactory = IconFactory.getInstance(this);
    Icon icon = iconFactory.fromResource(R.drawable.map_marker);
    LatLng markerPosition = new LatLng(destination.getLatitude(),
      destination.getLongitude());
    map.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(icon));
  }

  /**
   * Used to determine if a location has a bearing.
   *
   * @return true if bearing exists, false if not
   */
  private boolean locationHasBearing() {
    return location != null && location.hasBearing();
  }

  /**
   * Will finish building {@link NavigationRoute} after adding a bearing
   * and request the route.
   *
   * @param routeBuilder to fetch the route
   */
  private void fetchRouteWithBearing(NavigationRoute.Builder routeBuilder) {
    routeBuilder.addBearing(location.getBearing(), 90);
    routeBuilder.build().getRoute(this);
  }

  /**
   * Activates a new {@link MockLocationEngine} with the given
   * {@link DirectionsRoute}.
   *
   * @param route to be mocked
   */
  private void activateMockLocationEngine(DirectionsRoute route) {
    locationEngine = new MockLocationEngine(1000, 30, false);
    ((MockLocationEngine) locationEngine).setRoute(route);
    locationEngine.activate();
  }

  /**
   * Shuts down anything running in onDestroy
   */
  private void shutdownNavigation() {
    deactivateNavigation();
    deactivateLocationEngine();
    deactivateInstructionPlayer();
  }

  /**
   * Destroys the {@link InstructionPlayer} if not null
   */
  private void deactivateInstructionPlayer() {
    if (instructionPlayer != null) {
      instructionPlayer.onDestroy();
    }
  }

  /**
   * Destroys {@link MapboxNavigation} if not null
   */
  private void deactivateNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  /**
   * Deactivates and removes listeners
   * for the {@link LocationEngine} if not null
   */
  private void deactivateLocationEngine() {
    if (locationEngine != null) {
      locationEngine.removeLocationUpdates();
      locationEngine.removeLocationEngineListener(this);
      locationEngine.deactivate();
    }
  }
}
