package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

class NetworkProgressInterceptor implements Interceptor {

  private final NetworkProgressListener listener;

  NetworkProgressInterceptor(NetworkProgressListener listener) {
    this.listener = listener;
  }

  @Override
  public Response intercept(@NonNull Chain chain) throws IOException {
    Response originalResponse = chain.proceed(chain.request());
    return originalResponse.newBuilder()
      .body(new NetworkResponseBody(originalResponse.body(), listener))
      .build();
  }
}
