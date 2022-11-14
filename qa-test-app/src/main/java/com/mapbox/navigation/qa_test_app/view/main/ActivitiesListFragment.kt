package com.mapbox.navigation.qa_test_app.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutFragmentActivitiesListBinding
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription
import com.mapbox.navigation.qa_test_app.view.adapters.ActivitiesListAdaptersSupport
import com.mapbox.navigation.qa_test_app.view.adapters.GenericListAdapter

class ActivitiesListFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var binding: LayoutFragmentActivitiesListBinding

    private val listAdapter by lazy {
        GenericListAdapter(
            ActivitiesListAdaptersSupport.activitiesListOnBindViewHolderFun,
            ActivitiesListAdaptersSupport.viewHolderFactory,
            this::onItemSelected,
            auxViewClickMap = mapOf(Pair(R.id.infoLabel, this::onAuxViewClicked))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutFragmentActivitiesListBinding.inflate(inflater, container, false)

        val layoutManager = LinearLayoutManager(context)
        binding.activitiesList.layoutManager = LinearLayoutManager(context)
        binding.activitiesList.adapter = listAdapter
        binding.activitiesList.addItemDecoration(
            DividerItemDecoration(context, layoutManager.orientation)
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = arguments?.getString(ARG_CATEGORY) ?: "none"

        listAdapter.swap(viewModel.getActivitiesList(category))
    }

    private fun onItemSelected(index: Int, element: TestActivityDescription) {
        viewModel.onSelectItem(element)
    }

    private fun onAuxViewClicked(index: Int, element: TestActivityDescription) {
        viewModel.onSelectInfoIcon(element)
    }

    companion object {
        const val ARG_CATEGORY = "category"

        fun create(category: String) = ActivitiesListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY, category)
            }
        }
    }
}
