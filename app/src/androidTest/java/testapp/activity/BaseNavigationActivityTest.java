package testapp.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResourceTimeoutException;
import android.support.test.rule.ActivityTestRule;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.io.IOException;
import java.io.InputStream;

import testapp.utils.OnNavigationReadyIdlingResource;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public abstract class BaseNavigationActivityTest {

  @Rule
  public ActivityTestRule<Activity> rule = new ActivityTestRule<>(getActivityClass());

  private NavigationView navigationView;
  private AssetManager assetManager;
  private OnNavigationReadyIdlingResource idlingResource;

  @Before
  public void beforeTest() {
    try {
      Activity activity = rule.getActivity();
      idlingResource = new OnNavigationReadyIdlingResource(activity);
      IdlingRegistry.getInstance().register(idlingResource);
      checkViewIsDisplayed(R.id.navigationView);
      navigationView = idlingResource.getNavigationView();
      assetManager = activity.getAssets();
    } catch (IdlingResourceTimeoutException idlingResourceTimeoutException) {
      Timber.e("Idling resource timed out. Could not validate if navigation is ready.");
      throw new RuntimeException("Could not start test for " + getActivityClass().getSimpleName() + ".\n"
        + "The ViewHierarchy doesn't contain a view with resource id = R.id.navigationView");
    }
  }

  @After
  public void afterTest() {
    IdlingRegistry.getInstance().unregister(idlingResource);
  }

  protected void validateTestSetup() {
    Assert.assertTrue("Device is not connected to the Internet.", isConnected(rule.getActivity()));
    checkViewIsDisplayed(R.id.navigationView);
  }

  protected NavigationView getNavigationView() {
    return navigationView;
  }

  protected abstract Class getActivityClass();

  protected String loadJsonFromAsset(String filename) {
    try {
      InputStream is = assetManager.open(filename);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      return new String(buffer, "UTF-8");

    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private void checkViewIsDisplayed(int id) {
    onView(withId(id)).check(matches(isDisplayed()));
  }

  private boolean isConnected(Context context) {
    ConnectivityManager connectivityManager
      = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}