package com.mapbox.navigation.ui;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked assignment")
public class VoiceInstructionCacheTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void checksPreCachingCachesNineInstructions() throws Exception {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader mockedVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider mockedConnectivityStatus = mock(ConnectivityStatusProvider.class);
    when(mockedConnectivityStatus.isConnected()).thenReturn(true);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(mockedMapboxNavigation,
      mockedVoiceInstructionLoader, mockedConnectivityStatus);
    DirectionsRoute twentyOneInstructionsRoute = buildDirectionsRoute();
    ArgumentCaptor<List> voiceInstructionsToCache = ArgumentCaptor.forClass(List.class);

    theVoiceInstructionCache.initCache(twentyOneInstructionsRoute);

    verify(mockedVoiceInstructionLoader, times(0))
      .cacheInstructions(voiceInstructionsToCache.capture());

    assertEquals(21, theVoiceInstructionCache.getTotalVoiceInstructionNumber());
  }

  @Test
  public void checksCacheIsNotCalledIfIsVoiceInstructionsToCacheThresholdNotReached() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader mockedVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider mockedConnectivityStatus = mock(ConnectivityStatusProvider.class);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(mockedMapboxNavigation,
      mockedVoiceInstructionLoader, mockedConnectivityStatus);

    theVoiceInstructionCache.cache();

    verifyZeroInteractions(mockedVoiceInstructionLoader);
  }

  @Test
  public void checksCaching() throws Exception {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader mockedVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider mockedConnectivityStatus = mock(ConnectivityStatusProvider.class);
    when(mockedConnectivityStatus.isConnected()).thenReturn(true);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(mockedMapboxNavigation,
      mockedVoiceInstructionLoader, mockedConnectivityStatus);
    DirectionsRoute twentyOneInstructionsRoute = buildDirectionsRoute();

    theVoiceInstructionCache.initCache(twentyOneInstructionsRoute);
    theVoiceInstructionCache.update(5);
    theVoiceInstructionCache.cache();
    theVoiceInstructionCache.update(10);
    theVoiceInstructionCache.cache();
    theVoiceInstructionCache.update(15);
    theVoiceInstructionCache.cache();

    ArgumentCaptor<List> voiceInstructionsToCache = ArgumentCaptor.forClass(List.class);
    verify(mockedVoiceInstructionLoader, times(3))
      .cacheInstructions(voiceInstructionsToCache.capture());
    List<List> capturedVoiceInstructionsToCache = voiceInstructionsToCache.getAllValues();
    assertEquals(9, capturedVoiceInstructionsToCache.get(0).size());
    assertEquals(10, capturedVoiceInstructionsToCache.get(1).size());
    assertEquals(2, capturedVoiceInstructionsToCache.get(2).size());
  }

  @Test
  public void checksEvictVoiceInstructionsIsCalledWhenCaching() throws Exception {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader mockedVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider mockedConnectivityStatus = mock(ConnectivityStatusProvider.class);
    when(mockedConnectivityStatus.isConnected()).thenReturn(true);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(mockedMapboxNavigation,
      mockedVoiceInstructionLoader, mockedConnectivityStatus);
    DirectionsRoute theDirectionsRoute = buildDirectionsRoute();

    theVoiceInstructionCache.initCache(theDirectionsRoute);
    theVoiceInstructionCache.update(5);
    theVoiceInstructionCache.cache();

    verify(mockedVoiceInstructionLoader, times(1)).evictVoiceInstructions();
  }

  @Test
  public void noConnectivityDoesNotAllowCaching() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    VoiceInstructionLoader mockedVoiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider mockedConnectivityStatus = mock(ConnectivityStatusProvider.class);
    when(mockedConnectivityStatus.isConnected()).thenReturn(false);
    VoiceInstructionCache theVoiceInstructionCache = new VoiceInstructionCache(mockedMapboxNavigation,
      mockedVoiceInstructionLoader, mockedConnectivityStatus);

    theVoiceInstructionCache.cache();

    verifyZeroInteractions(mockedVoiceInstructionLoader);
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = DirectionsResponse.fromJson(body);
    return response.routes().get(0);
  }

}