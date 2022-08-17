package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }
    }
}
