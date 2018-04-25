package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.ui.v5.location.NavigationLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.location.NavigationLocationEngineListener;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationLocationEngineTest extends BaseTest {

  @Test
  public void sanity() throws Exception {
    NavigationLocationEngineListener mockCallback = mock(NavigationLocationEngineListener.class);
    NavigationLocationEngine navigationLocationEngine = new NavigationLocationEngine(mockCallback);

    assertNotNull(navigationLocationEngine);
  }

  @Test
  public void onInitWithSimulation_mockLocationEngineIsActivated() throws Exception {
    NavigationLocationEngineListener mockCallback = mock(NavigationLocationEngineListener.class);
    NavigationLocationEngine navigationLocationEngine = new NavigationLocationEngine(mockCallback);

    navigationLocationEngine.initializeLocationEngine(createMockContext(), true);
    LocationEngine locationEngine = navigationLocationEngine.obtainLocationEngine();

    assertTrue(locationEngine instanceof MockLocationEngine);
  }

  @NonNull
  private Context createMockContext() {
    Context mockContext = mock(Context.class);
    LocationManager mockLocationManager = mock(LocationManager.class);
    when(mockContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager);
    when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));
    when(mockContext.getApplicationContext()).thenReturn(mock(Context.class));
    return mockContext;
  }
}
