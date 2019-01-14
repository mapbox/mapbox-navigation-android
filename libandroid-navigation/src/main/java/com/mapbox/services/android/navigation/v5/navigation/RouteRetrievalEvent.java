package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.android.telemetry.Event;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RouteRetrievalEvent extends Event implements Parcelable {
  private static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US);
  private static final String NAVIGATION_ROUTE_RETRIEVAL = "performance.trace";
  private static final String ELAPSED_TIME_NAME = "elapsed_time";
  private static final String IS_OFFLINE_NAME = "is_offline";
  private final String event;
  private final String created;
  private final List<LongCounter> counters;
  private final List<Attribute> attributes;

  private RouteRetrievalEvent(String event, String created, List<LongCounter> counters,
                              List<Attribute> attributes) {
    this.event = event;
    this.created = created;
    this.counters = counters;
    this.attributes = attributes;
  }

  RouteRetrievalEvent(long elapsedTime, boolean isOffline) {
    this(NAVIGATION_ROUTE_RETRIEVAL, obtainCurrentDate(),
      Collections.singletonList(new LongCounter(ELAPSED_TIME_NAME, elapsedTime)),
      Collections.singletonList(new Attribute(IS_OFFLINE_NAME, Boolean.toString(isOffline))));
  }

  static String obtainCurrentDate() {
    return dateFormat.format(new Date());
  }

  private RouteRetrievalEvent(Parcel parcel) {
    this(parcel.readString(), parcel.readString(),
      parcel.readArrayList(LongCounter.class.getClassLoader()),
      parcel.readArrayList(Attribute.class.getClassLoader()));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(event);
    parcel.writeString(created);
    parcel.writeList(counters);
    parcel.writeList(attributes);
  }

  public static final Creator<RouteRetrievalEvent> CREATOR = new Creator<RouteRetrievalEvent>() {

    @Override
    public RouteRetrievalEvent createFromParcel(Parcel parcel) {
      return new RouteRetrievalEvent(parcel);
    }

    @Override
    public RouteRetrievalEvent[] newArray(int size) {
      return new RouteRetrievalEvent[size];
    }
  };
}
