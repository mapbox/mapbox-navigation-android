package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
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
  private View turnLaneLayout;
  private RecyclerView rvTurnLanes;

  private TurnLaneAdapter turnLaneAdapter;

  private Animation slideUpTop;
  private Animation slideDownTop;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private AnimationSet fadeInSlowOut;

  private boolean showingRerouteState;
  private boolean turnLanesHidden = false;

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

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    initTurnLaneRecyclerView();
    initAnimations();
  }

  public void setSoundFabOff() {
    soundFab.setImageResource(R.drawable.ic_sound_off);
  }

  public void setSoundFabOn() {
    soundFab.setImageResource(R.drawable.ic_sound_on);
  }

  public void setSoundChipText(String text) {
    soundChipText.setText(text);
  }

  public void showSoundChip() {
    soundChipText.startAnimation(fadeInSlowOut);
  }

  public void show() {
    if (this.getVisibility() == INVISIBLE) {
      this.setVisibility(VISIBLE);
      this.bringToFront();
      this.startAnimation(slideDownTop);
    }
  }

  public void hide() {
    if (this.getVisibility() == VISIBLE) {
      this.startAnimation(slideUpTop);
      this.setVisibility(INVISIBLE);
    }
  }

  public void showRerouteState() {
    showingRerouteState = true;
    rerouteLayout.startAnimation(rerouteSlideDownTop);
  }

  private void init() {
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }

  private void bind() {
    maneuverImage = (ImageView) findViewById(R.id.maneuverImageView);
    distanceText = (TextView) findViewById(R.id.distanceText);
    instructionText = (TextView) findViewById(R.id.instructionText);
    soundChipText = (TextView) findViewById(R.id.soundText);
    soundFab = (FloatingActionButton) findViewById(R.id.soundFab);
    rerouteLayout = findViewById(R.id.rerouteLayout);
    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = (RecyclerView) findViewById(R.id.rvTurnLanes);
  }

  private void initTurnLaneRecyclerView() {
    turnLaneAdapter = new TurnLaneAdapter();
    rvTurnLanes.setAdapter(turnLaneAdapter);
    rvTurnLanes.setHasFixedSize(true);
    rvTurnLanes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
  }

  private void initAnimations() {
    Context context = getContext();
    slideUpTop = AnimationUtils.loadAnimation(context, R.anim.slide_up_top);
    slideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);
    rerouteSlideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);
    rerouteSlideUpTop = AnimationUtils.loadAnimation(context, R.anim.slide_up_top);

    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(300);

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setStartOffset(1000);
    fadeOut.setDuration(1000);

    fadeInSlowOut = new AnimationSet(false);
    fadeInSlowOut.addAnimation(fadeIn);
    fadeInSlowOut.addAnimation(fadeOut);
  }

  private void hideRerouteState() {
    showingRerouteState = false;
    rerouteLayout.startAnimation(rerouteSlideUpTop);
  }

  private void addTurnLanes(InstructionModel model) {
    if (model.getTurnLanes() != null
      && !TextUtils.isEmpty(model.getManeuverModifier())) {
      turnLaneAdapter.addTurnLanes(model.getTurnLanes(), model.getManeuverModifier());
      showTurnLanes();
    } else {
      hideTurnLanes();
    }
  }

  private void showTurnLanes() {
    if (turnLanesHidden) {
      turnLanesHidden = false;
      turnLaneLayout.setVisibility(VISIBLE);
    }
  }

  private void hideTurnLanes() {
    if (!turnLanesHidden) {
      turnLanesHidden = true;
      turnLaneLayout.setVisibility(GONE);
    }
  }
}
