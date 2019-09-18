package testapp;

import android.support.test.espresso.contrib.RecyclerViewActions;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;

import testapp.activity.BaseNavigationActivityTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertNotNull;
import static testapp.action.NavigationViewAction.invoke;

public class NavigationViewTest extends BaseNavigationActivityTest {

  @Override
  protected Class getActivityClass() {
    return TestNavigationActivity.class;
  }

  @Test
  public void onInitialization_navigationMapboxMapIsNotNull() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });
    NavigationMapboxMap navigationMapboxMap = getNavigationView().retrieveNavigationMapboxMap();

    assertNotNull(navigationMapboxMap);
  }

  @Test
  public void onNavigationStart_mapboxNavigationIsNotNull() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
    });
    MapboxNavigation mapboxNavigation = getNavigationView().retrieveMapboxNavigation();

    assertNotNull(mapboxNavigation);
  }

  @Test
  public void onNavigationStart_mapboxSendFeedback() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      navigationView.retrieveFeedbackButton().show();
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      NavigationViewOptions options = NavigationViewOptions.builder()
              .directionsRoute(testRoute)
              .build();

      navigationView.startNavigation(options);
    });
    onView(withId(R.id.feedbackFab)).perform(click());
    onView(withId(R.id.feedbackItems)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
  }
}
