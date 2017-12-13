package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent.FEEDBACK_TYPE_CONFUSING_INSTRUCTION;
import static com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent.FEEDBACK_TYPE_NOT_ALLOWED;
import static com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent.FEEDBACK_TYPE_REPORT_TRAFFIC;
import static com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent.FEEDBACK_TYPE_ROAD_CLOSED;
import static com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent.FEEDBACK_TYPE_ROUTING_ERROR;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackViewHolder> {

  private List<FeedbackItem> feedbackItems = new ArrayList<>();

  FeedbackAdapter() {
    feedbackItems.add(new FeedbackItem("Not\nAllowed",
      R.drawable.ic_no_turn_allowed, FEEDBACK_TYPE_NOT_ALLOWED, ""));
    feedbackItems.add(new FeedbackItem("Road\nClosed",
      R.drawable.ic_road_closed, FEEDBACK_TYPE_ROAD_CLOSED, ""));
    feedbackItems.add(new FeedbackItem("Report\nTraffic",
      R.drawable.ic_traffic, FEEDBACK_TYPE_REPORT_TRAFFIC, ""));
    feedbackItems.add(new FeedbackItem("Confusing\nInstruction",
      R.drawable.ic_confusing_directions, FEEDBACK_TYPE_CONFUSING_INSTRUCTION, ""));
    feedbackItems.add(new FeedbackItem("Other\nMap Issue",
      R.drawable.ic_map_error, FeedbackEvent.FEEDBACK_TYPE_OTHER_MAP_ISSUE, ""));
    feedbackItems.add(new FeedbackItem("Bad\nRoute",
      R.drawable.ic_wrong_directions, FEEDBACK_TYPE_ROUTING_ERROR, ""));
  }

  @Override
  public FeedbackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.feedback_viewholder_layout, parent, false);
    return new FeedbackViewHolder(view);
  }

  @Override
  public void onBindViewHolder(FeedbackViewHolder viewHolder, int position) {
    viewHolder.setFeedbackImage(feedbackItems.get(position).getFeedbackImageId());
    viewHolder.setFeedbackText(feedbackItems.get(position).getFeedbackText());
  }

  @Override
  public int getItemCount() {
    return feedbackItems.size();
  }

  FeedbackItem getFeedbackItem(int feedbackPosition) {
    if (feedbackPosition < feedbackItems.size()) {
      return feedbackItems.get(feedbackPosition);
    } else {
      return null;
    }
  }
}
