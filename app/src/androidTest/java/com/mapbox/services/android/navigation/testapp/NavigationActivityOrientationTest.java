package com.mapbox.services.android.navigation.testapp;

import android.support.test.espresso.ViewAction;

import com.mapbox.services.android.navigation.testapp.activity.BaseNavigationActivityTest;
import com.mapbox.services.android.navigation.testapp.activity.test.TestNavigationActivity;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static com.mapbox.services.android.navigation.testapp.action.OrientationChangeAction.orientationLandscape;
import static com.mapbox.services.android.navigation.testapp.action.OrientationChangeAction.orientationLandscapeReverse;
import static com.mapbox.services.android.navigation.testapp.action.OrientationChangeAction.orientationPortrait;
import static com.mapbox.services.android.navigation.testapp.action.OrientationChangeAction.orientationPortraitReverse;

public class NavigationActivityOrientationTest extends BaseNavigationActivityTest {

  @Override
  protected Class getActivityClass() {
    return TestNavigationActivity.class;
  }

  @Test
  public void testChangeDeviceOrientation() {
    changeOrientation(orientationLandscape(), 3000);
    changeOrientation(orientationLandscapeReverse(), 2500);
    changeOrientation(orientationPortraitReverse(), 5000);
    changeOrientation(orientationLandscape(), 500);
    changeOrientation(orientationLandscapeReverse(), 2000);
    changeOrientation(orientationLandscape(), 3000);
    changeOrientation(orientationPortraitReverse(), 5000);
    changeOrientation(orientationPortrait(), 5000);
    waitAction(2000);
  }

  private void changeOrientation(ViewAction newOrientation, long waitTime) {
    waitAction(waitTime);
    onView(isRoot()).perform(newOrientation);
  }
}
