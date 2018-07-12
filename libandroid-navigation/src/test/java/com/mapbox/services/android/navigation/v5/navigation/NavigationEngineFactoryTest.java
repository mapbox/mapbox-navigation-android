package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

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
  public void clearEngines_cameraEngineIsRemoved() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.clearEngines();

    assertNull(provider.retrieveCameraEngine());
  }

  @Test
  public void clearEngines_offRouteEngineIsRemoved() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.clearEngines();

    assertNull(provider.retrieveOffRouteEngine());
  }

  @Test
  public void clearEngines_snapEngineIsRemoved() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.clearEngines();

    assertNull(provider.retrieveSnapEngine());
  }

  @Test
  public void clearEngines_fasterRouteEngineIsRemoved() {
    NavigationEngineFactory provider = new NavigationEngineFactory();

    provider.clearEngines();

    assertNull(provider.retrieveFasterRouteEngine());
  }
}