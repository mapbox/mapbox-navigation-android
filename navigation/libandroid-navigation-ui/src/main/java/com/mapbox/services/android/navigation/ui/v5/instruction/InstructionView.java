package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationInstructionPlayer;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;

public class InstructionView extends RelativeLayout implements ProgressChangeListener,
  MilestoneEventListener, OffRouteListener {

  private NavigationInstructionPlayer instructionPlayer;

  private ImageView maneuverImage;
  private TextView stepDistanceText;
  private TextView stepInstructionText;
  private TextView soundChipText;
  private FloatingActionButton soundFab;
  private View rerouteLayout;
  private View turnLaneLayout;
  private RecyclerView rvTurnLanes;
  private TurnLaneAdapter turnLaneAdapter;

  private Animation slideDownTop;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private AnimationSet fadeInSlowOut;

  private boolean showingRerouteState;
  private boolean turnLanesHidden;
  public boolean isMuted;

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
    initListeners();
    initVoiceInstructions();
    initTurnLaneRecyclerView();
    initAnimations();
  }

  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    instructionPlayer.play(instruction);
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    update(routeProgress);
  }

  @Override
  public void userOffRoute(Location location) {
    showRerouteState();
  }

  public void soundFabOff() {
    soundFab.setImageResource(R.drawable.ic_sound_off);
  }

  public void soundFabOn() {
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
      this.startAnimation(slideDownTop);
    }
  }

  public void showRerouteState() {
    showingRerouteState = true;
    rerouteLayout.startAnimation(rerouteSlideDownTop);
  }

  public void hideRerouteState() {
    showingRerouteState = false;
    rerouteLayout.startAnimation(rerouteSlideUpTop);
  }

  private void init() {
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }

  private void bind() {
    maneuverImage = findViewById(R.id.maneuverImageView);
    stepDistanceText = findViewById(R.id.stepDistanceText);
    stepInstructionText = findViewById(R.id.stepInstructionText);
    soundChipText = findViewById(R.id.soundText);
    soundFab = findViewById(R.id.soundFab);
    rerouteLayout = findViewById(R.id.rerouteLayout);
    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = findViewById(R.id.rvTurnLanes);
    initInstructionAutoSize();
  }

  private void initListeners() {
    soundFab.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        instructionPlayer.setMuted(isMuted ? unmute() : mute());
      }
    });
  }

  private boolean mute() {
    isMuted = true;
    setSoundChipText(getContext().getString(R.string.muted));
    showSoundChip();
    soundFabOff();
    return isMuted;
  }

  private boolean unmute() {
    isMuted = false;
    setSoundChipText(getContext().getString(R.string.unmuted));
    showSoundChip();
    soundFabOn();
    return isMuted;
  }

  private void initInstructionAutoSize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepInstructionText,
      16, 28, 2, TypedValue.COMPLEX_UNIT_SP);
  }

  private void initVoiceInstructions() {
    instructionPlayer = new NavigationInstructionPlayer(getContext(), null);
  }

  private void initTurnLaneRecyclerView() {
    turnLaneAdapter = new TurnLaneAdapter();
    rvTurnLanes.setAdapter(turnLaneAdapter);
    rvTurnLanes.setHasFixedSize(true);
    rvTurnLanes.setLayoutManager(new LinearLayoutManager(getContext(),
      LinearLayoutManager.HORIZONTAL, false));
  }

  private void initAnimations() {
    Context context = getContext();
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

  private void update(RouteProgress routeProgress) {
    if (routeProgress != null && !showingRerouteState) {
      InstructionModel model = new InstructionModel(routeProgress);
      maneuverImage.setImageResource(model.getManeuverImage());
      stepDistanceText.setText(model.getStepDistanceRemaining());
      stepInstructionText.setText(StringAbbreviator.abbreviate(model.getTextInstruction()));
      addTurnLanes(model);
    }
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

  public boolean toggleMute() {
    return isMuted ? unmute() : mute();
  }
}
