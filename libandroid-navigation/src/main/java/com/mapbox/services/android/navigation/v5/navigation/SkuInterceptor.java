package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

class SkuInterceptor implements Interceptor {

  private static final String SKU_KEY = "sku";
  private final Context context;

  SkuInterceptor(Context context) {
    this.context = context;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    String skuToken = AccountsManagerImpl.getInstance(context).obtainSku();
    HttpUrl url = request.url().newBuilder().addQueryParameter(SKU_KEY, skuToken).build();
    request = request.newBuilder().url(url).build();
    return chain.proceed(request);
  }
}
