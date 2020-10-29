package com.mapbox.navigation.ui.voice;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.speech.v1.MapboxSpeech;
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider;
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Callback;
import timber.log.Timber;

public class VoiceInstructionLoader {
  private static final int VOICE_INSTRUCTIONS_TO_EVICT_THRESHOLD = 4;
  private static final String SSML_TEXT_TYPE = "ssml";
  private final ConnectivityStatusProvider connectivityStatus;
  private final String accessToken;
  private List<String> urlsCached;
  private final Cache cache;
  private final Context context;
  @Nullable
  private MapboxSpeech.Builder mapboxSpeechBuilder = null;
  private UrlSkuTokenProvider urlSkuTokenProvider;
  private String baseUrl;

  public VoiceInstructionLoader(@NonNull Context context, String accessToken, Cache cache) {
    this(context, accessToken, cache, null);
  }

  public VoiceInstructionLoader(@NonNull Context context, String accessToken, Cache cache, @Nullable String baseUrl) {
    this.connectivityStatus = new ConnectivityStatusProvider(context);
    this.accessToken = accessToken;
    this.context = context;
    this.urlsCached = new ArrayList<>();
    this.cache = cache;
    this.urlSkuTokenProvider = MapboxNavigationAccounts.getInstance(context.getApplicationContext());
    this.baseUrl = baseUrl;
  }

  // Package private (no modifier) for testing purposes
  VoiceInstructionLoader(Context context, String accessToken, Cache cache, MapboxSpeech.Builder mapboxSpeechBuilder,
                         ConnectivityStatusProvider connectivityStatus) {
    this.accessToken = accessToken;
    this.context = context;
    this.urlsCached = new ArrayList<>();
    this.cache = cache;
    this.mapboxSpeechBuilder = mapboxSpeechBuilder;
    this.connectivityStatus = connectivityStatus;
  }

  @NonNull
  public List<String> evictVoiceInstructions() {
    List<String> urlsToRemove = new ArrayList<>();
    for (int i = 0; i < urlsCached.size() && i < VOICE_INSTRUCTIONS_TO_EVICT_THRESHOLD; i++) {
      String urlToRemove = urlsCached.get(i);
      try {
        Iterator<String> urlsCurrentlyCached = cache.urls();
        for (Iterator<String> urlCached = urlsCurrentlyCached; urlCached.hasNext(); ) {
          String url = urlCached.next();
          if (url.equals(urlToRemove)) {
            urlCached.remove();
            urlsToRemove.add(urlToRemove);
            break;
          }
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    urlsCached.removeAll(urlsToRemove);
    return urlsToRemove;
  }

  public void cacheInstructions(@NonNull List<String> instructions) {
    for (String instruction : instructions) {
      cacheInstruction(instruction);
    }
  }

  // Package private (no modifier) for testing purposes
  void addStubUrlsToCache(List<String> urlsToCache) {
    this.urlsCached = urlsToCache;
  }

  void setupMapboxSpeechBuilder(String language) {
    if (mapboxSpeechBuilder == null) {
      mapboxSpeechBuilder = MapboxSpeech.builder()
        .accessToken(accessToken)
        .language(language)
        .cache(cache)
        .interceptor(provideOfflineCacheInterceptor());

      if (baseUrl != null) {
        mapboxSpeechBuilder.baseUrl(baseUrl);
      }
    }
  }

  void requestInstruction(@NonNull String instruction, String textType, Callback<ResponseBody> callback) {
    if (context != null && !cache.isClosed() && mapboxSpeechBuilder != null) {
      MapboxSpeech mapboxSpeech = mapboxSpeechBuilder
        .instruction(instruction)
        .textType(textType)
        .build();
      mapboxSpeech.enqueueCall(callback);
    }
  }

  boolean hasCache() {
    return !urlsCached.isEmpty();
  }

  void flushCache() {
    try {
      cache.evictAll();
    } catch (IOException exception) {
      Timber.e(exception);
    }
  }

  void addCachedUrl(String url) {
    urlsCached.add(url);
  }

  private void cacheInstruction(@NonNull String instruction) {
    if (!TextUtils.isEmpty(instruction)) {
      requestInstruction(instruction, SSML_TEXT_TYPE, new InstructionCacheCallback(this));
    }
  }

  @NonNull
  private Interceptor provideOfflineCacheInterceptor() {
    return new Interceptor() {
      @NotNull
      @Override
      public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();

        HttpUrl httpUrl = chain.request().url();
        String skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toString(), httpUrl.querySize());
        Request.Builder newBuilder = request.newBuilder();
        newBuilder.url(skuUrl);

        if (!connectivityStatus.isConnected()) {
          CacheControl cacheControl = new CacheControl.Builder()
            .maxStale(3, TimeUnit.DAYS)
            .build();
          newBuilder.cacheControl(cacheControl);
        }

        return chain.proceed(newBuilder.build());
      }
    };
  }
}
