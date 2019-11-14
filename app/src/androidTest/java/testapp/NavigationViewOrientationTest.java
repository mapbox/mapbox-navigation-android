package testapp;

import android.content.res.Configuration;
import androidx.test.espresso.ViewAction;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;

import org.junit.Test;

import testapp.activity.BaseNavigationActivityTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertFalse;
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

    changeOrientation(orientationLandscape());
  }

  @Test
  public void onOrientationPortrait_navigationContinuesRunning() {
    if (checkOrientation(Configuration.ORIENTATION_PORTRAIT)) {
      return;
    }
    validateTestSetup();

    changeOrientation(orientationPortrait());
  }

  @Test
  public void onOrientationChange_recenterBtnStateIsRestore() {
    if (checkOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
      return;
    }
    validateTestSetup();

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

    onView(withId(R.id.navigationMapView)).perform(swipeUp());
    changeOrientation(orientationLandscape());

    NavigationMapboxMap navigationMapboxMap = getNavigationView().retrieveNavigationMapboxMap();
    boolean isWaynameVisible = navigationMapboxMap.isWaynameVisible();
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
