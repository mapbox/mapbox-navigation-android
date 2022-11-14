package com.mapbox.navigation.qa_test_app.view

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.qa_test_app.databinding.ActivityMainBinding
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription
import com.mapbox.navigation.qa_test_app.domain.TestActivitySuite
import com.mapbox.navigation.qa_test_app.utils.PermissionsHelper
import com.mapbox.navigation.qa_test_app.utils.PermissionsState
import com.mapbox.navigation.qa_test_app.view.main.ActivitiesListFragment
import com.mapbox.navigation.qa_test_app.view.main.MainViewModel
import com.mapbox.navigation.qa_test_app.view.main.PageInfo
import com.mapbox.navigation.qa_test_app.view.util.observe
import kotlinx.coroutines.launch

/**
 * To add additional activities see [TestActivitySuite]. Alteration of this class shouldn't be
 * necessary.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionsHelper = PermissionsHelper()
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.pager.adapter = PagerAdapter(supportFragmentManager, viewModel.pages)
        binding.tabLayout.setupWithViewPager(binding.pager)

        viewModel.didSelectItemEvent.observe(this) { element ->
            if (element.launchAfterPermissionResult) {
                launchAfterPermissionResult(element)
            } else {
                element.launchActivityFun(this@MainActivity)
            }
        }

        viewModel.didSelectInfoEvent.observe(this) { element ->
            AlertDialog.Builder(this@MainActivity)
                .setMessage(element.fullDescriptionResource)
                .setTitle("Test Description")
                .setPositiveButton("Ok") { dlg, _ ->
                    dlg.dismiss()
                }.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun launchAfterPermissionResult(element: TestActivityDescription) {
        lifecycleScope.launch {
            val ready = permissionsHelper.checkAndRequestPermissions(
                this@MainActivity,
                DEFAULT_PERMISSIONS
            )
            showToastForDeniedPermissions(ready)
            element.launchActivityFun(this@MainActivity)
        }
    }

    private fun showToastForDeniedPermissions(ready: PermissionsState.Ready) {
        val permissionsDenied = ready.results.filter { !it.isGranted }.map { it.permission }
        if (permissionsDenied.isNotEmpty()) {
            val toastMessage = when {
                permissionsDenied.containsAll(DEFAULT_PERMISSIONS) ->
                    "You didn't grant location or storage permissions."
                permissionsDenied.contains(Manifest.permission.ACCESS_FINE_LOCATION) ->
                    "You didn't grant location permissions."
                permissionsDenied.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) ->
                    "You didn't grant storage permissions."
                else -> "Permissions denied: ${permissionsDenied.joinToString()}"
            }
            Toast.makeText(this@MainActivity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    class PagerAdapter(
        fm: FragmentManager,
        private val pages: List<PageInfo>
    ) : FragmentStatePagerAdapter(fm) {

        override fun getCount(): Int = pages.size

        override fun getItem(i: Int): Fragment = ActivitiesListFragment.create(pages[i].category)

        override fun getPageTitle(position: Int): CharSequence = pages[position].title
    }

    companion object {
        private val DEFAULT_PERMISSIONS = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
    }
}
