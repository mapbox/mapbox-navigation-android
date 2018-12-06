package com.mapbox.services.android.navigation.v5.navigation;

public class ThreadLocationUpdaterTest {

//  @Test
//  public void onConnected_engineRequestsUpdates() {
//    LocationEngine locationEngine = mock(LocationEngine.class);
//    LocationUpdater listener = buildListener(locationEngine);
//
//    listener.onConnected();
//
//    verify(locationEngine).requestLocationUpdates();
//  }
//
//  @Test
//  public void onConnected_nonNullLastLocationIsSent() {
//    LocationEngine locationEngine = mock(LocationEngine.class);
//    Location location = mock(Location.class);
//    when(locationEngine.getLastLocation()).thenReturn(location);
//    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
//    LocationUpdater listener = new LocationUpdater(
//      thread, locationEngine
//    );
//
//    listener.onConnected();
//
//    verify(thread).updateLocation(location);
//  }
//
//  @Test
//  public void onConnected_nullLastLocationIsIgnored() {
//    LocationEngine locationEngine = mock(LocationEngine.class);
//    when(locationEngine.getLastLocation()).thenReturn(null);
//    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
//    LocationUpdater listener = new LocationUpdater(
//      thread, locationEngine
//    );
//
//    listener.onConnected();
//
//    verifyZeroInteractions(thread);
//  }
//
//  @Test
//  public void queueValidLocationUpdate_threadReceivesUpdate() {
//    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
//    LocationUpdater listener = buildListener(thread);
//    Location location = mock(Location.class);
//
//    listener.onLocationChanged(location);
//
//    verify(thread).updateLocation(location);
//  }
//
//  private LocationUpdater buildListener(RouteProcessorBackgroundThread thread) {
//    return new LocationUpdater(thread, mock(LocationEngine.class));
//  }
//
//  private LocationUpdater buildListener(LocationEngine locationEngine) {
//    return new LocationUpdater(mock(RouteProcessorBackgroundThread.class), locationEngine);
//  }
}