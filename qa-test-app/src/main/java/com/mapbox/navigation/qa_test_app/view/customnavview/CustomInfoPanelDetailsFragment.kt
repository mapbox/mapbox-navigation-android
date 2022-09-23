package com.mapbox.navigation.qa_test_app.view.customnavview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.navigation.qa_test_app.databinding.LayoutFragmentInfoPanelDetailsLargeBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutFragmentInfoPanelDetailsSmallBinding

class CustomInfoPanelDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contentSize = arguments?.getString("content_size") ?: "SMALL"
        return if (contentSize == "LARGE") {
            LayoutFragmentInfoPanelDetailsLargeBinding
                .inflate(inflater, container, false)
                .root
        } else {
            LayoutFragmentInfoPanelDetailsSmallBinding
                .inflate(inflater, container, false)
                .root
        }
    }

    companion object {
        fun create(contentSize: String = "SMALL"): CustomInfoPanelDetailsFragment {
            return CustomInfoPanelDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("content_size", contentSize)
                }
            }
        }
    }
}
