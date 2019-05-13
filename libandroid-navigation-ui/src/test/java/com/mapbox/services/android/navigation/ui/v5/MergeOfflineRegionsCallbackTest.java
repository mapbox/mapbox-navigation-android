package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MergeOfflineRegionsCallbackTest {

  @Test
  public void checksOfflineDatabaseLoadedOnCompleteCallbackIsCalledWhenOnMerge() {
    OfflineDatabaseLoadedCallback mockedOfflineDatabaseLoadedCallback = mock(OfflineDatabaseLoadedCallback.class);
    MergeOfflineRegionsCallback theMergeOfflineRegionsCallback =
      new MergeOfflineRegionsCallback(mockedOfflineDatabaseLoadedCallback);
    OfflineRegion[] anyOfflineRegion = new OfflineRegion[0];

    theMergeOfflineRegionsCallback.onMerge(anyOfflineRegion);

    verify(mockedOfflineDatabaseLoadedCallback).onComplete();
  }

  @Test
  public void checksOfflineDatabaseLoadedOnErrorCallbackIsCalledWhenOnError() {
    OfflineDatabaseLoadedCallback mockedOfflineDatabaseLoadedCallback = mock(OfflineDatabaseLoadedCallback.class);
    MergeOfflineRegionsCallback theMergeOfflineRegionsCallback =
      new MergeOfflineRegionsCallback(mockedOfflineDatabaseLoadedCallback);
    String anyError = "any error message";

    theMergeOfflineRegionsCallback.onError(anyError);

    verify(mockedOfflineDatabaseLoadedCallback).onError(eq(anyError));
  }

  @Test
  public void checksOfflineDatabaseLoadedOnDestroyReleasesCallback() {
    OfflineDatabaseLoadedCallback mockedOfflineDatabaseLoadedCallback = mock(OfflineDatabaseLoadedCallback.class);
    MergeOfflineRegionsCallback theMergeOfflineRegionsCallback =
      new MergeOfflineRegionsCallback(mockedOfflineDatabaseLoadedCallback);

    OfflineDatabaseLoadedCallback destroyedCallback = theMergeOfflineRegionsCallback.onDestroy();

    assertNull(destroyedCallback);
  }
}