package com.mapbox.navigation.qa_test_app.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.DialogDestinationSelectBinding
import com.mapbox.navigation.qa_test_app.domain.Destination

class SelectDestinationDialogFragment(
    private val destinations: List<Destination>,
    private val onSelectItem: ((dest: Destination, startReplay: Boolean) -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogDestinationSelectBinding.inflate(layoutInflater, container, false)
        destinations.forEach { dest ->
            val textView = TextView(context, null, 0, R.style.DestinationSelectItem).apply {
                text = dest.name
                setOnClickListener {
                    this@SelectDestinationDialogFragment.dismiss()
                    onSelectItem?.invoke(dest, binding.startReplaySwitch.isChecked)
                }
            }
            binding.root.addView(textView)
        }
        return binding.root
    }

    companion object {
        const val TAG = "SelectDestinationDialogFragment"
    }
}
