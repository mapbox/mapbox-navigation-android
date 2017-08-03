package com.mapbox.services.android.navigation.ui.v5.searchdetailsview;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.List;

class SearchDetailsAdapter extends RecyclerView.Adapter<SearchDetailsAdapter.ViewHolder> {

  private List<SearchDetailItem> dataSource;

  SearchDetailsAdapter(List<SearchDetailItem> dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // create a new view
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_search_detail, parent, false);

    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    SearchDetailItem detailItem = dataSource.get(position);

    holder.titleTextView.setText(detailItem.getTitle());
    holder.descriptionTextView.setText(detailItem.getDescription());
  }

  @Override
  public int getItemCount() {
    return (null != dataSource ? dataSource.size() : 0);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    TextView titleTextView;
    TextView descriptionTextView;

    ViewHolder(View itemView) {
      super(itemView);
      titleTextView = (TextView) itemView.findViewById(R.id.menuItemTitle);
      descriptionTextView = (TextView) itemView.findViewById(R.id.menuItemDescription);
    }
  }
}
