package com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.SimplifiedCallback;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import retrofit2.Call;
import retrofit2.Response;

public class NavigationFragment extends Fragment implements OnNavigationReadyCallback, NavigationListener {

  private static final double ORIGIN_LONGITUDE = -77.04012393951416;
  private static final double ORIGIN_LATITUDE = 38.9111117447887;
  private static final double DESTINATION_LONGITUDE = -77.03847169876099;
  private static final double DESTINATION_LATITUDE = 38.91113678979344;

  private NavigationView navigationView;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.navigation_view_fragment_layout, container);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    navigationView = view.findViewById(R.id.navigation_view_fragment);
    navigationView.onCreate(savedInstanceState);
    navigationView.initialize(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      navigationView.onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    navigationView.onDestroy();
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    Point origin = Point.fromLngLat(ORIGIN_LONGITUDE, ORIGIN_LATITUDE);
    Point destination = Point.fromLngLat(DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
    fetchRoute(origin, destination);
  }

  @Override
  public void onCancelNavigation() {
    if (getActivity() != null) {
      getActivity().finish();
    }
  }

  @Override
  public void onNavigationFinished() {
    if (getActivity() != null) {
      getActivity().finish();
    }
  }

  @Override
  public void onNavigationRunning() {
    // no-op
  }

  private void fetchRoute(Point origin, Point destination) {
    NavigationRoute.builder(getContext())
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination)
      .build()
      .getRoute(new SimplifiedCallback() {
        @Override
        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
          DirectionsRoute directionsRoute = response.body().routes().get(0);
          startNavigation(directionsRoute);
        }
      });
  }

  private void startNavigation(DirectionsRoute directionsRoute) {
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(directionsRoute)
      .shouldSimulateRoute(true)
      .navigationListener(NavigationFragment.this)
      .build();
    navigationView.startNavigation(options);
  }
}
