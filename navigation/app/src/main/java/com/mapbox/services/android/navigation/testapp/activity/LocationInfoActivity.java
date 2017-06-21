package com.mapbox.services.android.navigation.testapp.activity;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class LocationInfoActivity extends AppCompatActivity implements LocationEngineListener {

  @BindView(R.id.accuracyTextView)
  TextView accuracyTextView;
  @BindView(R.id.speedTextView)
  TextView speedTextView;
  @BindView(R.id.bearingTextView)
  TextView bearingTextView;
  @BindView(R.id.latitudeTextView)
  TextView latitudeTextView;
  @BindView(R.id.longitudeTextView)
  TextView longitudeTextView;
  @BindView(R.id.satCountTextView)
  TextView satCountTextView;

  private LocationEngine locationEngine;
  private LocationManager locationManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_location_info);
    ButterKnife.bind(this);

    locationEngine = LostLocationEngine.getLocationEngine(this);
    locationEngine.setFastestInterval(1000);
    locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
    locationEngine.setSmallestDisplacement(0);
    locationEngine.setInterval(0);
    locationEngine.addLocationEngineListener(this);
    locationEngine.activate();

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  public void onConnected() {
    Timber.d("LocationEngine connected");
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    updateLocationView(location);
  }

  private void updateLocationView(Location location) {
    accuracyTextView.setText(String.format(Locale.ENGLISH, "%f m", location.getAccuracy()));
    speedTextView.setText(String.format(Locale.ENGLISH, "%f mph", convertToMph(location.getSpeed())));
    bearingTextView.setText(String.format(Locale.ENGLISH, "%f degrees", location.getBearing()));
    latitudeTextView.setText(String.format(Locale.ENGLISH, "%f", location.getLatitude()));
    longitudeTextView.setText(String.format(Locale.ENGLISH, "%f", location.getLongitude()));
    satCountTextView.setText(String.format(Locale.ENGLISH, "%d", getSatelliteCount()));
  }

  @SuppressWarnings( {"MissingPermission"})
  private int getSatelliteCount() {
    if (locationManager == null) {
      return 0;
    }
    int count = 0;
    GpsStatus status = locationManager.getGpsStatus(null);
    for (GpsSatellite sat : status.getSatellites()) {
      if (sat.usedInFix()) {
        count++;
      }
    }

    return count;
  }

  private double convertToMph(float speed) {
    return speed * 2.2369;
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (locationEngine != null) {
      locationEngine.addLocationEngineListener(this);
      locationEngine.activate();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (locationEngine != null) {
      locationEngine.removeLocationEngineListener(this);
      locationEngine.deactivate();
    }
  }
}