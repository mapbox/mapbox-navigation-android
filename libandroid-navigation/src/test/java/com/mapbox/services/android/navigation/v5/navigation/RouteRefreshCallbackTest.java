package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse;

import org.junit.Test;

import retrofit2.Call;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteRefreshCallbackTest {
  @Test
  public void onErrorIsCalled_nullLegs() {
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    RouteRefreshCallback routeRefreshCallback =
      new RouteRefreshCallback(directionsRoute, 1, refreshCallback);
    Call<DirectionsRefreshResponse> call = mock(Call.class);
    Response<DirectionsRefreshResponse> response = mock(Response.class);
    DirectionsRefreshResponse directionsRefreshResponse = mock(DirectionsRefreshResponse.class);
    when(directionsRoute.legs()).thenReturn(null);
    when(directionsRefreshResponse.route()).thenReturn(directionsRoute);
    when(response.body()).thenReturn(directionsRefreshResponse);

    routeRefreshCallback.onResponse(call, response);

    verify(refreshCallback).onError(any(RefreshError.class));
  }

  @Test
  public void onErrorIsCalled_nullBody() {
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    RouteRefreshCallback routeRefreshCallback =
      new RouteRefreshCallback(directionsRoute, 1, refreshCallback);
    Call<DirectionsRefreshResponse> call = mock(Call.class);
    Response<DirectionsRefreshResponse> response = mock(Response.class);
    when(response.body()).thenReturn(null);

    routeRefreshCallback.onResponse(call, response);

    verify(refreshCallback).onError(any(RefreshError.class));
  }

  @Test
  public void onErrorIsCalled_nullRoute() {
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    RouteRefreshCallback routeRefreshCallback =
      new RouteRefreshCallback(mock(DirectionsRoute.class), 1, refreshCallback);
    Call<DirectionsRefreshResponse> call = mock(Call.class);
    Response<DirectionsRefreshResponse> response = mock(Response.class);
    DirectionsRefreshResponse directionsRefreshResponse = mock(DirectionsRefreshResponse.class);
    when(directionsRefreshResponse.route()).thenReturn(null);
    when(response.body()).thenReturn(directionsRefreshResponse);

    routeRefreshCallback.onResponse(call, response);

    verify(refreshCallback).onError(any(RefreshError.class));
  }

  @Test
  public void onRefreshIsCalled() {
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    DirectionsRoute directionsRoute1 = mock(DirectionsRoute.class);
    DirectionsRoute directionsRoute2 = mock(DirectionsRoute.class);
    DirectionsRoute directionsRoute3 = mock(DirectionsRoute.class);
    int legIndex = 1;
    RouteAnnotationUpdater routeAnnotationUpdater = mock(RouteAnnotationUpdater.class);
    RouteRefreshCallback routeRefreshCallback =
      new RouteRefreshCallback(routeAnnotationUpdater, directionsRoute1, legIndex, refreshCallback);
    Call<DirectionsRefreshResponse> call = mock(Call.class);
    Response<DirectionsRefreshResponse> response = mock(Response.class);
    DirectionsRefreshResponse directionsRefreshResponse = mock(DirectionsRefreshResponse.class);
    when(directionsRefreshResponse.route()).thenReturn(directionsRoute2);
    when(response.body()).thenReturn(directionsRefreshResponse);
    when(routeAnnotationUpdater.update(directionsRoute1, directionsRoute2, legIndex)).thenReturn(directionsRoute3);

    routeRefreshCallback.onResponse(call, response);

    verify(refreshCallback).onRefresh(any(DirectionsRoute.class));
  }
}
