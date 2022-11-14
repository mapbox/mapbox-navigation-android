package com.mapbox.navigation.qa_test_app.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Point
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.qa_test_app.view.customnavview.NavigationViewController
import kotlinx.coroutines.launch

class MapboxNavigationViewActivity : DrawerActivity() {

    private lateinit var binding: LayoutActivityNavigationViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    private lateinit var controller: NavigationViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = NavigationViewController(this, binding.navigationView)
        processIntentExtras()

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }
    }

    private fun processIntentExtras() {
        if (intent.getBooleanExtra(ARG_REPLAY_ENABLED, false)) {
            binding.navigationView.api.routeReplayEnabled(true)
        }
        intent.getDestinationPointExtra()?.also { destination ->
            lifecycleScope.launch {
                Log.d("MapboxNavigationViewActivity", "navigating to $destination")
                controller.startActiveGuidance(destination)
            }
        }
    }

    private fun Intent.getDestinationPointExtra(): Point? =
        getStringExtra(ARG_DESTINATION_POINT)?.let {
            runCatching { Point.fromJson(it) }.getOrNull()
        }

    companion object {
        const val ARG_DESTINATION_POINT = "destination"
        const val ARG_REPLAY_ENABLED = "replay_enabled"

        fun startActivity(
            parent: Activity,
            destinationPoint: Point,
            replayEnabled: Boolean = false
        ) {
            val bundle = Bundle().apply {
                putString(ARG_DESTINATION_POINT, destinationPoint.toJson())
                putBoolean(ARG_REPLAY_ENABLED, replayEnabled)
            }
            parent.startActivity<MapboxNavigationViewActivity>(bundle)
        }
    }
}
