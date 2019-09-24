package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

public class InstructionListAdapter extends RecyclerView.Adapter<InstructionViewHolder> {

  private final InstructionListPresenter presenter;

  public InstructionListAdapter(RouteUtils routeUtils, DistanceFormatter distanceFormatter) {
    presenter = new InstructionListPresenter(routeUtils, distanceFormatter);
  }

  @NonNull
  @Override
  public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.instruction_viewholder_layout, parent, false);
    return new InstructionViewHolder(view);
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
    boolean didUpdate = presenter.updateBannerListWith(routeProgress);
    if (didUpdate && isListShowing) {
      notifyDataSetChanged();
    }
  }

  public void updateDistanceFormatter(DistanceFormatter distanceFormatter) {
    presenter.updateDistanceFormatter(distanceFormatter);
  }
}
