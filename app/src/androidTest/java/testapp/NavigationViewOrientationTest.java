package testapp;

import android.content.res.Configuration;
import android.support.test.espresso.ViewAction;

import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;

import org.junit.Test;

import testapp.activity.BaseNavigationActivityTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
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

  // TODO create test rule for this to conditionally ignore
  private boolean checkOrientation(int testedOrientation) {
    int orientation = getNavigationView().getContext().getResources().getConfiguration().orientation;
    return orientation == testedOrientation;
  }

  private void changeOrientation(ViewAction newOrientation) {
    onView(isRoot()).perform(newOrientation);
  }
}
