package com.mapbox.navigation.ui;

public class OfflineRouteFoundCallbackTest {

  /*@Test
  public void onRouteFound_routerIsUpdated() {
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    DirectionsRoute offlineRoute = mock(DirectionsRoute.class);
    OfflineRouteFoundCallback callback = new OfflineRouteFoundCallback(router);

    callback.onRouteFound(offlineRoute);

    verify(router).updateCurrentRoute(offlineRoute);
  }

  @Test
  public void onRouteFound_callStatusIsUpdated() {
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    DirectionsRoute offlineRoute = mock(DirectionsRoute.class);
    OfflineRouteFoundCallback callback = new OfflineRouteFoundCallback(router);

    callback.onRouteFound(offlineRoute);

    verify(router).updateCallStatusReceived();
  }

  @Test
  public void onError_routerReceivesErrorMessage() {
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    OfflineError error = mock(OfflineError.class);
    String errorMessage = "error message";
    when(error.getMessage()).thenReturn(errorMessage);
    OfflineRouteFoundCallback callback = new OfflineRouteFoundCallback(router);

    callback.onError(error);

    verify(router).onRequestError(eq(errorMessage));
  }

  @Test
  public void onError_callStatusIsUpdated() {
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    OfflineError error = mock(OfflineError.class);
    String errorMessage = "error message";
    when(error.getMessage()).thenReturn(errorMessage);
    OfflineRouteFoundCallback callback = new OfflineRouteFoundCallback(router);

    callback.onError(error);

    verify(router).updateCallStatusReceived();
  }*/
}