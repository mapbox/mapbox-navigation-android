package com.mapbox.navigation.examples;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.navigation.examples.activity.HybridNavigationActivity;
import com.mapbox.navigation.examples.activity.MockNavigationActivity;
import com.mapbox.navigation.examples.activity.OffboardRouterActivityJava;
import com.mapbox.navigation.examples.activity.OffboardRouterActivityKt;
import com.mapbox.navigation.examples.activity.OnboardRouterActivityJava;
import com.mapbox.navigation.examples.activity.OnboardRouterActivityKt;
import com.mapbox.navigation.examples.activity.SimpleMapboxNavigationKt;
import com.mapbox.navigation.examples.activity.TripServiceActivityKt;
import com.mapbox.navigation.examples.activity.TripSessionActivityKt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements PermissionsListener {

  @BindView(R.id.settingsFab)
  FloatingActionButton settingsFab;

  private static final int CHANGE_SETTING_REQUEST_CODE = 1;
  private RecyclerView recyclerView;
  private PermissionsManager permissionsManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    settingsFab.setOnClickListener(v -> startActivityForResult(
            new Intent(MainActivity.this, NavigationSettingsActivity.class),
            CHANGE_SETTING_REQUEST_CODE
    ));

    final List<SampleItem> samples = new ArrayList<>(Arrays.asList(
      new SampleItem(
        getString(R.string.title_simple_navigation_kotlin),
        getString(R.string.description_simple_navigation_kotlin),
        SimpleMapboxNavigationKt.class
      ),
      new SampleItem(
        getString(R.string.title_mock_navigation),
        getString(R.string.description_mock_navigation),
        MockNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_offboard_router_kotlin),
        getString(R.string.description_offboard_router_kotlin),
        OffboardRouterActivityKt.class
      ),
      new SampleItem(
        getString(R.string.title_component_hybrid_navigation),
        getString(R.string.description_hybrid_router),
        HybridNavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_offboard_router_java),
        getString(R.string.description_offboard_router_java),
        OffboardRouterActivityJava.class
      ),
      new SampleItem(
        getString(R.string.title_onboard_router_kotlin),
        getString(R.string.description_onboard_router_kotlin),
        OnboardRouterActivityKt.class
      ),
      new SampleItem(
        getString(R.string.title_onboard_router_java),
        getString(R.string.description_onboard_router_java),
        OnboardRouterActivityJava.class
      ),
      new SampleItem(
        getString(R.string.title_trip_service_kotlin),
        getString(R.string.description_trip_service_kotlin),
        TripServiceActivityKt.class
      ),
      new SampleItem(
        getString(R.string.title_trip_session_kotlin),
        getString(R.string.description_trip_session_kotlin),
        TripSessionActivityKt.class
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
