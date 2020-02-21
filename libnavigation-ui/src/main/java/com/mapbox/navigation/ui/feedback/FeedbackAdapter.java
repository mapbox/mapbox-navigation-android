package com.mapbox.navigation.ui.feedback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.libnavigation.ui.R;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackViewHolder> {

  private static final String EMPTY_FEEDBACK_DESCRIPTION = "";
  private List<FeedbackItem> feedbackItems = new ArrayList<>();

  FeedbackAdapter(Context context) {
    // TODO Telemetry impl
   /* feedbackItems.add(new FeedbackItem(context.getString(R.string.feedback_road_closure),
      R.drawable.ic_road_closed, FEEDBACK_TYPE_ROAD_CLOSED, EMPTY_FEEDBACK_DESCRIPTION));
    feedbackItems.add(new FeedbackItem(context.getString(R.string.feedback_not_allowed),
      R.drawable.ic_not_allowed, FEEDBACK_TYPE_NOT_ALLOWED, EMPTY_FEEDBACK_DESCRIPTION));
    feedbackItems.add(new FeedbackItem(context.getString(R.string.feedback_confusing_instruction),
      R.drawable.ic_confusing_directions, FEEDBACK_TYPE_CONFUSING_INSTRUCTION, EMPTY_FEEDBACK_DESCRIPTION));
    feedbackItems.add(new FeedbackItem(context.getString(R.string.feedback_bad_route),
      R.drawable.ic_bad_route, FEEDBACK_TYPE_ROUTING_ERROR, EMPTY_FEEDBACK_DESCRIPTION));*/
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
