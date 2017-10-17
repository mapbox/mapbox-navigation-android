package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

class FeedbackViewHolder extends RecyclerView.ViewHolder {

  private LinearLayout feedbackLayout;
  private ImageView feedbackImage;
  private TextView feedbackText;

  FeedbackViewHolder(View itemView) {
    super(itemView);
    feedbackLayout = itemView.findViewById(R.id.feedbackLayout);
    feedbackImage = itemView.findViewById(R.id.feedbackImage);
    feedbackText = itemView.findViewById(R.id.feedbackText);
  }

  void setFeedbackBackground(int feedbackBackgroundId) {
    feedbackLayout.setBackgroundDrawable(ContextCompat.getDrawable(feedbackLayout.getContext(), feedbackBackgroundId));
  }


  void setFeedbackImage(int feedbackImageId) {
    feedbackImage.setImageDrawable(ContextCompat.getDrawable(feedbackImage.getContext(), feedbackImageId));
  }

  void setFeedbackText(String feedbackText) {
    this.feedbackText.setText(feedbackText);
  }
}
