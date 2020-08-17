package com.mapbox.navigation.ui.feedback;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.navigation.ui.R;

/**
 * A ViewHolder describes a Feedback category view and metadata about its place within the RecyclerView.
 */
class FeedbackViewHolder extends RecyclerView.ViewHolder {

  private ImageView feedbackImage;
  private TextView feedbackText;

  FeedbackViewHolder(@NonNull View itemView) {
    super(itemView);
    feedbackImage = itemView.findViewById(R.id.feedbackImage);
    feedbackText = itemView.findViewById(R.id.feedbackText);
  }

  void setFeedbackImage(int feedbackImageId) {
    feedbackImage.setImageDrawable(AppCompatResources.getDrawable(feedbackImage.getContext(), feedbackImageId));
  }

  void setFeedbackText(String feedbackText) {
    this.feedbackText.setText(feedbackText);
  }

}
