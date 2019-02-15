package com.mapbox.services.android.navigation.v5.navigation;

interface NetworkProgressListener {
  void update(long bytesRead, long contentLength, boolean isDone);
}