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
    feedbackItems.add(new FeedbackItem("Road\nClosed",
      R.drawable.ic_road_closed, "", ""));
    feedbackItems.add(new FeedbackItem("Not\nAllowed",
      R.drawable.ic_no_turn_allowed, "", ""));
    feedbackItems.add(new FeedbackItem("Report\nTraffic",
      R.drawable.ic_traffic, "", ""));
    feedbackItems.add(new FeedbackItem("Confusing\nInstruction",
      R.drawable.ic_confusing_directions, "", ""));
    feedbackItems.add(new FeedbackItem("GPS\nInaccurate",
      R.drawable.ic_gps, "", ""));
    feedbackItems.add(new FeedbackItem("Bad\nRoute",
      R.drawable.ic_wrong_directions, "", ""));
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
    if (feedbackPosition < feedbackItems.size() - 1) {
      return feedbackItems.get(feedbackPosition);
    } else {
      return null;
    }
  }
}
