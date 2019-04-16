package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationStepMetadata implements Parcelable {
  private String upcomingInstruction = null;
  private String upcomingType = null;
  private String upcomingModifier = null;
  private String upcomingName = null;
  private String previousInstruction = null;
  private String previousType = null;
  private String previousModifier = null;
  private String previousName = null;
  private Integer distance = null;
  private Integer duration = null;
  private Integer distanceRemaining = null;
  private Integer durationRemaining = null;

  public NavigationStepMetadata() {
  }

  public void setUpcomingInstruction(String upcomingInstruction) {
    this.upcomingInstruction = upcomingInstruction;
  }

  public void setUpcomingType(String upcomingType) {
    this.upcomingType = upcomingType;
  }

  public void setUpcomingModifier(String upcomingModifier) {
    this.upcomingModifier = upcomingModifier;
  }

  public void setUpcomingName(String upcomingName) {
    this.upcomingName = upcomingName;
  }

  public void setPreviousInstruction(String previousInstruction) {
    this.previousInstruction = previousInstruction;
  }

  public void setPreviousType(String previousType) {
    this.previousType = previousType;
  }

  public void setPreviousModifier(String previousModifier) {
    this.previousModifier = previousModifier;
  }

  public void setPreviousName(String previousName) {
    this.previousName = previousName;
  }

  public void setDistance(Integer distance) {
    this.distance = distance;
  }

  public void setDuration(Integer duration) {
    this.duration = duration;
  }

  public void setDistanceRemaining(Integer distanceRemaining) {
    this.distanceRemaining = distanceRemaining;
  }

  public void setDurationRemaining(Integer durationRemaining) {
    this.durationRemaining = durationRemaining;
  }

  private NavigationStepMetadata(Parcel in) {
    upcomingInstruction = in.readString();
    upcomingType = in.readString();
    upcomingModifier = in.readString();
    upcomingName = in.readString();
    previousInstruction = in.readString();
    previousType = in.readString();
    previousModifier = in.readString();
    previousName = in.readString();
    distance = in.readByte() == 0x00 ? null : in.readInt();
    duration = in.readByte() == 0x00 ? null : in.readInt();
    distanceRemaining = in.readByte() == 0x00 ? null : in.readInt();
    durationRemaining = in.readByte() == 0x00 ? null : in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(upcomingInstruction);
    dest.writeString(upcomingType);
    dest.writeString(upcomingModifier);
    dest.writeString(upcomingName);
    dest.writeString(previousInstruction);
    dest.writeString(previousType);
    dest.writeString(previousModifier);
    dest.writeString(previousName);
    if (distance == null) {
      dest.writeByte((byte) (0x00));
    } else {
      dest.writeByte((byte) (0x01));
      dest.writeInt(distance);
    }
    if (duration == null) {
      dest.writeByte((byte) (0x00));
    } else {
      dest.writeByte((byte) (0x01));
      dest.writeInt(duration);
    }
    if (distanceRemaining == null) {
      dest.writeByte((byte) (0x00));
    } else {
      dest.writeByte((byte) (0x01));
      dest.writeInt(distanceRemaining);
    }
    if (durationRemaining == null) {
      dest.writeByte((byte) (0x00));
    } else {
      dest.writeByte((byte) (0x01));
      dest.writeInt(durationRemaining);
    }
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationStepMetadata> CREATOR =
    new Creator<NavigationStepMetadata>() {
      @Override
      public NavigationStepMetadata createFromParcel(Parcel in) {
        return new NavigationStepMetadata(in);
      }

      @Override
      public NavigationStepMetadata[] newArray(int size) {
        return new NavigationStepMetadata[size];
      }
    };
}
