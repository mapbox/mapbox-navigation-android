package com.mapbox.navigation.examples.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.base.logger.model.Tag;
import com.mapbox.navigation.examples.R;
import com.mapbox.navigation.examples.ui.notification.CustomNavigationNotification;
import com.mapbox.navigation.examples.utils.Utils;
import com.mapbox.navigation.logger.LogEntry;
import com.mapbox.navigation.logger.LogPriority;
import com.mapbox.navigation.logger.LoggerObserver;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter;
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsObserver;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.RefreshCallback;
import com.mapbox.services.android.navigation.v5.navigation.RefreshError;
import com.mapbox.services.android.navigation.v5.navigation.RouteRefresh;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MockNavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationEventListener,
        MilestoneEventListener, OffRouteListener, RefreshCallback, MetricsObserver,
        LoggerObserver {

  private static final int BEGIN_ROUTE_MILESTONE = 1001;
  private static final double TWENTY_FIVE_METERS = 25d;

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  @BindView(R.id.startRouteButton)
  Button startRouteButton;

  private MapboxMap mapboxMap;

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private Point destination;
  private Point waypoint;
  private RouteRefresh routeRefresh;
  private boolean isRefreshing = false;

  private static class MyBroadcastReceiver extends BroadcastReceiver {
    private final WeakReference<MapboxNavigation> weakNavigation;

    MyBroadcastReceiver(MapboxNavigation navigation) {
      this.weakNavigation = new WeakReference<>(navigation);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      MapboxNavigation navigation = weakNavigation.get();
      navigation.stopNavigation();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mock_navigation);
    ButterKnife.bind(this);
    MapboxLogger.INSTANCE.setLogLevel(LogPriority.VERBOSE);
    MapboxLogger.INSTANCE.setObserver(this);
    MapboxMetricsReporter.INSTANCE.setMetricsObserver(this);
    routeRefresh = new RouteRefresh(Mapbox.getAccessToken(), getApplicationContext());

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    Context context = getApplicationContext();
    CustomNavigationNotification customNotification = new CustomNavigationNotification(context);
    MapboxNavigationOptions options = new MapboxNavigationOptions.Builder()
      .navigationNotification(customNotification)
      .build();

    navigation = new MapboxNavigation(
            this,
            Mapbox.getAccessToken(),
            options
    );

    navigation.addMilestone(new RouteMilestone.Builder()
      .setIdentifier(BEGIN_ROUTE_MILESTONE)
      .setInstruction(new BeginRouteInstruction())
      .setTrigger(
        Trigger.all(
          Trigger.lt(TriggerProperty.STEP_INDEX, 3),
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
          Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
        )
      ).build());
    customNotification.register(new MyBroadcastReceiver(navigation), context);
  }

  @OnClick(R.id.startRouteButton)
  public void onStartRouteClick() {
    boolean isValidNavigation = navigation != null;
    boolean isValidRoute = route != null && route.distance() > TWENTY_FIVE_METERS;
    if (isValidNavigation && isValidRoute) {

      // Hide the start button
      startRouteButton.setVisibility(View.INVISIBLE);

      // Attach all of our navigation listeners.
      navigation.addNavigationEventListener(this);
      navigation.addProgressChangeListener(this);
      navigation.addMilestoneEventListener(this);
      navigation.addOffRouteListener(this);

      ((ReplayRouteLocationEngine) locationEngine).assign(route);
      navigation.setLocationEngine(locationEngine);
      mapboxMap.getLocationComponent().setLocationComponentEnabled(true);
      navigation.startNavigation(route);
      mapboxMap.removeOnMapClickListener(this);
    }
  }

  @OnClick(R.id.newLocationFab)
  public void onNewLocationClick() {
    newOrigin();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      LatLng latLng = Utils.getRandomLatLng(new double[] {-77.1825, 38.7825, -76.9790, 39.0157});
      ((ReplayRouteLocationEngine) locationEngine).assignLastLocation(
        Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())
      );
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.addOnMapClickListener(this);
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      LocationComponent locationComponent = mapboxMap.getLocationComponent();
      locationComponent.activateLocationComponent(this, style);
      locationComponent.setRenderMode(RenderMode.GPS);
      locationComponent.setLocationComponentEnabled(false);
      navigationMapRoute = new NavigationMapRoute(navigation, mapView, mapboxMap);
      Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint", Snackbar.LENGTH_LONG).show();
      locationEngine = new ReplayRouteLocationEngine();
      newOrigin();
    });
  }

  @Override
  public boolean onMapClick(@NonNull LatLng point) {
    if (destination == null) {
      destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else if (waypoint == null) {
      waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else {
      Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
    }
    mapboxMap.addMarker(new MarkerOptions().position(point));
    calculateRoute();
    return false;
  }

  @SuppressLint("MissingPermission")
  private void calculateRoute() {
    locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
      @Override
      public void onSuccess(LocationEngineResult result) {
        findRouteWith(result);
      }

      @Override
      public void onFailure(@NonNull Exception exception) {
        MapboxLogger.INSTANCE.e(new Message(exception.getLocalizedMessage()), exception);
      }
    });
  }

  private void findRouteWith(LocationEngineResult result) {
    Location userLocation = result.getLastLocation();
    if (userLocation == null) {
      MapboxLogger.INSTANCE.d(new Message("calculateRoute: User location is null, therefore, origin can't be set."));
      return;
    }
    Point origin = Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude());
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    final NavigationRoute.Builder navigationRouteBuilder = NavigationRoute.builder(this)
      .accessToken(Mapbox.getAccessToken());
    navigationRouteBuilder.origin(origin);
    navigationRouteBuilder.destination(destination);
    if (waypoint != null) {
      navigationRouteBuilder.addWaypoint(waypoint);
    }
    navigationRouteBuilder.enableRefresh(true);
    navigationRouteBuilder.build().getRoute(new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
        MapboxLogger.INSTANCE.d(new Message("Url: " + call.request().url().toString()));
        if (response.body() != null) {
          if (!response.body().routes().isEmpty()) {
            MockNavigationActivity.this.route = response.body().routes().get(0);
            navigationMapRoute.addRoutes(response.body().routes());
            startRouteButton.setVisibility(View.VISIBLE);
          }
        }
      }

      @Override
      public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
        MapboxLogger.INSTANCE.e(new Message("onFailure: navigation.getRoute()"), throwable);
      }
    });
  }

  /*
   * Navigation listeners
   */

  @Override
  public void onMilestoneEvent(@NotNull RouteProgress routeProgress, @NotNull String instruction, Milestone milestone) {
    MapboxLogger.INSTANCE.d(new Message("Milestone Event Occurred with id: " + milestone.getIdentifier()));
    MapboxLogger.INSTANCE.d(new Message("Voice instruction: " + instruction));
  }

  @Override
  public void onRunning(boolean running) {
    if (running) {
      MapboxLogger.INSTANCE.d(new Message("onRunning: Started"));
    } else {
      MapboxLogger.INSTANCE.d(new Message("onRunning: Stopped"));
    }
  }

  @Override
  public void userOffRoute(@NotNull Location location) {
    Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onProgressChange(@NotNull Location location, @NotNull RouteProgress routeProgress) {
    mapboxMap.getLocationComponent().forceLocationUpdate(location);
    if (!isRefreshing) {
      isRefreshing = true;
      routeRefresh.refresh(routeProgress, this);
    }
    MapboxLogger.INSTANCE.d(
      new Message("onProgressChange: fraction of route traveled: " + routeProgress.fractionTraveled())
    );
  }

  /*
   * Activity lifecycle methods
   */

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
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    MapboxMetricsReporter.INSTANCE.removeObserver();
    MapboxLogger.INSTANCE.removeObserver();
    navigation.onDestroy();
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onRefresh(@NotNull DirectionsRoute directionsRoute) {
    navigation.startNavigation(directionsRoute);
    isRefreshing = false;
  }

  @Override
  public void onError(@NotNull RefreshError error) {
    isRefreshing = false;
  }

  @Override
  public void onMetricUpdated(@NotNull @MetricEvent.Metric String metricName, @NotNull String jsonStringData) {
    MapboxLogger.INSTANCE.d(new Tag("METRICS_LOG"), new Message(metricName));
    MapboxLogger.INSTANCE.d(new Tag("METRICS_LOG"), new Message(jsonStringData));
  }

  @Override
  public void log(int level, @NotNull LogEntry entry) {
    if (entry.getTag() != null) {
      Timber.tag(entry.getTag());
    }
    switch (level) {
      case LogPriority.VERBOSE:
        Timber.v(entry.getThrowable(), entry.getMessage());
        break;
      case LogPriority.DEBUG:
        Timber.d(entry.getThrowable(), entry.getMessage());
        break;
      case LogPriority.INFO:
        Timber.i(entry.getThrowable(), entry.getMessage());
        break;
      case LogPriority.WARN:
        Timber.w(entry.getThrowable(), entry.getMessage());
        break;
      case LogPriority.ERROR:
        Timber.e(entry.getThrowable(), entry.getMessage());
        break;
      default: break;
    }
  }

  private static class BeginRouteInstruction extends Instruction {

    @NotNull
    @Override
    public String buildInstruction(@NotNull RouteProgress routeProgress) {
      return "Have a safe trip!";
    }
  }
}
