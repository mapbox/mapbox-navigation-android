package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NavigationViewActivity extends AppCompatActivity implements Callback<DirectionsResponse> {

  private Button launchRouteBtn;
  private ProgressBar routeLoading;

  // Washington, D.C.
  private Position origin = Position.fromCoordinates(-77.009003, 38.889931);

  // Arlington, VA
  private Position destination = Position.fromCoordinates(-77.100703, 38.878337);

  private DirectionsRoute directionsRoute;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_view);
    fetchRoute();

    routeLoading = findViewById(R.id.routeLoading);
    Button launchCoordinatesBtn = findViewById(R.id.launchCoordinatesBtn);
    launchCoordinatesBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchNavigationWithCoordinates();
      }
    });

    launchRouteBtn = findViewById(R.id.launchRouteBtn);
    launchRouteBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchNavigationWithRoute();
      }
    });
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      directionsRoute = response.body().getRoutes().get(0);
      routeLoading.setVisibility(View.INVISIBLE);
      launchRouteBtn.setEnabled(true);
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    Timber.e(throwable.getMessage());
  }

  private void fetchRoute() {
    NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination)
      .build()
      .getRoute(this);
  }

  private void launchNavigationWithCoordinates() {
    // Launch with Coordinates
    NavigationLauncher.startNavigation(this, origin, destination);
  }

  private void launchNavigationWithRoute() {
    // Launch with Route
    if (directionsRoute != null) {
      NavigationLauncher.startNavigation(this, directionsRoute);
    } else {
      Toast.makeText(this, "Route is still loading", Toast.LENGTH_SHORT).show();
    }
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && response.body().getRoutes() != null
      && response.body().getRoutes().size() > 0;
  }
}
