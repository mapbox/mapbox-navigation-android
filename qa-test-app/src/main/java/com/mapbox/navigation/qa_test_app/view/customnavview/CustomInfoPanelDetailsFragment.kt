package com.mapbox.navigation.qa_test_app.view.customnavview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.navigation.qa_test_app.databinding.LayoutFragmentInfoPanelDetailsBinding

class CustomInfoPanelDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding =
            LayoutFragmentInfoPanelDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
}
