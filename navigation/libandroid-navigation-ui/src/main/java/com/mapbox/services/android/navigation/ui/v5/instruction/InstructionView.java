package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneAdapter;

public class InstructionView extends RelativeLayout {

  private ImageView maneuverImage;
  private TextView distanceText;
  private TextView instructionText;
  private TextView soundChipText;
  private FloatingActionButton soundFab;
  private View rerouteLayout;
  private View routeTurnLaneLayout;
  private RecyclerView rvTurnLanes;

  private TurnLaneAdapter turnLaneAdapter;

  private Animation slideUpTop;
  private Animation slideDownTop;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private AnimationSet fadeInSlowOut;

  public InstructionView(Context context) {
    this(context, null);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }
}
