package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.MutableLiveData;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationViewRouteEngineListenerTest {

  @Test
  public void onRouteUpdate_checksUpdateRouteCalled() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    DirectionsRoute aDirectionsRoute = mock(DirectionsRoute.class);
    NavigationViewRouteEngineListener theRouteEngineListener
      = new NavigationViewRouteEngineListener(mockedNavigationViewModel);

    theRouteEngineListener.onRouteUpdate(aDirectionsRoute);

    verify(mockedNavigationViewModel).updateRoute(eq(aDirectionsRoute));
  }

  @Test
  public void checksSendEventFailedRerouteCalledIfNavigationIsOffRoute() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    when(mockedNavigationViewModel.isOffRoute()).thenReturn(true);
    Throwable aThrowable = mock(Throwable.class);
    String anError = "An error occurred!";
    when(aThrowable.getMessage()).thenReturn(anError);
    NavigationViewRouteEngineListener theRouteEngineListener
      = new NavigationViewRouteEngineListener(mockedNavigationViewModel);

    theRouteEngineListener.onRouteRequestError(aThrowable);

    verify(mockedNavigationViewModel).sendEventFailedReroute(eq(anError));
  }

  @Test
  public void checksSendEventFailedRerouteNotCalledIfNavigationIsNotOffRoute() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    when(mockedNavigationViewModel.isOffRoute()).thenReturn(false);
    Throwable aThrowable = mock(Throwable.class);
    String anError = "An error occurred!";
    when(aThrowable.getMessage()).thenReturn(anError);
    NavigationViewRouteEngineListener theRouteEngineListener
      = new NavigationViewRouteEngineListener(mockedNavigationViewModel);

    theRouteEngineListener.onRouteRequestError(aThrowable);

    verify(mockedNavigationViewModel, times(0)).sendEventFailedReroute(eq(anError));
  }

  @Test
  public void checksOnDestinationSet() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    Point mockedPoint = mock(Point.class);
    MutableLiveData<Point> mockedDestination = mock(MutableLiveData.class);
    when(mockedNavigationViewModel.retrieveDestination()).thenReturn(mockedDestination);
    NavigationViewRouteEngineListener theRouteEngineListener
      = new NavigationViewRouteEngineListener(mockedNavigationViewModel);

    theRouteEngineListener.onDestinationSet(mockedPoint);

    verify(mockedDestination).setValue(eq(mockedPoint));
  }
}