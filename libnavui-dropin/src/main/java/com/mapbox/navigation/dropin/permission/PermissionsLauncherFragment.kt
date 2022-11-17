package com.mapbox.navigation.dropin.permission

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * A view-less fragment that requests permissions using [ActivityResultCaller] interface.
 */
internal class PermissionsLauncherFragment(
    private val permissions: Array<String>,
    private val onResult: ActivityResultCallback<Map<String, Boolean>>
) : Fragment() {

    private var launcher: ActivityResultLauncher<Array<String>>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val missingPermissions = permissions.filter { permission ->
            checkSelfPermission(context, permission) != PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            launcher = registerForActivityResult(RequestMultiplePermissions(), onResult)
            launcher?.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onDetach() {
        super.onDetach()
        launcher?.unregister()
    }

    companion object {
        const val TAG = "MapboxPermissionsLauncherFragment"

        fun install(
            fragActivity: FragmentActivity,
            permissions: Array<String>,
            onResult: ActivityResultCallback<Map<String, Boolean>>
        ) {
            fragActivity.supportFragmentManager.apply {
                val t = beginTransaction()
                findFragmentByTag(TAG)?.also { t.remove(it) }
                t.add(PermissionsLauncherFragment(permissions, onResult), TAG)
                t.commit()
            }
        }

        fun uninstall(fragActivity: FragmentActivity) {
            if (!fragActivity.isFinishing) {
                fragActivity.supportFragmentManager.apply {
                    findFragmentByTag(TAG)?.also {
                        beginTransaction().remove(it).commit()
                    }
                }
            }
        }
    }
}
