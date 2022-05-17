package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewFragmentLifecycleBinding
import com.mapbox.navigation.utils.internal.logD

private const val FIRST_FRAGMENT_TAG = "FirstFragment"
private const val SECOND_FRAGMENT_TAG = "SecondFragment"

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationViewFragmentLifecycleActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityNavigationViewFragmentLifecycleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewFragmentLifecycleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.commit {
            add(
                R.id.navigationViewFragment,
                PageNavigationFragment(FIRST_FRAGMENT_TAG),
                FIRST_FRAGMENT_TAG
            )
        }
        binding.swapFragments.setOnClickListener {
            swapFragments()
        }
        binding.viewAttachment.setOnClickListener {
            val fragment = supportFragmentManager
                .findFragmentById(R.id.navigationViewFragment) as PageNavigationFragment
            (it as TextView).text = if (fragment.navigationView?.isAttachedToWindow == true) {
                fragment.detachNavigationView()
                "ATTACH VIEW"
            } else {
                fragment.attachNavigationView()
                "DETACH VIEW"
            }
        }
    }

    private fun swapFragments() {
        val fragment = supportFragmentManager.findFragmentById(R.id.navigationViewFragment)
        val newFragmentPair = if (fragment?.tag == FIRST_FRAGMENT_TAG) {
            Pair(PageNavigationFragment(SECOND_FRAGMENT_TAG), SECOND_FRAGMENT_TAG)
        } else {
            Pair(PageNavigationFragment(FIRST_FRAGMENT_TAG), FIRST_FRAGMENT_TAG)
        }
        supportFragmentManager.commit {
            replace(R.id.navigationViewFragment, newFragmentPair.first, newFragmentPair.second)
        }
        binding.viewAttachment.text = "DETACH VIEW"
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PageNavigationFragment(private val logTag: String) : Fragment() {

    var navigationView: NavigationView? = null
        private set
    private var frame: FrameLayout? = null

    init {
        lifecycle.addObserver(LifecycleLogger("$logTag(${hashCode()})"))
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.layout_fragment_frame, null).also { frameLayout ->
            frameLayout as FrameLayout
            frame = frameLayout
            NavigationView(
                context = requireContext(),
                accessToken = getString(R.string.mapbox_access_token),
            ).also { navView ->
                frameLayout.addView(navView)
                navView.lifecycle.addObserver(
                    LifecycleLogger("${logTag}NavView(${navView.hashCode()})")
                )
                navigationView = navView
            }
        }
    }

    fun detachNavigationView() {
        navigationView?.let {
            frame?.removeView(it)
        }
    }

    fun attachNavigationView() {
        navigationView?.let {
            frame?.addView(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigationView = null
        frame = null
    }
}

private const val TAG = "navigation_view_lifecycle_debug"

private class LifecycleLogger(val name: String) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        log("onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        log("onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        log("onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        log("onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        log("onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        log("onDestroy")
    }

    private fun log(state: String) {
        logD("$name - $state", TAG)
    }
}
