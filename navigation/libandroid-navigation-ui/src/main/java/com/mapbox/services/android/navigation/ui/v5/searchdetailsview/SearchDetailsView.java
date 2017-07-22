package com.mapbox.services.android.navigation.ui.v5.searchdetailsview;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;

import java.util.ArrayList;
import java.util.List;

public class SearchDetailsView extends CoordinatorLayout {

  private BottomSheetBehavior bottomSheetBehavior;
  private Button exapandBottomSheetButton;
  private FloatingActionButton floatingActionButton;
  private CoordinatorLayout rootView;
  private List<SearchDetailItem> detailItems;
  private RecyclerView.Adapter adapter;

  public SearchDetailsView(Context context) {
    this(context, null);
  }

  public SearchDetailsView(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SearchDetailsView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context);
  }

  private void initialize(Context context) {
    rootView = (CoordinatorLayout) inflate(context, R.layout.view_search_details, this);
    bottomSheetBehavior = BottomSheetBehavior.from(rootView.findViewById(R.id.bottomSheet));
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    addExpandButtonListener();
    addFloatingActionButtonListener();

    detailItems = new ArrayList<>();
    detailItems.add(new SearchDetailItem("Phone", "(713) 504-3029", R.drawable.ic_car));
    detailItems.add(new SearchDetailItem("Phone", "(713) 504-3029", R.drawable.ic_car));

    adapter = new SearchDetailsAdapter(detailItems);
    RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.searchDetailsRecyclerView);
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new CustomDividerItemDecorator(context));
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
    recyclerView.setAdapter(adapter);
  }

  private void addFloatingActionButtonListener() {
    floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.fabStartNavigation);
    // TODO allow users to handle click events
  }

  private void addExpandButtonListener() {
    exapandBottomSheetButton = (Button) rootView.findViewById(R.id.expandSheetButton);
    exapandBottomSheetButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        bottomSheetBehavior.setState((bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
          ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED);
      }
    });
  }

  public void onSearchResult(CarmenFeature carmenFeature) {
    bottomSheetBehavior.setHideable(false);
    bottomSheetBehavior.setPeekHeight(rootView.findViewById(R.id.contentSearchDetail).getHeight());
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    revealAnimation(true);
  }

  public void hide() {
    revealAnimation(false);
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
  }

  public int getState() {
    return bottomSheetBehavior.getState();
  }


  private void revealAnimation(boolean show) {

    if (show) {
      floatingActionButton.animate().scaleY(1).scaleX(1).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);
      floatingActionButton.setVisibility(VISIBLE);
    } else {
      floatingActionButton.animate().scaleY(0).scaleX(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);


    }
  }


}
