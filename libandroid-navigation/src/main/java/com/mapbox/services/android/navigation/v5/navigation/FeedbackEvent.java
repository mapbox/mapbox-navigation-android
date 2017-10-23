package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.StringDef;

import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FeedbackEvent {

  @Retention(RetentionPolicy.SOURCE)
  @StringDef({
    FEEDBACK_TYPE_GENERAL,
    FEEDBACK_TYPE_ACCIDENT,
    FEEDBACK_TYPE_HAZARD,
    FEEDBACK_TYPE_ROAD_CLOSED,
    FEEDBACK_TYPE_NOT_ALLOWED_TURN,
    FEEDBACK_TYPE_ROUTING_ERROR,
    FEEDBACK_TYPE_INSTRUCTION_TIMING,
    FEEDBACK_TYPE_CONFUSING_INSTRUCTION,
    FEEDBACK_TYPE_INACCURATE_GPS,
    FEEDBACK_TYPE_BAD_ROUTE,
    FEEDBACK_TYPE_REPORT_TRAFFIC
  })
  public @interface FeedbackType {
  }

  @Retention(RetentionPolicy.SOURCE)
  @StringDef({
    FEEDBACK_SOURCE_REROUTE,
    FEEDBACK_SOURCE_UI
  })
  public @interface FeedbackSource {
  }

  public static final String FEEDBACK_TYPE_GENERAL = "general";
  public static final String FEEDBACK_TYPE_ACCIDENT = "accident";
  public static final String FEEDBACK_TYPE_HAZARD = "hazard";
  public static final String FEEDBACK_TYPE_ROAD_CLOSED = "road_closed";
  public static final String FEEDBACK_TYPE_NOT_ALLOWED_TURN = "not_allowed_turn";
  public static final String FEEDBACK_TYPE_ROUTING_ERROR = "routing_error";
  public static final String FEEDBACK_TYPE_INSTRUCTION_TIMING = "instruction_timing";
  public static final String FEEDBACK_TYPE_CONFUSING_INSTRUCTION = "confusing_instruction";
  public static final String FEEDBACK_TYPE_INACCURATE_GPS = "inaccurate_gps";
  public static final String FEEDBACK_TYPE_BAD_ROUTE = "bad_route";
  public static final String FEEDBACK_TYPE_REPORT_TRAFFIC = "report_traffic";

  public static final String FEEDBACK_SOURCE_REROUTE = "reroute";
  public static final String FEEDBACK_SOURCE_UI = "user";

  private String feedbackType;
  private String feedbackSource;
  private String feedbackId;
  private String description;
  private SessionState sessionState;

  public FeedbackEvent(SessionState sessionState, @FeedbackSource String feedbackSource) {
    this.sessionState = sessionState;
    this.feedbackSource = feedbackSource;
    this.feedbackType = "general"; // Default until updated
    this.feedbackId = TelemetryUtils.buildUUID();
  }

  public String getFeedbackId() {
    return feedbackId;
  }

  @FeedbackType
  public String getFeedbackType() {
    return feedbackType;
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

  public SessionState getSessionState() {
    return sessionState;
  }
}
