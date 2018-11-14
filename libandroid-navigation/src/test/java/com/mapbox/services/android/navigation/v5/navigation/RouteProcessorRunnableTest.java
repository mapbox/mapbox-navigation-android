package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class RouteProcessorRunnableTest {

  @Test
  public void onRun_buildNewRouteProgressReceivesStatusAndRoute() {
    NavigationRouteProcessor processor = mock(NavigationRouteProcessor.class);
    NavigationStatus status = buildMockStatus();
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteProcessorRunnable runnable = buildRouteProcessorRunnableWith(processor, status, route);
    runnable.updateRawLocation(mock(Location.class));

    runnable.run();

    verify(processor).buildNewRouteProgress(status, route);
  }

  @Test
  public void onRun_previousRouteProgressIsUpdated() {
    NavigationRouteProcessor processor = mock(NavigationRouteProcessor.class);
    NavigationStatus status = buildMockStatus();
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteProgress progress = mock(RouteProgress.class);
    when(processor.buildNewRouteProgress(status, route)).thenReturn(progress);
    RouteProcessorRunnable runnable = buildRouteProcessorRunnableWith(processor, status, route);
    runnable.updateRawLocation(mock(Location.class));

    runnable.run();

    verify(processor).updatePreviousRouteProgress(progress);
  }

  @Test
  public void onRun_offRouteDetectorReceivesStatus() {
    OffRouteDetector detector = mock(OffRouteDetector.class);
    NavigationEngineFactory factory = buildMockFactory(detector);
    when(factory.retrieveOffRouteEngine()).thenReturn(detector);
    NavigationStatus status = buildMockStatus();
    RouteProcessorRunnable runnable = buildRouteProcessorRunnableWith(factory, status);
    runnable.updateRawLocation(mock(Location.class));

    runnable.run();

    verify(detector).isUserOffRouteWith(status);
  }

  @Test
  public void onRun_snapToRouteReceivesStatus() {
    SnapToRoute snapToRoute = mock(SnapToRoute.class);
    NavigationEngineFactory factory = buildMockFactory(snapToRoute);
    NavigationStatus status = buildMockStatus();
    RouteProcessorRunnable runnable = buildRouteProcessorRunnableWith(factory, status);
    Location rawLocation = mock(Location.class);
    runnable.updateRawLocation(rawLocation);

    runnable.run();

    verify(snapToRoute).getSnappedLocationWith(status, rawLocation);
  }

  private RouteProcessorRunnable buildRouteProcessorRunnableWith(NavigationRouteProcessor processor,
                                                                 NavigationStatus status, DirectionsRoute route) {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    MapboxNavigator navigator = mock(MapboxNavigator.class);
    when(navigator.retrieveStatus(any(Date.class), any(Long.class))).thenReturn(status);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigation.options()).thenReturn(options);
    when(navigation.getRoute()).thenReturn(route);
    when(navigation.retrieveMapboxNavigator()).thenReturn(navigator);
    when(navigation.retrieveEngineFactory()).thenReturn(new NavigationEngineFactory());
    return new RouteProcessorRunnable(
      processor,
      navigation,
      mock(Handler.class),
      mock(Handler.class),
      mock(RouteProcessorBackgroundThread.Listener.class)
    );
  }

  private RouteProcessorRunnable buildRouteProcessorRunnableWith(NavigationEngineFactory factory,
                                                                 NavigationStatus status) {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    MapboxNavigator navigator = mock(MapboxNavigator.class);
    when(navigator.retrieveStatus(any(Date.class), any(Long.class))).thenReturn(status);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigation.options()).thenReturn(options);
    when(navigation.getRoute()).thenReturn(mock(DirectionsRoute.class));
    when(navigation.retrieveMapboxNavigator()).thenReturn(navigator);
    when(navigation.retrieveEngineFactory()).thenReturn(factory);
    return new RouteProcessorRunnable(
      mock(NavigationRouteProcessor.class),
      navigation,
      mock(Handler.class),
      mock(Handler.class),
      mock(RouteProcessorBackgroundThread.Listener.class)
    );
  }

  private NavigationEngineFactory buildMockFactory(SnapToRoute snapToRoute) {
    NavigationEngineFactory factory = mock(NavigationEngineFactory.class);
    when(factory.retrieveSnapEngine()).thenReturn(snapToRoute);
    when(factory.retrieveOffRouteEngine()).thenReturn(mock(OffRouteDetector.class));
    when(factory.retrieveFasterRouteEngine()).thenReturn(mock(FasterRouteDetector.class));
    when(factory.retrieveCameraEngine()).thenReturn(mock(SimpleCamera.class));
    return factory;
  }

  private NavigationEngineFactory buildMockFactory(OffRouteDetector detector) {
    NavigationEngineFactory factory = mock(NavigationEngineFactory.class);
    when(factory.retrieveSnapEngine()).thenReturn(mock(SnapToRoute.class));
    when(factory.retrieveOffRouteEngine()).thenReturn(detector);
    when(factory.retrieveFasterRouteEngine()).thenReturn(mock(FasterRouteDetector.class));
    when(factory.retrieveCameraEngine()).thenReturn(mock(SimpleCamera.class));
    return factory;
  }

  private NavigationStatus buildMockStatus() {
    NavigationStatus status = mock(NavigationStatus.class);
    Point location = Point.fromLngLat(0.0, 0.0);
    when(status.getLocation()).thenReturn(location);
    when(status.getTime()).thenReturn(new Date());
    when(status.getBearing()).thenReturn(0.0f);
    return status;
  }
}
