package com.mapbox.services.android.navigation.ui.v5;

import android.os.Parcel;
import android.os.Parcelable;

class NavigationViewInstanceState implements Parcelable {
  private int bottomSheetBehaviorState;
  private int cameraState;
  private boolean instructionViewVisible;

  NavigationViewInstanceState(int bottomSheetBehaviorState, int cameraState,
                              boolean instructionViewVisible) {
    this.bottomSheetBehaviorState = bottomSheetBehaviorState;
    this.cameraState = cameraState;
    this.instructionViewVisible = instructionViewVisible;
  }

  int getBottomSheetBehaviorState() {
    return bottomSheetBehaviorState;
  }

  int getCameraState() {
    return cameraState;
  }

  boolean isInstructionViewVisible() {
    return instructionViewVisible;
  }

  private NavigationViewInstanceState(Parcel parcel) {
    bottomSheetBehaviorState = parcel.readInt();
    cameraState = parcel.readInt();
    instructionViewVisible = parcel.readByte() != 0x00;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(bottomSheetBehaviorState);
    dest.writeInt(cameraState);
    dest.writeByte((byte) (instructionViewVisible ? 0x01 : 0x00));
  }

  public static final Parcelable.Creator<NavigationViewInstanceState> CREATOR =
    new Parcelable.Creator<NavigationViewInstanceState>() {

      @Override
      public NavigationViewInstanceState createFromParcel(Parcel source) {
        return new NavigationViewInstanceState(source);
      }

      @Override
      public NavigationViewInstanceState[] newArray(int size) {
        return new NavigationViewInstanceState[size];
      }
    };
}
