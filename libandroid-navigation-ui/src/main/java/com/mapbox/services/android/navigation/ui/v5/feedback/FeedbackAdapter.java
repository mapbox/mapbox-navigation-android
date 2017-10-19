package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.ArrayList;
import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackViewHolder> {

  private List<FeedbackItem> feedbackItems = new ArrayList<>();

  FeedbackAdapter() {
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_orange_background,
      "Instruction\nTiming", R.drawable.ic_access_time, "", ""));
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_red_background,
      "Confusing\nInstruction", R.drawable.ic_routing_error,"", ""));
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_orange_background,
      "Not\nAllowed", R.drawable.ic_road_closed, "", ""));
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_red_background,
      "GPS\nInaccurate", R.drawable.ic_gps_not_fixed, "", ""));
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_orange_background,
      "Bad\nRoute", R.drawable.ic_hazard, "", ""));
    feedbackItems.add(new FeedbackItem(R.drawable.feedback_item_red_background,
      "Report\nTraffic", R.drawable.ic_traffic, "", ""));
  }

  @Override
  public FeedbackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.feedback_viewholder_layout, parent, false);
    return new FeedbackViewHolder(view);
  }

  @Override
  public void onBindViewHolder(FeedbackViewHolder viewHolder, int position) {
    viewHolder.setFeedbackBackground(feedbackItems.get(position).getFeedbackBackgroundId());
    viewHolder.setFeedbackImage(feedbackItems.get(position).getFeedbackImageId());
    viewHolder.setFeedbackText(feedbackItems.get(position).getFeedbackText());
  }

  @Override
  public int getItemCount() {
    return feedbackItems.size();
  }

  FeedbackItem getFeedbackItem(int feedbackPosition) {
    if (feedbackPosition < feedbackItems.size() - 1) {
      return feedbackItems.get(feedbackPosition);
    } else {
      return null;
    }
  }
}
