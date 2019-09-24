package com.mapbox.services.android.navigation.ui.v5.instruction;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;

import com.mapbox.services.android.navigation.ui.v5.summary.list.InstructionListAdapter;

class InstructionListTransitionListener extends TransitionListenerAdapter {

  private static final int TOP = 0;
  private final RecyclerView rvInstructions;
  private final InstructionListAdapter instructionListAdapter;

  InstructionListTransitionListener(RecyclerView rvInstructions, InstructionListAdapter instructionListAdapter) {
    this.rvInstructions = rvInstructions;
    this.instructionListAdapter = instructionListAdapter;
  }

  @Override
  public void onTransitionEnd(@NonNull Transition transition) {
    super.onTransitionEnd(transition);
    onAnimationFinished();
  }

  @Override
  public void onTransitionCancel(@NonNull Transition transition) {
    super.onTransitionCancel(transition);
    onAnimationFinished();
  }

  private void onAnimationFinished() {
    rvInstructions.stopScroll();
    instructionListAdapter.notifyDataSetChanged();
    rvInstructions.smoothScrollToPosition(TOP);
  }
}
