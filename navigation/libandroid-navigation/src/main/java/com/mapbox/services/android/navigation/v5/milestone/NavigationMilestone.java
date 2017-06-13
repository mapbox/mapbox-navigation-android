package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.MapboxNavigation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NavigationMilestone {

  private List<MilestoneEventListener> milestoneListeners;
  private MapboxNavigation navigation;

  public NavigationMilestone(Builder builder) {
    milestoneListeners = new CopyOnWriteArrayList<>();
  }

  public boolean onNextStep(int previousIndex, int newIndex) {
    return previousIndex != newIndex;
  }

  public static final class Builder {

    private float triggerFraction;
    private boolean triggerOnNewStep;
    private int identifier;

    public Builder() {

    }

    public float getTriggerFraction() {
      return triggerFraction;
    }

    public Builder setTriggerFraction(float triggerFraction) {
      this.triggerFraction = triggerFraction;
      return this;
    }

    public boolean isTriggerOnNewStep() {
      return triggerOnNewStep;
    }

    public Builder triggerOnNewStep(boolean triggerOnNewStep) {
      this.triggerOnNewStep = triggerOnNewStep;
      return this;
    }

    public int getIdentifier() {
      return identifier;
    }

    public Builder setIdentifier(int identifier) {
      this.identifier = identifier;
      return this;
    }

    public NavigationMilestone build() {
      return new NavigationMilestone(this);
    }
  }

}
