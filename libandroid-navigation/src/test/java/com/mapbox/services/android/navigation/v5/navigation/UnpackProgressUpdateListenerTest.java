package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UnpackProgressUpdateListenerTest {

  @Test
  public void onProgressUpdate_downloadListenerIsTriggered() {
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    Long progress = 58L;
    UnpackProgressUpdateListener progressUpdateListener = new UnpackProgressUpdateListener(listener);

    progressUpdateListener.onProgressUpdate(progress);

    verify(listener).onProgressUpdate(progress.intValue());
  }

  @Test
  public void onCompletion_downloadListenerIsTriggered() {
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    UnpackProgressUpdateListener progressUpdateListener = new UnpackProgressUpdateListener(listener);

    progressUpdateListener.onCompletion();

    verify(listener).onCompletion();
  }
}