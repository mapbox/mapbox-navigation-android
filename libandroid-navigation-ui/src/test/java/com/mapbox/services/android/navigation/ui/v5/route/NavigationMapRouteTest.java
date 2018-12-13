package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NavigationMapRouteTest {

  @Test
  public void checksMapClickListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);

    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedStyleRes, mockedBelowLayer,
      mockedMapClickListener, mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);

    verify(mockedMapboxMap, times(1)).addOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);

    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedStyleRes, mockedBelowLayer,
      mockedMapClickListener, mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);

    verify(mockedMapView, times(1))
      .addOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapRouteProgressChangeListenerIsAddedAtConstructionTime() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);

    new NavigationMapRoute(mockedNavigation, mockedMapView, mockedMapboxMap, mockedStyleRes, mockedBelowLayer,
      mockedMapClickListener, mockedDidFinishLoadingStyleListener, mockedProgressChangeListener);

    verify(mockedNavigation, times(1))
      .addProgressChangeListener(eq(mockedProgressChangeListener));
  }

  @Test
  public void checksMapClickListenerIsNotAddedIfIsMapClickListenerAdded() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedStyleRes, mockedBelowLayer, mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    theNavigationMapRoute.onStart();

    verify(mockedMapboxMap, times(1)).addOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsNotAddedIfIsDidFinishLoadingStyleListenerAdded() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedStyleRes, mockedBelowLayer, mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    theNavigationMapRoute.onStart();

    verify(mockedMapView, times(1))
      .addOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapClickListenerIsRemovedInOnStop() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedStyleRes, mockedBelowLayer, mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    theNavigationMapRoute.onStop();

    verify(mockedMapboxMap, times(1)).removeOnMapClickListener(eq(mockedMapClickListener));
  }

  @Test
  public void checksDidFinishLoadingStyleListenerIsRemovedInOnStop() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedStyleRes, mockedBelowLayer, mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    theNavigationMapRoute.onStop();

    verify(mockedMapView, times(1))
      .removeOnDidFinishLoadingStyleListener(eq(mockedDidFinishLoadingStyleListener));
  }

  @Test
  public void checksMapRouteProgressChangeListenerIsRemovedInOnStop() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapView mockedMapView = mock(MapView.class);
    MapboxMap mockedMapboxMap = mock(MapboxMap.class);
    int mockedStyleRes = 0;
    String mockedBelowLayer = "mocked_below_layer";
    MapboxMap.OnMapClickListener mockedMapClickListener = mock(MapboxMap.OnMapClickListener.class);
    MapView.OnDidFinishLoadingStyleListener mockedDidFinishLoadingStyleListener =
      mock(MapView.OnDidFinishLoadingStyleListener.class);
    MapRouteProgressChangeListener mockedProgressChangeListener = mock(MapRouteProgressChangeListener.class);
    NavigationMapRoute theNavigationMapRoute = new NavigationMapRoute(mockedNavigation, mockedMapView,
      mockedMapboxMap, mockedStyleRes, mockedBelowLayer, mockedMapClickListener, mockedDidFinishLoadingStyleListener,
      mockedProgressChangeListener);

    theNavigationMapRoute.onStop();

    verify(mockedNavigation, times(1))
      .removeProgressChangeListener(eq(mockedProgressChangeListener));
  }
}
