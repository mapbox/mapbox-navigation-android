package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh;

import org.junit.Test;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RouteRefreshTest {

  @Test
  public void refreshEnqueuesCall() {
    MapboxDirectionsRefresh mapboxDirectionsRefresh = mock(MapboxDirectionsRefresh.class);
    RouteRefresh routeRefresh = new RouteRefresh(mapboxDirectionsRefresh);
    Callback callback = mock(CallbackImpl.class);

    routeRefresh.refresh(callback);

    verify(mapboxDirectionsRefresh).enqueueCall(callback);
  }

  private class CallbackImpl implements Callback {
    @Override
    public void onResponse(Call call, Response response) {
      // stub

    }
    @Override
    public void onFailure(Call call, Throwable throwable) {
      // stub
    }
  }
}
