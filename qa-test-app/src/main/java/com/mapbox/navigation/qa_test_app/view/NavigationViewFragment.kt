package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.databinding.LayoutFragmentNavigationViewBinding
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel

@ExperimentalPreviewMapboxNavigationAPI
class NavigationViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return LayoutFragmentNavigationViewBinding.inflate(inflater).root
    }
}
