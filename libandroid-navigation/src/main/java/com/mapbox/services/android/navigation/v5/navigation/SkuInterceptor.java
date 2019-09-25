package com.mapbox.services.android.navigation.v5.navigation;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

class SkuInterceptor implements Interceptor {

  private static final String SKU_KEY = "sku";
  private final AccountsManagerImpl accountsManager;

  SkuInterceptor(AccountsManagerImpl accountsManager) {
    this.accountsManager = accountsManager;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    String skuToken = accountsManager.obtainSku();
    HttpUrl url = request.url().newBuilder().addQueryParameter(SKU_KEY, skuToken).build();
    request = request.newBuilder().url(url).build();
    return chain.proceed(request);
  }
}
