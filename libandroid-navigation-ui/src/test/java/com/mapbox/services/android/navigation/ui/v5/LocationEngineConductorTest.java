package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductor;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductorListener;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationEngineConductorTest extends BaseTest {

  @Test
  public void sanity() throws Exception {
    LocationEngineConductorListener mockCallback = mock(LocationEngineConductorListener.class);
    LocationEngineConductor locationEngineConductor = new LocationEngineConductor(mockCallback);

    assertNotNull(locationEngineConductor);
  }

  @Test
  public void onInitWithSimulation_mockLocationEngineIsActivated() throws Exception {
    LocationEngineConductorListener mockCallback = mock(LocationEngineConductorListener.class);
    LocationEngineConductor locationEngineConductor = new LocationEngineConductor(mockCallback);
    boolean simulateRouteEnabled = true;
    locationEngineConductor.initializeLocationEngine(createMockContext(), simulateRouteEnabled);

    LocationEngine locationEngine = locationEngineConductor.obtainLocationEngine();

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
