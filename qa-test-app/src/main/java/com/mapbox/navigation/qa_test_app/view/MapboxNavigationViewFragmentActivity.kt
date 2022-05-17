package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.LifecycleLogger
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewFragmentBinding
import kotlin.reflect.KClass


class MapboxNavigationViewFragmentActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityNavigationViewFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceIfNotSet(FirstPageNavigationFragment::class)
        binding.swapFragments.setOnClickListener {
            !replaceIfNotSet(FirstPageNavigationFragment::class) ||
                replaceIfNotSet(SecondPageNavigationFragment::class)
        }
    }

    private inline fun <reified T : Fragment> replaceIfNotSet(kClazz: KClass<T>): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.navigationViewFragment)
        val matched = kClazz.isInstance(currentFragment)
        if (!matched) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<T>(R.id.navigationViewFragment)
            }
        }
        return matched
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class FirstPageNavigationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return NavigationView(
            context = requireContext(),
            accessToken = getString(R.string.mapbox_access_token),
            hostingFragment = this
        )
    }

    init {
        lifecycle.addObserver(LifecycleLogger("FirstPageFragment"))
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SecondPageNavigationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return NavigationView(
            context = requireContext(),
            accessToken = getString(R.string.mapbox_access_token),
            hostingFragment = this
        )
    }

    init {
        lifecycle.addObserver(LifecycleLogger("SecondPageFragment"))
    }
}
