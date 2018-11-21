package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

/**
 * Can be extended to add a callback to an async task implementation.
 */
public abstract class CallbackAsyncTask<P, Q, R> extends AsyncTask<P, Q, R> {
  Callback<R> callback;

  CallbackAsyncTask(Callback<R> callback) {
    this.callback = callback;
  }

  @Override
  protected void onPostExecute(R result) {
    callback.onResult(result);
  }

  public interface Callback<R> {
    void onResult(R result);
  }
}