package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.NonNull;
import android.support.transition.Transition;
import android.support.transition.TransitionListenerAdapter;

import com.mapbox.services.android.navigation.ui.v5.summary.list.InstructionListAdapter;

class InstructionListTransitionListener extends TransitionListenerAdapter {

  private final InstructionListAdapter instructionListAdapter;

  InstructionListTransitionListener(InstructionListAdapter instructionListAdapter) {
    this.instructionListAdapter = instructionListAdapter;
  }

  @Override
  public void onTransitionEnd(@NonNull Transition transition) {
    super.onTransitionEnd(transition);
    instructionListAdapter.notifyDataSetChanged();
  }

  @Override
  public void onTransitionCancel(@NonNull Transition transition) {
    super.onTransitionCancel(transition);
    instructionListAdapter.notifyDataSetChanged();
  }
}
