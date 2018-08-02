package testapp;

import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;

import testapp.activity.BaseNavigationActivityTest;

import static junit.framework.Assert.assertNotNull;

public class NavigationViewTest extends BaseNavigationActivityTest {

  @Override
  protected Class getActivityClass() {
    return TestNavigationActivity.class;
  }

  @Test
  public void onInitialization_navigationMapboxMapIsNotNull() {
    validateTestSetup();

    NavigationMapboxMap navigationMapboxMap = getNavigationView().retrieveNavigationMapboxMap();

    assertNotNull(navigationMapboxMap);
  }

  @Test
  public void onNavigationStart_mapboxNavigationIsNotNull() {
    validateTestSetup();

    MapboxNavigation mapboxNavigation = getNavigationView().retrieveMapboxNavigation();

    assertNotNull(mapboxNavigation);
  }
}
