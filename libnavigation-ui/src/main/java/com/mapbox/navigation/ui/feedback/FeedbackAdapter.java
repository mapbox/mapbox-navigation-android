package com.mapbox.navigation.ui.feedback;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.navigation.ui.R;

import java.util.List;

/**
 * FeedbackAdapter provides a binding from {@link FeedbackBottomSheet} data set
 * to {@link FeedbackViewHolder} that are displayed within a {@link RecyclerView}.
 */
class FeedbackAdapter extends RecyclerView.Adapter<FeedbackViewHolder> {

  private List<FeedbackItem> feedbackItems;

  FeedbackAdapter(List<FeedbackItem> feedbackItems) {
    this.feedbackItems = feedbackItems;
  }

  @Override
  @NonNull
  public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.mapbox_item_feedback, parent, false);
    return new FeedbackViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull FeedbackViewHolder viewHolder, int position) {
    viewHolder.setFeedbackImage(feedbackItems.get(position).getFeedbackImageId());
    viewHolder.setFeedbackText(feedbackItems.get(position).getFeedbackText());
  }

  @Override
  public int getItemCount() {
    return feedbackItems.size();
  }

  @Nullable
  FeedbackItem getFeedbackItem(int feedbackPosition) {
    if (feedbackPosition < feedbackItems.size()) {
      return feedbackItems.get(feedbackPosition);
    } else {
      return null;
    }
  }
}
