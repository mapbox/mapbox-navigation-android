package com.mapbox.services.android.navigation.v5.navigation;

import android.util.Log;

import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Mapbox specific services used internally within the SDK. Subclasses must implement baseUrl and
 * initializeCall.
 *
 * @param <T> Type parameter for response.
 * @param <S> Type parameter for service interface.
 * @since 1.0.0
 */
public abstract class MapboxService<T, S> {

  private final Class<S> serviceType;
  private boolean enableDebug;
  protected OkHttpClient okHttpClient;
  private okhttp3.Call.Factory callFactory;
  private Retrofit retrofit;
  private Call<T> call;
  private S service;

  /**
   * Constructor for creating a new MapboxService setting the service type for use when
   * initializing retrofit. Subclasses should pass their service class to this constructor.
   *
   * @param serviceType for initializing retrofit
   * @since 3.0.0
   */
  public MapboxService(Class<S> serviceType) {
    this.serviceType = serviceType;
  }

  /**
   * Should return base url for retrofit calls.
   *
   * @return baseUrl as a string
   * @since 3.0.0
   */
  protected abstract String baseUrl();

  /**
   * Abstract method for getting Retrofit {@link Call} from the subclass. Subclasses should override
   * this method and construct and return the call.
   *
   * @return call
   * @since 3.0.0
   */
  protected abstract Call<T> initializeCall();

  /**
   * Get call if already created, otherwise get it from subclass implementation
   *
   * @return call
   * @since 3.0.0
   */
  protected Call<T> getCall() {
    Log.d("MapboxService", "hit count: " + okHttpClient.cache().hitCount());
    Log.d("MapboxService", "network count: " + okHttpClient.cache().networkCount());
    Log.d("MapboxService", "request count: " + okHttpClient.cache().requestCount());
    Log.d("MapboxService", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ");

    if (call == null) {
      call = initializeCall();
    }

    return call;
  }

  /**
   * Wrapper method for Retrofits {@link Call#execute()} call returning a response specific to the
   * API implementing this class.
   *
   * @return the response once the call completes successfully
   * @throws IOException Signals that an I/O exception of some sort has occurred
   * @since 3.0.0
   */
  public Response<T> executeCall() throws IOException {
    return getCall().execute();
  }

  /**
   * Wrapper method for Retrofits {@link Call#enqueue(Callback)} call returning a response specific
   * to the API implementing this class. Use this method to make a request on the Main Thread.
   *
   * @param callback a {@link Callback} which is used once the API response is created.
   * @since 3.0.0
   */
  public void enqueueCall(Callback<T> callback) {
    getCall().enqueue(callback);
  }

  /**
   * Wrapper method for Retrofits {@link Call#cancel()} call, important to manually cancel call if
   * the user dismisses the calling activity or no longer needs the returned results.
   *
   * @since 3.0.0
   */
  public void cancelCall() {
    getCall().cancel();
  }

  /**
   * Wrapper method for Retrofits {@link Call#clone()} call, useful for getting call information.
   *
   * @return cloned call
   * @since 3.0.0
   */
  public Call<T> cloneCall() {
    return getCall().clone();
  }

  /**
   * Creates the Retrofit object and the service if they are not already created. Subclasses can
   * override getGsonBuilder to add anything to the GsonBuilder.
   *
   * @return new service if not already created, otherwise the existing service
   * @since 3.0.0
   */
  protected S getService() {
    // No need to recreate it
    if (service != null) {
      return service;
    }

    Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
      .baseUrl(baseUrl())
      .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()));

    if (getCallFactory() != null) {
      retrofitBuilder.callFactory(getCallFactory());
    } else {
      okHttpClient = okHttpClient == null ? initializeOkHttpClient() : okHttpClient;
      retrofitBuilder.client(okHttpClient);
    }

    retrofit = retrofitBuilder.build();
    service = (S) retrofit.create(serviceType);
    return service;
  }

  /**
   * Returns the retrofit instance.
   *
   * @return retrofit, or null if it hasn't been initialized yet.
   * @since 3.0.0
   */
  public Retrofit getRetrofit() {
    return retrofit;
  }

  /**
   * Gets the GsonConverterFactory. Subclasses can override to register TypeAdapterFactories, etc.
   *
   * @return GsonBuilder for Retrofit
   * @since 3.0.0
   */
  protected GsonBuilder getGsonBuilder() {
    return new GsonBuilder();
  }

  /**
   * Returns if debug logging is enabled in Okhttp
   *
   * @return whether enableDebug is true
   * @since 3.0.0
   */
  public boolean isEnableDebug() {
    return enableDebug;
  }

  /**
   * Enable for more verbose log output while making request.
   *
   * @param enableDebug true if you'd like Okhttp to log
   * @since 3.0.0
   */
  public void enableDebug(boolean enableDebug) {
    this.enableDebug = enableDebug;
  }

  /**
   * Gets the call factory for creating {@link Call} instances.
   *
   * @return the call factory, or the default OkHttp client if it's null.
   * @since 2.0.0
   */
  public okhttp3.Call.Factory getCallFactory() {
    return callFactory;
  }

  /**
   * Specify a custom call factory for creating {@link Call} instances.
   *
   * @param callFactory implementation
   * @since 2.0.0
   */
  public void setCallFactory(okhttp3.Call.Factory callFactory) {
    this.callFactory = callFactory;
  }

  /**
   * Used Internally.
   *
   * @return OkHttpClient
   * @since 1.0.0
   */
  protected synchronized OkHttpClient initializeOkHttpClient() {
    if (isEnableDebug()) {
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
      OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
      httpClient.addInterceptor(logging);
      return httpClient.build();
    } else {
      return new OkHttpClient();
    }
  }
}

