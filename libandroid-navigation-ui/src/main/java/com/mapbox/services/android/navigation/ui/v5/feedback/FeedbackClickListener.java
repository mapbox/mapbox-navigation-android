package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

public class FeedbackClickListener implements RecyclerView.OnItemTouchListener {

  private static final int FEEDBACK_AT_FIRST_POS = 0;
  private GestureDetector gestureDetector;
  private ClickCallback callback;

  FeedbackClickListener(Context context, ClickCallback callback) {
    this.gestureDetector = new GestureDetector(context, new ResultGestureListener());
    this.callback = callback;
  }

  @Override
  public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
    View child = rv.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
    ViewGroup group = (ViewGroup) child;
    ImageView imageView = null;
    if (group != null && group.getChildAt(FEEDBACK_AT_FIRST_POS) instanceof ImageView) {
      imageView = (ImageView) group.getChildAt(FEEDBACK_AT_FIRST_POS);
    }
    if (child != null && gestureDetector.onTouchEvent(motionEvent)) {
      child.playSoundEffect(SoundEffectConstants.CLICK);
      int position = rv.getChildAdapterPosition(child);
      callback.onFeedbackItemClick(imageView, position);
    }
    return false;
  }

  @Override
  public void onTouchEvent(RecyclerView rv, MotionEvent motionEvent) {

  }

  @Override
  public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

  }

  private static class ResultGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
      return true;
    }
  }

  public interface ClickCallback {

    void onFeedbackItemClick(ImageView view, int feedbackPosition);
  }
}
