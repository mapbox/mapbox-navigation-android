package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.StringDef;

import com.mapbox.android.telemetry.TelemetryUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FeedbackEvent implements TelemetryEvent {

  @Retention(RetentionPolicy.SOURCE)
  @StringDef( {
    FEEDBACK_TYPE_GENERAL_ISSUE,
    FEEDBACK_TYPE_OTHER_MAP_ISSUE,
    FEEDBACK_TYPE_ACCIDENT,
    FEEDBACK_TYPE_HAZARD,
    FEEDBACK_TYPE_ROAD_CLOSED,
    FEEDBACK_TYPE_NOT_ALLOWED,
    FEEDBACK_TYPE_ROUTING_ERROR,
    FEEDBACK_TYPE_CONFUSING_INSTRUCTION,
    FEEDBACK_TYPE_INACCURATE_GPS,
    FEEDBACK_TYPE_REPORT_TRAFFIC,
    FEEDBACK_TYPE_MISSING_ROAD,
    FEEDBACK_TYPE_MISSING_EXIT
  })
  public @interface FeedbackType {
  }

  @Retention(RetentionPolicy.SOURCE)
  @StringDef( {
    FEEDBACK_SOURCE_REROUTE,
    FEEDBACK_SOURCE_UI
  })
  public @interface FeedbackSource {
  }

  public static final String FEEDBACK_TYPE_GENERAL_ISSUE = "general";
  public static final String FEEDBACK_TYPE_OTHER_MAP_ISSUE = "other_map_issue";
  public static final String FEEDBACK_TYPE_ACCIDENT = "accident";
  public static final String FEEDBACK_TYPE_HAZARD = "hazard";
  public static final String FEEDBACK_TYPE_ROAD_CLOSED = "road_closed";
  public static final String FEEDBACK_TYPE_NOT_ALLOWED = "not_allowed";
  public static final String FEEDBACK_TYPE_ROUTING_ERROR = "routing_error";
  public static final String FEEDBACK_TYPE_MISSING_ROAD = "missing_road";
  public static final String FEEDBACK_TYPE_MISSING_EXIT = "missing_exit";
  public static final String FEEDBACK_TYPE_CONFUSING_INSTRUCTION = "confusing_instruction";
  public static final String FEEDBACK_TYPE_INACCURATE_GPS = "inaccurate_gps";
  public static final String FEEDBACK_TYPE_REPORT_TRAFFIC = "report_traffic";

  public static final String FEEDBACK_SOURCE_REROUTE = "reroute";
  public static final String FEEDBACK_SOURCE_UI = "user";

  private String feedbackType;
  private String feedbackSource;
  private String screenshot;
  private String eventId;
  private String description;
  private SessionState feedbackSessionState;

  public FeedbackEvent(SessionState sessionState, @FeedbackSource String feedbackSource) {
    this.feedbackSessionState = sessionState;
    this.feedbackSource = feedbackSource;
    this.feedbackType = FEEDBACK_TYPE_GENERAL_ISSUE; // Default until updated
    this.eventId = TelemetryUtils.obtainUniversalUniqueIdentifier();
    this.screenshot = "";
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  @Override
  public SessionState getSessionState() {
    return feedbackSessionState;
  }

  @FeedbackType
  public String getFeedbackType() {
    return feedbackType;
  }

  @FeedbackSource
  public String getFeedbackSource() {
    return feedbackSource;
  }

  public String getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(String screenshot) {
    this.screenshot = screenshot;
  }

  public void setFeedbackType(@FeedbackType String feedbackType) {
    this.feedbackType = feedbackType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
