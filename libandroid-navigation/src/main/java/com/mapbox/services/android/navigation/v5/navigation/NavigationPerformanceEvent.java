package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.android.telemetry.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NavigationPerformanceEvent extends Event implements Parcelable {
  private static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US);
  private static final String PERFORMANCE_TRACE = "performance.trace";

  protected final String event;
  protected final String created;
  protected final String sessionId;
  protected final List<Counter> counters;
  protected final List<Attribute> attributes;

  private NavigationPerformanceEvent(String event, String created, String sessionId,
                                     List<Counter> counters, List<Attribute> attributes) {
    this.event = event;
    this.created = created;
    this.sessionId = sessionId;
    this.counters = counters;
    this.attributes = attributes;
  }

  NavigationPerformanceEvent(String sessionId) {
    this(PERFORMANCE_TRACE, obtainCurrentDate(), sessionId,
      new ArrayList<Counter>(), new ArrayList<Attribute>());
  }

  private static String obtainCurrentDate() {
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
    parcel.writeString(event);
    parcel.writeString(created);
    parcel.writeString(sessionId);
    parcel.writeList(counters);
    parcel.writeList(attributes);
  }

  public static Creator<NavigationPerformanceEvent> CREATOR = new
    Creator<NavigationPerformanceEvent>() {

    @Override
    public NavigationPerformanceEvent createFromParcel(Parcel parcel) {
      return new NavigationPerformanceEvent(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readArrayList(LongCounter.class.getClassLoader()),
        parcel.readArrayList(Attribute.class.getClassLoader()));
    }

    @Override
    public NavigationPerformanceEvent[] newArray(int size) {
      return new NavigationPerformanceEvent[size];
    }
  };
}
