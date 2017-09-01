package com.mapbox.services.android.navigation.ui.v5;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationSource;
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
import com.mapbox.services.commons.models.Position;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.services.android.telemetry.location.LocationEnginePriority.HIGH_ACCURACY;

public class NavigationView extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnScrollListener,
  LocationEngineListener, ProgressChangeListener, OffRouteListener, Callback<DirectionsResponse> {

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

  private MapboxMap map;
  private MapboxNavigation navigation;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationEngine locationEngine;
  private LocationLayerPlugin locationLayer;

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

    initMap(savedInstanceState);
    initSummaryBottomSheet();
    initNavigation();
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
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

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

  @Override
  public void onScroll() {
    summaryBehavior.setHideable(true);
    summaryBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    recenterBtn.setVisibility(View.VISIBLE);
    camera.setCameraTrackingLocation(false);
  }

  @SuppressWarnings({"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    this.location = location;
    checkLaunchData(getIntent());
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
    // TODO Send message
  }

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
        // TODO Finish with cancelled result code
        finish();
      }
    });
    expandArrow.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (summaryBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
          summaryBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
          summaryBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
      }
    });
    recenterBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        summaryBehavior.setHideable(false);
        summaryBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        camera.resetCameraPosition();
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
    navigation = new MapboxNavigation(this);
    navigation.addProgressChangeListener(this);
    navigation.addProgressChangeListener(instructionView);
    navigation.addProgressChangeListener(summaryBottomSheet);
    navigation.addMilestoneEventListener(instructionView);
    navigation.addOffRouteListener(this);
    navigation.addOffRouteListener(summaryBottomSheet);
    navigation.addOffRouteListener(instructionView);
  }

  @SuppressWarnings({"MissingPermission"})
  private void initLocation() {
    locationEngine = new LocationSource(this);
    locationEngine.setPriority(HIGH_ACCURACY);
    locationEngine.setInterval(0);
    locationEngine.setFastestInterval(1000);
    locationEngine.addLocationEngineListener(this);
    locationEngine.activate();

    if (locationEngine.getLastLocation() != null) {
      onLocationChanged(locationEngine.getLastLocation());
    }
  }

  private void initRoute() {
    mapRoute = new NavigationMapRoute(mapView, map);
  }

  private void initCamera() {
    camera = new NavigationCamera(this, map, navigation);
  }

  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, map, null);
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

  private void startRouteNavigation() {
    DirectionsRoute route = NavigationLauncher.extractRoute(this);
    if (route != null) {
      startNavigation(route);
      checkLaunchData = false;
    }
  }

  private void startCoordinateNavigation() {
    HashMap<String, Position> coordinates = NavigationLauncher.extractCoordinates(this);
    if (coordinates.size() > 0) {
      Position origin = coordinates.get(NavigationConstants.NAVIGATION_VIEW_ORIGIN);
      destination = coordinates.get(NavigationConstants.NAVIGATION_VIEW_DESTINATION);
      fetchRoute(origin, destination);
      checkLaunchData = false;
    }
  }

  @SuppressWarnings({"MissingPermission"})
  private void startNavigation(DirectionsRoute route) {
    //    activateMockLocationEngine(route);
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
}
