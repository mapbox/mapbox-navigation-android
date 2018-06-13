package com.mapbox.services.android.navigation.v5.location.gpx;

import android.animation.TimeAnimator;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class can be used to replay ".gpx" files, returning {@link Location} objects
 * with {@link GpxTimeListener}.
 *
 * The constructor takes an {@link InputStream} and {@link OnGpxAnimatorReadyCallback}.  Once the
 * stream has finished being parsed, the callback will be triggered indicating you can begin GPX playback
 * with {@link GpxAnimator#start()}.
 *
 * The {@link InputStream} will be automatically closed once the parsing has finished.
 *
 * @since 0.3.0
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class GpxAnimator extends TimeAnimator {

  private static final String TAG = GpxAnimator.class.getSimpleName();

  private final CopyOnWriteArrayList<GpxLocationListener> gpxLocationListeners = new CopyOnWriteArrayList<>();
  private final OnGpxAnimatorReadyCallback callback;
  private List<Location> gpxLocationList;

  public GpxAnimator(InputStream gpxInputStream, OnGpxAnimatorReadyCallback callback) {
    this.callback = callback;
    parse(gpxInputStream);
  }

  /**
   * This method should be called after {@link OnGpxAnimatorReadyCallback#onGpxAnimatorReady(GpxAnimator)}.
   *
   * Once called, {@link Location} updates will begin to be passed to {@link GpxLocationListener} based on their
   * timing as specified by the original GPX file.
   *
   * @since 0.3.0
   */
  @Override
  public void start() {
    setTimeListener(new GpxTimeListener(gpxLocationListeners, gpxLocationList));
    super.start();
  }

  /**
   * Adds a listener that gets invoked when a new {@link Location} should be provided from the GPX trace.
   *
   * @param listener GpxLocationListener that is invoked when when a new {@link Location} should be
   *                 provided from the GPX trace
   * @since 0.3.0
   */
  public void addLocationListener(GpxLocationListener listener) {
    gpxLocationListeners.add(listener);
  }

  /**
   * Removes a listener that gets invoked when a new {@link Location} should be provided from the GPX trace.
   *
   * @param listener GpxLocationListener that is invoked when when a new {@link Location} should be
   *                 provided from the GPX trace
   * @since 0.3.0
   */
  public void removeLocationListener(GpxLocationListener listener) {
    gpxLocationListeners.remove(listener);
  }

  private void parse(InputStream gpxInputStream) {
    GpxParser decoder = new GpxParser();
    new ParseGpxTask(decoder, parseGpxTaskListener).execute(gpxInputStream);
  }

  private ParseGpxTask.Listener parseGpxTaskListener = new ParseGpxTask.Listener() {
    @Override
    public void onParseComplete(@NonNull List<Location> gpxLocationList) {
      GpxAnimator.this.gpxLocationList = gpxLocationList;
      callback.onGpxAnimatorReady(GpxAnimator.this);
      Log.d(TAG, String.format("List successfully parsed: %s", gpxLocationList));
    }

    @Override
    public void onParseError(Exception exception) {
      Log.e(TAG, "An error occurred while parsing.", exception);
    }
  };
}
