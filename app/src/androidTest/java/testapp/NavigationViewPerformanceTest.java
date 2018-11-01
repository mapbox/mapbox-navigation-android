package testapp;

import android.Manifest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.rule.GrantPermissionRule;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.testapp.test.TestNavigationViewActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;

import org.junit.Rule;
import org.junit.Test;

import testapp.activity.BaseNavigationViewActivityTest;
import testapp.rules.BatteryStatsDumpsysRule;
import testapp.rules.CpuInfoDumpsysRule;
import testapp.rules.GraphicsDumpsysRule;
import testapp.rules.MemoryInfoDumpsysRule;
import testapp.rules.TraceRule;

import static testapp.action.NavigationViewAction.invoke;

public class NavigationViewPerformanceTest extends BaseNavigationViewActivityTest {

  private static final String PERFORMANCE_IDLING_RESOURCE = "performance_idling_resource";
  private static final int EIGHTY_KM_PER_HOUR = 80;

  private final CountingIdlingResource performanceIdlingResource =
    new CountingIdlingResource(PERFORMANCE_IDLING_RESOURCE);

  @Rule
  public TraceRule traceRule = new TraceRule();

  @Rule
  public BatteryStatsDumpsysRule batteryRule = new BatteryStatsDumpsysRule();

  @Rule
  public GraphicsDumpsysRule graphicsRule = new GraphicsDumpsysRule();

  @Rule
  public CpuInfoDumpsysRule cpuInfoRule = new CpuInfoDumpsysRule();

  @Rule
  public MemoryInfoDumpsysRule memoryInfoRule = new MemoryInfoDumpsysRule();

  @Rule
  public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Override
  protected Class getActivityClass() {
    return TestNavigationViewActivity.class;
  }

  @Override
  public void beforeTest() {
    super.beforeTest();
    IdlingRegistry.getInstance().register(performanceIdlingResource);
  }

  @Test
  public void lancaster() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Test
  public void dca_Arboretum_Tunnels() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("DCA-Arboretum-Tunnels-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Test
  public void pipefitters_FourSeasonsBoston_TunnelExit() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("Pipefitters-FourSeasonsBoston-TunnelExit-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Test
  public void unionStation_Ikea() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("UnionStation-Ikea-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Test
  public void truck_Route() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("truck-route-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Test
  public void stPetersburg_Orlando() {
    validateTestSetup();

    invoke(getNavigationView(), (uiController, navigationView) -> {
      DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("StPetersburg-Orlando-1.json"));
      LocationEngine replayEngine = buildReplayEngineFrom(testRoute);
      NavigationViewOptions options = NavigationViewOptions.builder()
        .progressChangeListener(new PerformanceProgressChangeListener(performanceIdlingResource))
        .locationEngine(replayEngine)
        .directionsRoute(testRoute)
        .build();

      navigationView.startNavigation(options);
      performanceIdlingResource.increment();
    });

    Espresso.onIdle();
  }

  @Override
  public void afterTest() {
    super.afterTest();
    IdlingRegistry.getInstance().unregister(performanceIdlingResource);
  }

  private LocationEngine buildReplayEngineFrom(DirectionsRoute testRoute) {
    ReplayRouteLocationEngine replayRouteLocationEngine = new ReplayRouteLocationEngine();
    replayRouteLocationEngine.updateSpeed(EIGHTY_KM_PER_HOUR);
    replayRouteLocationEngine.assign(testRoute);
    return replayRouteLocationEngine;
  }
}
