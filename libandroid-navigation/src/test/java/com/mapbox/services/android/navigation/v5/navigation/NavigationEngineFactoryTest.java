package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class NavigationEngineFactoryTest {

  @Test
  public void onInitialization_defaultCameraEngineIsCreated() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    assertNotNull(provider.retrieveCameraEngine());
  }

  @Test
  public void onInitialization_defaultOffRouteEngineIsCreated() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    assertNotNull(provider.retrieveOffRouteEngine());
  }

  @Test
  public void onInitialization_defaultSnapEngineIsCreated() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    assertNotNull(provider.retrieveSnapEngine());
  }

  @Test
  public void onInitialization_defaultFasterRouteEngineIsCreated() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    assertNotNull(provider.retrieveFasterRouteEngine());
  }

  @Test
  public void updateFasterRouteEngine_ignoresNull() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.updateFasterRouteEngine(null);

    assertNotNull(provider.retrieveFasterRouteEngine());
  }

  @Test
  public void updateOffRouteEngine_ignoresNull() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.updateOffRouteEngine(null);

    assertNotNull(provider.retrieveOffRouteEngine());
  }

  @Test
  public void updateCameraEngine_ignoresNull() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.updateCameraEngine(null);

    assertNotNull(provider.retrieveCameraEngine());
  }

  @Test
  public void updateSnapEngine_ignoresNull() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.updateSnapEngine(null);

    assertNotNull(provider.retrieveSnapEngine());
  }
}