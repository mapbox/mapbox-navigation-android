package com.mapbox.services.android.navigation.ui.v5;

import android.os.Parcel;
import android.os.Parcelable;

class NavigationViewInstanceState implements Parcelable {

  private int bottomSheetBehaviorState;
  private int recenterButtonVisibility;
  private boolean instructionViewVisible;
  private boolean isWayNameVisible;
  private boolean isMuted;
  private String wayNameText;

  NavigationViewInstanceState(int bottomSheetBehaviorState, int recenterButtonVisibility,
                              boolean instructionViewVisible, boolean isWayNameVisible, String wayNameText,
                              boolean isMuted) {
    this.bottomSheetBehaviorState = bottomSheetBehaviorState;
    this.recenterButtonVisibility = recenterButtonVisibility;
    this.instructionViewVisible = instructionViewVisible;
    this.isWayNameVisible = isWayNameVisible;
    this.wayNameText = wayNameText;
    this.isMuted = isMuted;
  }

  int getBottomSheetBehaviorState() {
    return bottomSheetBehaviorState;
  }

  int getRecenterButtonVisibility() {
    return recenterButtonVisibility;
  }

  boolean isInstructionViewVisible() {
    return instructionViewVisible;
  }

  boolean isWayNameVisible() {
    return isWayNameVisible;
  }

  String getWayNameText() {
    return wayNameText;
  }

  boolean isMuted() {
    return isMuted;
  }

  private NavigationViewInstanceState(Parcel in) {
    bottomSheetBehaviorState = in.readInt();
    recenterButtonVisibility = in.readInt();
    instructionViewVisible = in.readByte() != 0;
    isWayNameVisible = in.readByte() != 0;
    isMuted = in.readByte() != 0;
    wayNameText = in.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(bottomSheetBehaviorState);
    dest.writeInt(recenterButtonVisibility);
    dest.writeByte((byte) (instructionViewVisible ? 1 : 0));
    dest.writeByte((byte) (isWayNameVisible ? 1 : 0));
    dest.writeByte((byte) (isMuted ? 1 : 0));
    dest.writeString(wayNameText);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationViewInstanceState> CREATOR = new Creator<NavigationViewInstanceState>() {
    @Override
    public NavigationViewInstanceState createFromParcel(Parcel in) {
      return new NavigationViewInstanceState(in);
    }

    @Override
    public NavigationViewInstanceState[] newArray(int size) {
      return new NavigationViewInstanceState[size];
    }
  };
}
