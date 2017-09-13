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

import com.google.gson.Gson;
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
import timber.log.Timber;

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
  private DirectionsRoute route;
  private boolean checkLaunchData;
  private boolean navigationRunning;
  private boolean restartNavigation;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Timber.d("onCreate()");
    setContentView(R.layout.navigation_view_layout);
    checkLaunchData = savedInstanceState == null;
    Timber.d("checkLaunchData: " + checkLaunchData);
    bind();
    initClickListeners();
    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    initMap(savedInstanceState);
    initSummaryBottomSheet();
    initNavigation();
    initVoiceInstructions();
  }

  @SuppressWarnings({"MissingPermission"})
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
    Timber.d("onSaveInstanceState()");
    outState.putBoolean("navigation_running", navigationRunning);
    outState.putParcelable("current_location", location);
    outState.putString("current_route", new Gson().toJson(route));
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    Timber.d("onRestoreInstanceState()");
    restartNavigation = savedInstanceState.getBoolean("navigation_running");
    if (restartNavigation) {
      location = savedInstanceState.getParcelable("current_location");
      String currentRoute = savedInstanceState.getString("current_route");
      route = new Gson().fromJson(currentRoute, DirectionsRoute.class);
    }
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    map = mapboxMap;
    map.setOnScrollListener(this);
    initRoute();
    initCamera();
    initLocationLayer();
    initLocation();
    startNavigation();
  }

  @Override
  public void onScroll() {
    if (!summaryBehavior.isHideable()) {
      summaryBehavior.setHideable(true);
      summaryBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
      camera.setCameraTrackingLocation(false);
    }
  }

  @SuppressWarnings({"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    this.location = location;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayer.forceLocationUpdate(location);
  }

  @Override
  public void userOffRoute(Location location) {
    Position newOrigin = Position.fromLngLat(location.getLongitude(), location.getLatitude());
    fetchRoute(newOrigin, destination);
  }

  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    instructionPlayer.play(instruction);
    if (identifier == NavigationConstants.ARRIVAL_MILESTONE) {
      finish();
    }
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      route = response.body().getRoutes().get(0);
      if (navigationRunning) {
        updateNavigation(route);
        instructionView.hideRerouteState();
        summaryBottomSheet.hideRerouteState();
      } else {
        startNavigation(route);
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable t) {}

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

  private void initMap(Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

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

  private void initNavigation() {
    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());
    navigation.addProgressChangeListener(this);
    navigation.addProgressChangeListener(instructionView);
    navigation.addProgressChangeListener(summaryBottomSheet);
    navigation.addMilestoneEventListener(this);
    navigation.addOffRouteListener(this);
    navigation.addOffRouteListener(summaryBottomSheet);
    navigation.addOffRouteListener(instructionView);
  }

  private void initVoiceInstructions() {
    instructionPlayer = new NavigationInstructionPlayer(this,
      preferences.getString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, null));
  }

  private void initRoute() {
    mapRoute = new NavigationMapRoute(mapView, map, NavigationConstants.ROUTE_BELOW_LAYER);
    if (route != null) {
      Timber.d("Adding route...");
      mapRoute.addRoute(route);
    }
  }

  private void initCamera() {
    camera = new NavigationCamera(this, map, navigation);
    if (location != null) {
      Timber.d("Camera resuming...");
      camera.resume(location);
    }
  }

  @SuppressWarnings({"MissingPermission"})
  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, map, null);
    if (navigationRunning) {
      Timber.d("Location layer enabled...");
      locationLayer.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);
    }
  }

  @SuppressWarnings({"MissingPermission"})
  private void initLocation() {
    if (shouldSimulateRoute()) {
      checkLaunchData(getIntent());
    } else {
      Timber.d("Starting up location engine...");
      locationEngine = navigation.getLocationEngine();
      locationEngine.addLocationEngineListener(this);
      locationEngine.activate();

      if (locationEngine.getLastLocation() != null) {
        onLocationChanged(locationEngine.getLastLocation());
      }
    }
  }

  private void startNavigation() {
    Timber.d("Starting navigation onMapReady...");
    if (restartNavigation) {
      Timber.d("Restarting navigation...");
      navigation.setLocationEngine(locationEngine);
      navigation.startNavigation(route);
      navigationRunning = true;
    } else {
      checkLaunchData(getIntent());
    }
  }

  private void checkLaunchData(Intent intent) {
    if (checkLaunchData) {
      if (launchWithRoute(intent)) {
        startRouteNavigation();
      } else {
        startCoordinateNavigation();
      }
    }
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && response.body().getRoutes() != null
      && response.body().getRoutes().size() > 0;
  }

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

  private boolean launchWithRoute(Intent intent) {
    return intent.getBooleanExtra(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, false);
  }

  private boolean shouldSimulateRoute() {
    return preferences.getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false);
  }

  private void startRouteNavigation() {
    route = NavigationLauncher.extractRoute(this);
    if (route != null) {
      RouteLeg lastLeg = route.getLegs().get(route.getLegs().size() - 1);
      LegStep lastStep = lastLeg.getSteps().get(lastLeg.getSteps().size() - 1);
      destination = lastStep.getManeuver().asPosition();
      addDestinationMarker(destination);
      startNavigation(route);
      checkLaunchData = false;
    }
  }

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

  @SuppressWarnings({"MissingPermission"})
  private void startNavigation(DirectionsRoute route) {
    Timber.d("Starting navigation with DirectionsRoute...");
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

  private void updateNavigation(DirectionsRoute route) {
    mapRoute.addRoute(route);
    navigation.startNavigation(route);
  }

  private void addDestinationMarker(Position destination) {
    IconFactory iconFactory = IconFactory.getInstance(this);
    Icon icon = iconFactory.fromResource(R.drawable.map_marker);
    LatLng markerPosition = new LatLng(destination.getLatitude(),
      destination.getLongitude());
    map.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(icon));
  }

  private boolean locationHasBearing() {
    return location != null && location.hasBearing();
  }

  private void fetchRouteWithBearing(NavigationRoute.Builder routeBuilder) {
    routeBuilder.addBearing(location.getBearing(), 90);
    routeBuilder.build().getRoute(this);
  }

  private void activateMockLocationEngine(DirectionsRoute route) {
    locationEngine = new MockLocationEngine(1000, 30, false);
    ((MockLocationEngine) locationEngine).setRoute(route);
    locationEngine.activate();
  }

  private void shutdownNavigation() {
    deactivateNavigation();
    deactivateLocationEngine();
    deactivateInstructionPlayer();
  }

  private void deactivateInstructionPlayer() {
    if (instructionPlayer != null) {
      instructionPlayer.onDestroy();
    }
  }

  private void deactivateNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  private void deactivateLocationEngine() {
    if (locationEngine != null) {
      locationEngine.removeLocationUpdates();
      locationEngine.removeLocationEngineListener(this);
      locationEngine.deactivate();
    }
  }
}
