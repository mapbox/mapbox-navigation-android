package com.mapbox.navigation.ui.internal.summary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;

public class InstructionListAdapter extends RecyclerView.Adapter<InstructionViewHolder> {

  @NonNull
  private final InstructionListPresenter presenter;

  private int primaryTextColor;
  private int secondaryTextColor;
  private int maneuverViewPrimaryColor;
  private int maneuverViewSecondaryColor;

  public InstructionListAdapter(DistanceFormatter distanceFormatter) {
    presenter = new InstructionListPresenter(distanceFormatter);
  }

  @NonNull
  @Override
  public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.mapbox_item_instruction, parent, false);
    InstructionViewHolder viewHolder = new InstructionViewHolder(view);
    viewHolder.updateViewColors(
      primaryTextColor, secondaryTextColor, maneuverViewPrimaryColor, maneuverViewSecondaryColor);
    return viewHolder;
  }

  @Override
  public void onBindViewHolder(@NonNull InstructionViewHolder holder, int position) {
    presenter.onBindInstructionListViewAtPosition(position, holder);
  }

  @Override
  public int getItemCount() {
    return presenter.retrieveBannerInstructionListSize();
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull InstructionViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    holder.itemView.clearAnimation();
  }

  public void updateBannerListWith(RouteProgress routeProgress, boolean isListShowing) {
    if (isListShowing) {
      presenter.update(routeProgress, this);
    }
  }

  public void updateDistanceFormatter(DistanceFormatter distanceFormatter) {
    presenter.updateDistanceFormatter(distanceFormatter);
  }

  public void setColors(int primaryTextColor, int secondaryTextColor,
                        int maneuverPrimaryColor, int maneuverSecondaryColor) {
    this.primaryTextColor = primaryTextColor;
    this.secondaryTextColor = secondaryTextColor;
    this.maneuverViewPrimaryColor = maneuverPrimaryColor;
    this.maneuverViewSecondaryColor = maneuverSecondaryColor;
  }
}
