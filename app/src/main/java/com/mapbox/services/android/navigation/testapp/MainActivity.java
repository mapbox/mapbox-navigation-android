package com.mapbox.services.android.navigation.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.services.android.navigation.testapp.activity.MockNavigationActivity;
import com.mapbox.services.android.navigation.testapp.activity.RerouteActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.DualNavigationMapActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.EmbeddedNavigationActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.EndNavigationActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationLauncherActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationMapRouteActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.WaypointNavigationActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment.FragmentNavigationActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionsListener {
  private RecyclerView recyclerView;
  private PermissionsManager permissionsManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final List<SampleItem> samples = new ArrayList<>(Arrays.asList(
      new SampleItem(
        getString(R.string.title_navigation_launcher),
        getString(R.string.description_navigation_launcher),
        NavigationLauncherActivity.class
      ),
      new SampleItem(
        getString(R.string.title_end_navigation),
        getString(R.string.description_end_navigation),
        EndNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_dual_navigation_map),
        getString(R.string.description_dual_navigation_map),
        DualNavigationMapActivity.class
      ),
      new SampleItem(
        getString(R.string.title_mock_navigation),
        getString(R.string.description_mock_navigation),
        MockNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_reroute),
        getString(R.string.description_reroute),
        RerouteActivity.class
      ),
      new SampleItem(
        getString(R.string.title_navigation_route_ui),
        getString(R.string.description_navigation_route_ui),
        NavigationMapRouteActivity.class
      ),
      new SampleItem(
        getString(R.string.title_waypoint_navigation),
        getString(R.string.description_waypoint_navigation),
        WaypointNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_embedded_navigation),
        getString(R.string.description_embedded_navigation),
        EmbeddedNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_fragment_navigation),
        getString(R.string.description_fragment_navigation),
        FragmentNavigationActivity.class
      )
    ));

    // RecyclerView
    recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setHasFixedSize(true);

    // Use a linear layout manager
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);

    // Specify an adapter
    RecyclerView.Adapter adapter = new MainAdapter(samples);
    recyclerView.setAdapter(adapter);

    // Check for location permission
    permissionsManager = new PermissionsManager(this);
    if (!PermissionsManager.areLocationPermissionsGranted(this)) {
      recyclerView.setEnabled(false);
      permissionsManager.requestLocationPermissions(this);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, "This app needs location permissions in order to show its functionality.",
      Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      recyclerView.setEnabled(true);
    } else {
      Toast.makeText(this, "You didn't grant location permissions.",
        Toast.LENGTH_LONG).show();
    }
  }

  /*
   * Recycler view
   */

  private class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private List<SampleItem> samples;

    class ViewHolder extends RecyclerView.ViewHolder {

      private TextView nameView;
      private TextView descriptionView;

      ViewHolder(View view) {
        super(view);
        nameView = (TextView) view.findViewById(R.id.nameView);
        descriptionView = (TextView) view.findViewById(R.id.descriptionView);
      }
    }

    MainAdapter(List<SampleItem> samples) {
      this.samples = samples;
    }

    @Override
    public MainAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater
        .from(parent.getContext())
        .inflate(R.layout.item_main_feature, parent, false);

      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          int position = recyclerView.getChildLayoutPosition(view);
          Intent intent = new Intent(view.getContext(), samples.get(position).getActivity());
          startActivity(intent);
        }
      });

      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MainAdapter.ViewHolder holder, int position) {
      holder.nameView.setText(samples.get(position).getName());
      holder.descriptionView.setText(samples.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
      return samples.size();
    }
  }
}
