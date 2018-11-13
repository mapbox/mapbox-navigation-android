package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class VoiceInstructionCacheTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void checksPreCachingCachesNineInstructions() throws Exception {
    MapboxNavigation aMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader aVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(aMapboxNavigation,
      aVoiceInstructionLoader);
    DirectionsRoute aRoute = buildDirectionsRoute();
    ArgumentCaptor<List> voiceInstructionsToCache = ArgumentCaptor.forClass(List.class);

    theVoiceInstructionCache.preCache(aRoute);

    verify(aVoiceInstructionLoader, times(1)).cacheInstructions(voiceInstructionsToCache.capture());
    assertEquals(9, voiceInstructionsToCache.getValue().size());
  }

  @Test
  public void checksCacheIsNotCalledIfIsVoiceInstructionsToCacheThresholdNotReached() {
    MapboxNavigation aMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader aVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(aMapboxNavigation,
      aVoiceInstructionLoader);

    theVoiceInstructionCache.cache();

    verifyZeroInteractions(aVoiceInstructionLoader);
  }

  @Test
  public void checksCaching() throws Exception {
    MapboxNavigation aMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader aVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(aMapboxNavigation,
      aVoiceInstructionLoader);
    DirectionsRoute twentyOneInstructionsRoute = buildDirectionsRoute();
    ArgumentCaptor<List> voiceInstructionsToCache = ArgumentCaptor.forClass(List.class);

    theVoiceInstructionCache.preCache(twentyOneInstructionsRoute);
    theVoiceInstructionCache.update(5);
    theVoiceInstructionCache.cache();
    theVoiceInstructionCache.update(10);
    theVoiceInstructionCache.cache();

    verify(aVoiceInstructionLoader, times(3)).cacheInstructions(voiceInstructionsToCache.capture());
    List<List> capturedVoiceInstructionsToCache = voiceInstructionsToCache.getAllValues();
    assertEquals(9, capturedVoiceInstructionsToCache.get(0).size());
    assertEquals(10, capturedVoiceInstructionsToCache.get(1).size());
    assertEquals(2, capturedVoiceInstructionsToCache.get(2).size());
  }

  @Test
  public void checksEvictVoiceInstructionsIsCalledWhenCaching() throws Exception {
    MapboxNavigation aMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader aVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(aMapboxNavigation,
      aVoiceInstructionLoader);
    DirectionsRoute aRoute = buildDirectionsRoute();

    theVoiceInstructionCache.preCache(aRoute);
    theVoiceInstructionCache.update(5);
    theVoiceInstructionCache.cache();

    verify(aVoiceInstructionLoader, times(1)).evictVoiceInstructions();
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = DirectionsResponse.fromJson(body);
    return response.routes().get(0);
  }

}