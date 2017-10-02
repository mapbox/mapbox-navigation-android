package com.mapbox.services.android.navigation.ui.v5;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResourceTimeoutException;
import android.support.test.espresso.UiController;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationMapRouteActivity;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.utils.OnMapReadyIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute.NavigationMapLayers.NAVIGATION_ROUTE_LAYER;
import static com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute.NavigationMapLayers.NAVIGATION_ROUTE_SHIELD_LAYER;
import static com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute.NavigationMapSources.NAVIGATION_ROUTE_SOURCE;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NavigationMapRouteTest {

  @Rule
  public ActivityTestRule<NavigationMapRouteActivity> rule = new ActivityTestRule<>(NavigationMapRouteActivity.class);

  private OnMapReadyIdlingResource idlingResource;
  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;

  @Before
  public void beforeTest() {
    try {
      Timber.e("@Before: register idle resource");
      idlingResource = new OnMapReadyIdlingResource(rule.getActivity());
      Espresso.registerIdlingResources(idlingResource);
      onView(withId(android.R.id.content)).check(matches(isDisplayed()));
      mapboxMap = idlingResource.getMapboxMap();
      navigationMapRoute = rule.getActivity().getNavigationMapRoute();
    } catch (IdlingResourceTimeoutException idlingResourceTimeoutException) {
      Timber.e("Idling resource timed out. Couldn't not validate if map is ready.");
      throw new RuntimeException("Could not start executeNavigationMapRouteTest for "
        + this.getClass().getSimpleName() + ".\n"
        + "The ViewHierarchy doesn't contain a view with resource id = R.id.mapView or \n"
        + "the Activity doesn't contain an instance variable with a name equal to mapboxMap.\n");
    }
  }

  @Test
  public void sanity() throws Exception {
    assertTrue(mapboxMap != null);
    assertTrue(navigationMapRoute != null);
  }

  @Test
  public void sourceAdded() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController uiController) {
        assertTrue(mapboxMap.getSource(NAVIGATION_ROUTE_SOURCE) != null);
      }
    });
  }

  @Test
  public void routeShieldLayerAdded() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController uiController) {
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_SHIELD_LAYER) != null);
      }
    });
  }

  @Test
  public void routeLayerAdded() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController uiController) {
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_LAYER) != null);
      }
    });
  }

  @Test
  public void routeLayerShieldVisible() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_SHIELD_LAYER)
          .getVisibility().getValue().equals(Property.VISIBLE));
      }
    });
  }

  @Test
  public void routeLayerVisible() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_LAYER)
          .getVisibility().getValue().equals(Property.VISIBLE));
      }
    });
  }

  @Test
  public void removeRouteHidesSheildLayer() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        navigationMapRoute.removeRoute();
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_SHIELD_LAYER)
          .getVisibility().getValue().equals(Property.NONE));
      }
    });
  }

  @Test
  public void removeRouteHidesLayer() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        navigationMapRoute.removeRoute();
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_LAYER)
          .getVisibility().getValue().equals(Property.NONE));
      }
    });
  }

  @Test
  public void reattachRouteSheildLayerWithLoadNewStyle() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        mapboxMap.setStyleUrl(Style.DARK);
        controller.loopMainThreadForAtLeast(500);
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_SHIELD_LAYER) != null);
      }
    });
  }

  @Test
  public void reattachRouteLayerWithLoadNewStyle() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        mapboxMap.setStyleUrl(Style.DARK);
        controller.loopMainThreadForAtLeast(500);
        assertTrue(mapboxMap.getLayer(NAVIGATION_ROUTE_LAYER) != null);
      }
    });
  }

  @Test
  public void reattachRouteSourceWithLoadNewStyle() throws Exception {
    executeNavigationMapRoute(new NavigationMapRouteAction.OnPerformNavigationMapRouteAction() {
      @Override
      public void onNavigationMapRouteAction(NavigationMapRoute navigationMapRoute, MapboxMap mapboxMap,
                                             UiController controller) {
        mapboxMap.setStyleUrl(Style.DARK);
        controller.loopMainThreadForAtLeast(500);
        assertTrue(mapboxMap.getSource(NAVIGATION_ROUTE_SOURCE) != null);
      }
    });
  }

  @After
  public void afterTest() {
    Timber.e("@After: unregister idle resource");
    Espresso.unregisterIdlingResources(idlingResource);
  }

  public void executeNavigationMapRoute(NavigationMapRouteAction.OnPerformNavigationMapRouteAction listener) {
    onView(withId(android.R.id.content)).perform(new NavigationMapRouteAction(mapboxMap, navigationMapRoute, listener));
  }
}
