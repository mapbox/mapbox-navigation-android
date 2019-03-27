package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.android.telemetry.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class NavigationPerformanceEvent extends Event implements Parcelable {
  private static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US);
  private static final String PERFORMANCE_TRACE = "mobile.performance_trace";
  private static final String EVENT_NAME = "event_name";

  private final String event;
  private final String created;
  private final String sessionId;
  private final List<Counter> counters;
  private final List<Attribute> attributes;
  NavigationPerformanceMetadata metadata;

  NavigationPerformanceEvent(String sessionId, String eventName,
                             NavigationPerformanceMetadata metadata) {
    this.event = PERFORMANCE_TRACE;
    this.created = obtainCurrentDate();
    this.sessionId = sessionId;
    this.counters = new ArrayList<>();
    this.attributes = new ArrayList<>();
    this.metadata = metadata;
    attributes.add(new Attribute(EVENT_NAME, eventName));
  }

  private String obtainCurrentDate() {
    return dateFormat.format(new Date());
  }

  void addCounter(Counter counter) {
    counters.add(counter);
  }

  void addAttribute(Attribute attribute) {
    attributes.add(attribute);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
  }
}
