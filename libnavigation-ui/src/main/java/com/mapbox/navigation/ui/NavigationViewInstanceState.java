package com.mapbox.navigation.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class NavigationViewInstanceState implements Parcelable {

  private int bottomSheetBehaviorState;
  private int recenterButtonVisibility;
  private boolean instructionViewVisible;
  private boolean isWayNameVisible;
  private boolean isMuted;
  @Nullable
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

  @Nullable
  String getWayNameText() {
    return wayNameText;
  }

  boolean isMuted() {
    return isMuted;
  }

  private NavigationViewInstanceState(@NonNull Parcel in) {
    bottomSheetBehaviorState = in.readInt();
    recenterButtonVisibility = in.readInt();
    instructionViewVisible = in.readByte() != 0;
    isWayNameVisible = in.readByte() != 0;
    isMuted = in.readByte() != 0;
    wayNameText = in.readString();
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
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
    @NonNull
    @Override
    public NavigationViewInstanceState createFromParcel(@NonNull Parcel in) {
      return new NavigationViewInstanceState(in);
    }

    @NonNull
    @Override
    public NavigationViewInstanceState[] newArray(int size) {
      return new NavigationViewInstanceState[size];
    }
  };
}
