package com.mapbox.services.android.navigation.testapp.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.Constants;
import com.mapbox.services.android.testapp.R;
import com.mapbox.services.android.testapp.Utils;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.navigation.v5.RouteUtils;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteUtilsV5Activity extends AppCompatActivity implements OnMapReadyCallback {

  private static final String LOG_TAG = "RouteUtilsV5Activity";

  private MapView mapView = null;
  private MapboxMap mapboxMap = null;

  private LatLng from = null;
  private LatLng to = null;
  private DirectionsRoute currentRoute = null;

  private Icon tapIcon;
  private Marker userTap = null;
  private List<Polyline> snapLines = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_route_utils_v5);

    tapIcon = Utils.drawableToIcon(this, R.drawable.ic_my_location_black_24dp);

    // Set up a standard Mapbox map
    mapView = (MapView) findViewById(R.id.mapview);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    mapboxMap.setStyleUrl(Style.MAPBOX_STREETS);

    // Dupont Circle
    LatLng target = new LatLng(38.90962, -77.04341);

    // Move map
    CameraPosition cameraPosition = new CameraPosition.Builder()
      .target(target)
      .zoom(14)
      .build();
    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
      @Override
      public void onMapClick(@NonNull LatLng point) {
        if (from == null) {
          setFrom(point);
        } else if (to == null) {
          setTo(point);
        } else {
          doUtils(point);
        }
      }
    });
  }

  private void setFrom(LatLng point) {
    from = point;
    mapboxMap.addMarker(new MarkerOptions()
      .position(point)
      .title("From"));
  }

  private void setTo(LatLng point) {
    to = point;
    mapboxMap.addMarker(new MarkerOptions()
      .position(point)
      .title("To"));

    getRoute(Position.fromCoordinates(from.getLongitude(), from.getLatitude()),
      Position.fromCoordinates(to.getLongitude(), to.getLatitude()));
  }

  private void doUtils(LatLng point) {
    // Remove previous
    if (userTap != null) {
      mapboxMap.removeMarker(userTap);
    }

    userTap = mapboxMap.addMarker(new MarkerOptions().position(point).setIcon(tapIcon));

    RouteUtils routeUtils = new RouteUtils();
    RouteLeg route = currentRoute.getLegs().get(0);
    Position position = Position.fromCoordinates(point.getLongitude(), point.getLatitude());

    // General situational message
    String message = String.format(Locale.US, "You're closest to step %d/%d (%s)",
      routeUtils.getClosestStep(position, route) + 1,
      route.getSteps().size(),
      routeUtils.isOffRoute(position, route) ? "off-route" : "not off-route");
    showMessage(message);

    // Remove previous lines
    if (snapLines != null && snapLines.size() > 0) {
      for (Polyline snapLine : snapLines) {
        mapboxMap.removePolyline(snapLine);
      }
    }

    // Draw snap to route lines
    snapLines = new ArrayList<>();
    for (int stepIndex = 0; stepIndex < route.getSteps().size(); stepIndex++) {
      Position snapPoint = RouteUtils.getSnapToRoute(position, route, stepIndex);
      LatLng[] points = new LatLng[] {
        point,
        new LatLng(snapPoint.getLatitude(), snapPoint.getLongitude())};
      snapLines.add(mapboxMap.addPolyline(new PolylineOptions()
        .add(points)
        .color(Color.parseColor("#f9886c"))
        .width(2)));
    }

    // Log some extra info
    for (int stepIndex = 0; stepIndex < route.getSteps().size(); stepIndex++) {
      Log.d(LOG_TAG, String.format("Step %d: in step = %b, distance = %.1fkm",
        stepIndex + 1,
        routeUtils.isInStep(position, route, stepIndex),
        routeUtils.getDistanceToStep(position, route, stepIndex)));
    }
  }

  private void getRoute(Position origin, Position destination) throws ServicesException {
    ArrayList<Position> positions = new ArrayList<>();
    positions.add(origin);
    positions.add(destination);

    MapboxDirections client = new MapboxDirections.Builder()
      .setAccessToken(Utils.getMapboxAccessToken(this))
      .setCoordinates(positions)
      .setProfile(DirectionsCriteria.PROFILE_DRIVING)
      .setSteps(true)
      .setOverview(DirectionsCriteria.OVERVIEW_FULL)
      .build();

    client.enqueueCall(new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        // You can get generic HTTP info about the response
        Log.d(LOG_TAG, "Response code: " + response.code());
        if (response.body() == null) {
          Log.e(LOG_TAG, "No routes found, make sure you set the right user and access token.");
          return;
        }

        // Print some info about the route
        currentRoute = response.body().getRoutes().get(0);
        Log.d(LOG_TAG, "Distance: " + currentRoute.getDistance());
        showMessage(String.format(Locale.US, "Route has %d steps and it's %.1f meters long.",
          currentRoute.getLegs().get(0).getSteps().size(),
          currentRoute.getDistance()));

        // Draw the route on the map
        drawRoute(currentRoute);
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Log.e(LOG_TAG, "Error: " + throwable.getMessage());
        showMessage("Error: " + throwable.getMessage());
      }
    });
  }

  private void drawRoute(DirectionsRoute route) {
    // We're gonna draw each step in an alternating color
    String[] colors = new String[] {"#3887be", "#56b881"}; // Blue, green

    List<Position> coordinates;
    LatLng[] points;
    int colorIndex = 0;
    for (int i = 0; i < route.getLegs().get(0).getSteps().size(); i++) {
      LegStep step = route.getLegs().get(0).getSteps().get(i);
      coordinates = PolylineUtils.decode(step.getGeometry(), Constants.PRECISION_6);
      points = new LatLng[coordinates.size()];
      for (int j = 0; j < coordinates.size(); j++) {
        points[j] = new LatLng(
          coordinates.get(j).getLatitude(),
          coordinates.get(j).getLongitude());
      }

      colorIndex ^= 1;
      mapboxMap.addPolyline(new PolylineOptions()
        .add(points)
        .color(Color.parseColor(colors[colorIndex]))
        .width(5));
    }
  }

  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

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
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
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
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }
}
