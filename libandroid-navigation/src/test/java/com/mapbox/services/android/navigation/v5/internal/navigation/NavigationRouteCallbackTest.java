package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.internal.navigation.ElapsedTime;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationRouteCallback;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationRouteEventListener;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry;

import org.junit.Test;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationRouteCallbackTest {

  @Test
  public void onResponse_callbackIsCalled() {
    NavigationTelemetry telemetry = mock(NavigationTelemetry.class);
    NavigationRouteEventListener listener = mock(NavigationRouteEventListener.class);
    Callback<DirectionsResponse> callback = mock(Callback.class);
    Call call = mock(Call.class);
    String uuid = "some_uuid";
    Response response = buildMockResponse(uuid);
    NavigationRouteCallback routeCallback = new NavigationRouteCallback(telemetry, listener, callback);

    routeCallback.onResponse(call, response);

    verify(callback).onResponse(call, response);
  }

  @Test
  public void onResponse_validResponseSendsEvent() {
    NavigationTelemetry telemetry = mock(NavigationTelemetry.class);
    NavigationRouteEventListener listener = mock(NavigationRouteEventListener.class);
    ElapsedTime elapsedTime = mock(ElapsedTime.class);
    when(listener.getTime()).thenReturn(elapsedTime);
    Callback<DirectionsResponse> callback = mock(Callback.class);
    Call call = mock(Call.class);
    String uuid = "some_uuid";
    Response response = buildMockResponse(uuid);
    NavigationRouteCallback routeCallback = new NavigationRouteCallback(telemetry, listener, callback);

    routeCallback.onResponse(call, response);

    verify(telemetry).routeRetrievalEvent(eq(elapsedTime), eq(uuid));
  }

  @Test
  public void onFailure_callbackIsCalled() {
    NavigationTelemetry telemetry = mock(NavigationTelemetry.class);
    NavigationRouteEventListener listener = mock(NavigationRouteEventListener.class);
    Callback<DirectionsResponse> callback = mock(Callback.class);
    Call call = mock(Call.class);
    Throwable throwable = mock(Throwable.class);
    NavigationRouteCallback routeCallback = new NavigationRouteCallback(telemetry, listener, callback);

    routeCallback.onFailure(call, throwable);

    verify(callback).onFailure(call, throwable);
  }

  @NonNull
  private Response buildMockResponse(String uuid) {
    Response response = mock(Response.class);
    DirectionsResponse directionsResponse = mock(DirectionsResponse.class);
    ArrayList<DirectionsRoute> routes = new ArrayList<>();
    routes.add(mock(DirectionsRoute.class));
    when(directionsResponse.uuid()).thenReturn(uuid);
    when(directionsResponse.routes()).thenReturn(routes);
    when(response.body()).thenReturn(directionsResponse);
    return response;
  }
}