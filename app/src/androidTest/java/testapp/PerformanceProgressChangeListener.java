package testapp;

import android.location.Location;
import android.support.test.espresso.idling.CountingIdlingResource;

import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState;

class PerformanceProgressChangeListener implements ProgressChangeListener {

  private final CountingIdlingResource performanceIdlingResource;
  private boolean isDecremented = false;

  PerformanceProgressChangeListener(CountingIdlingResource performanceIdlingResource) {
    this.performanceIdlingResource = performanceIdlingResource;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    RouteProgressState currentState = routeProgress.currentState();
    boolean hasArrived = currentState != null && currentState == RouteProgressState.ROUTE_ARRIVED;
    if (hasArrived && !isDecremented) {
      performanceIdlingResource.decrement();
      isDecremented = true;
    }
  }
}
