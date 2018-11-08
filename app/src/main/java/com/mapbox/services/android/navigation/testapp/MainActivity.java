package com.mapbox.services.android.navigation.testapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.mapbox.services.android.navigation.testapp.activity.OfflineRerouteActivity;
import com.mapbox.services.android.navigation.testapp.activity.RerouteActivity;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.ComponentNavigationActivity;
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

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

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
        getString(R.string.title_offline_reroute),
        getString(R.string.description_offline_reroute),
        OfflineRerouteActivity.class
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
      ),
      new SampleItem(
        getString(R.string.title_component_navigation),
        getString(R.string.description_component_navigation),
        ComponentNavigationActivity.class
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
      recyclerView.setVisibility(View.INVISIBLE);
      permissionsManager.requestLocationPermissions(this);
    } else {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (requestCode == 0) {
      permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    } else {
      boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
      if (!granted) {
        recyclerView.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "You didn't grant storage permissions.", Toast.LENGTH_LONG).show();
      } else {
        recyclerView.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, "This app needs location and storage permissions"
      + "in order to show its functionality.", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show();
    }
  }

  private void requestPermissionIfNotGranted(String permission) {
    List<String> permissionsNeeded = new ArrayList<>();
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionsNeeded.add(permission);
      ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 10);
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
        nameView = view.findViewById(R.id.nameView);
        descriptionView = view.findViewById(R.id.descriptionView);
      }
    }

    MainAdapter(List<SampleItem> samples) {
      this.samples = samples;
    }

    @NonNull
    @Override
    public MainAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater
        .from(parent.getContext())
        .inflate(R.layout.item_main_feature, parent, false);

      view.setOnClickListener(clickedView -> {
        int position = recyclerView.getChildLayoutPosition(clickedView);
        Intent intent = new Intent(clickedView.getContext(), samples.get(position).getActivity());
        startActivity(intent);
      });

      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainAdapter.ViewHolder holder, int position) {
      holder.nameView.setText(samples.get(position).getName());
      holder.descriptionView.setText(samples.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
      return samples.size();
    }
  }
}
