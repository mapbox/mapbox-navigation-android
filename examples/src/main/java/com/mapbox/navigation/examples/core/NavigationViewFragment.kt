package com.mapbox.navigation.examples.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.NavigationViewOptions
import com.mapbox.navigation.dropin.ViewProvider
import com.mapbox.navigation.examples.core.databinding.FragmentNavigationViewBinding

class NavigationViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNavigationViewBinding.inflate(inflater)
        val navigationView = NavigationView(
            context = requireContext(),
            accessToken = getString(R.string.mapbox_access_token),
            navigationViewOptions = NavigationViewOptions.Builder(requireContext())
                .useReplayEngine(true)
                .build(),
            lifecycleOwner = this,
            viewModelStoreOwner = this
        )
        binding.navigationViewContainer.addView(navigationView)
        navigationView.navigationViewApi.configureNavigationView(ViewProvider())
        binding.tempStartNavigation.setOnClickListener {
            navigationView.navigationViewApi.temporaryStartNavigation()
        }

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = NavigationViewFragment()
    }
}
