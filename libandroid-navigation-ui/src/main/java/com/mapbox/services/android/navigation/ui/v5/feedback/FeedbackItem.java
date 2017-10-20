package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FeedbackItem {

  @FeedbackType
  private String feedbackType;
  private String feedbackText;
  private String description;
  private int feedbackImage;

  @Retention(RetentionPolicy.SOURCE)
  @StringDef( {
    FEEDBACK_TYPE_GENERAL,
    FEEDBACK_TYPE_ACCIDENT,
    FEEDBACK_TYPE_HAZARD,
    FEEDBACK_TYPE_ROAD_CLOSED,
    FEEDBACK_TYPE_UNALLOWED_TURN,
    FEEDBACK_TYPE_ROUTING_ERROR,
    FEEDBACK_TYPE_INSTRUCTION_TIMING,
    FEEDBACK_TYPE_CONFUSING_INSTRUCTION,
    FEEDBACK_TYPE_INACCURATE_GPS,
    FEEDBACK_TYPE_BAD_ROUTE,
    FEEDBACK_TYPE_REPORT_TRAFFIC
  })
  public @interface FeedbackType {
  }

  public static final String FEEDBACK_TYPE_GENERAL = "general";
  public static final String FEEDBACK_TYPE_ACCIDENT = "accident";
  public static final String FEEDBACK_TYPE_HAZARD = "hazard";
  public static final String FEEDBACK_TYPE_ROAD_CLOSED = "road_closed";
  public static final String FEEDBACK_TYPE_UNALLOWED_TURN = "unallowed_turn";
  public static final String FEEDBACK_TYPE_ROUTING_ERROR = "routing_error";
  public static final String FEEDBACK_TYPE_INSTRUCTION_TIMING = "instruction_timing";
  public static final String FEEDBACK_TYPE_CONFUSING_INSTRUCTION = "confusing_instruction";
  public static final String FEEDBACK_TYPE_INACCURATE_GPS = "inaccurate_gps";
  public static final String FEEDBACK_TYPE_BAD_ROUTE = "bad_route";
  public static final String FEEDBACK_TYPE_REPORT_TRAFFIC = "report_traffic";

  FeedbackItem(String feedbackText,
               int feedbackImage,
               @FeedbackType String feedbackType,
               String description) {
    this.feedbackText = feedbackText;
    this.feedbackImage = feedbackImage;
    this.feedbackType = feedbackType;
    this.description = description;
  }

  String getFeedbackText() {
    return feedbackText;
  }

  int getFeedbackImageId() {
    return feedbackImage;
  }

  @FeedbackType
  public String getFeedbackType() {
    return feedbackType;
  }

  public String getDescription() {
    return description;
  }
}
