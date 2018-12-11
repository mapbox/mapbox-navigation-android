package testapp;

import android.content.res.Configuration;
import android.support.test.espresso.ViewAction;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;

import org.junit.Test;

import testapp.activity.BaseNavigationActivityTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertFalse;
import static testapp.action.NavigationViewAction.invoke;
import static testapp.action.OrientationChangeAction.orientationLandscape;
import static testapp.action.OrientationChangeAction.orientationPortrait;

public class NavigationViewOrientationTest extends BaseNavigationActivityTest {

  @Override
  protected Class getActivityClass() {
    return TestNavigationActivity.class;
  }

  @Test
  public void onOrientationLandscape_navigationContinuesRunning() {
    if (checkOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
      return;
    }
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });

    changeOrientation(orientationLandscape());
  }

  @Test
  public void onOrientationPortrait_navigationContinuesRunning() {
    if (checkOrientation(Configuration.ORIENTATION_PORTRAIT)) {
      return;
    }
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });

    changeOrientation(orientationPortrait());
  }

  @Test
  public void onOrientationChange_recenterBtnStateIsRestore() {
    if (checkOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
      return;
    }
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });

    onView(withId(R.id.routeOverviewBtn)).perform(click());
    changeOrientation(orientationLandscape());
    onView(withId(R.id.recenterBtn)).check(matches(isDisplayed()));
  }

  @Test
  public void onOrientationChange_cameraTrackingIsRestore() {
    if (checkOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
      return;
    }
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });

    onView(withId(R.id.navigationMapView)).perform(swipeUp());
    changeOrientation(orientationLandscape());

    NavigationMapboxMap navigationMapboxMap = getNavigationView().retrieveNavigationMapboxMap();
    boolean isTrackingEnabled = navigationMapboxMap.retrieveCamera().isTrackingEnabled();
    assertFalse(isTrackingEnabled);
  }

  @Test
  public void onOrientationChange_waynameVisibilityIsRestored() {
    if (checkOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
      return;
    }
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });

    onView(withId(R.id.navigationMapView)).perform(swipeUp());
    changeOrientation(orientationLandscape());

    boolean isWaynameVisible = getNavigationView().isWayNameVisible();
    assertFalse(isWaynameVisible);
  }

  // TODO create test rule for this to conditionally ignore
  private boolean checkOrientation(int testedOrientation) {
    int orientation = getNavigationView().getContext().getResources().getConfiguration().orientation;
    return orientation == testedOrientation;
  }

  private void changeOrientation(ViewAction newOrientation) {
    onView(isRoot()).perform(newOrientation);
  }
}
