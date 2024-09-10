package com.mapbox.navigation.core.telemetry

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class ApplicationLifecycleMonitor(
    application: Application,
) : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val ONE_HUNDRED_PERCENT = 100
    }

    private val startSessionTime: Long = System.currentTimeMillis()
    private val resumes = arrayListOf<Long>()
    private val pauses = arrayListOf<Long>()
    private val currentOrientation = AtomicInteger(Configuration.ORIENTATION_UNDEFINED)
    private val portraitStartTime = AtomicLong(0L)
    private val portraitTimeInMillis = AtomicReference(0.0)

    init {
        application.registerActivityLifecycleCallbacks(this)
        initCurrentOrientation(application)
    }

    override fun onActivityStarted(activity: Activity) {
        activity.let {
            val newOrientation = it.resources.configuration.orientation
            // If a new orientation is found, set it to the current
            if (!currentOrientation.compareAndSet(newOrientation, newOrientation)) {
                val currentTimeMillis = System.currentTimeMillis()
                // If the current orientation is now landscape, add the time the phone was just in portrait
                when (currentOrientation.get()) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        var portraitTime = portraitTimeInMillis.get()
                        portraitTime += currentTimeMillis - portraitStartTime.get()
                        portraitTimeInMillis.set(portraitTime)
                    }
                    Configuration.ORIENTATION_PORTRAIT -> {
                        portraitStartTime.set(currentTimeMillis)
                    }
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumes.add(System.currentTimeMillis())
    }

    override fun onActivityPaused(activity: Activity) {
        pauses.add(System.currentTimeMillis())
    }

    override fun onActivityDestroyed(activity: Activity) {
        activity.let {
            if (it.isFinishing) {
                it.application.unregisterActivityLifecycleCallbacks(this)
            }
        }
    }

    //region Unused Lifecycle Methods

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    //endregion

    fun obtainPortraitPercentage(): Int {
        // If no changes to landscape
        if (currentOrientation.get() == Configuration.ORIENTATION_PORTRAIT &&
            portraitTimeInMillis.get() == 0.0
        ) {
            return ONE_HUNDRED_PERCENT
        }
        // Calculate given the time spent in portrait
        val portraitFraction =
            portraitTimeInMillis.get() / (System.currentTimeMillis() - startSessionTime)
        return (ONE_HUNDRED_PERCENT * portraitFraction).toInt()
    }

    fun obtainForegroundPercentage(): Int {
        val currentTime = System.currentTimeMillis()
        val foregroundTime = calculateForegroundTime(currentTime)
        return (ONE_HUNDRED_PERCENT * (foregroundTime / (currentTime - startSessionTime))).toInt()
    }

    private fun initCurrentOrientation(application: Application) {
        currentOrientation.set(application.resources.configuration.orientation)
        // If starting in portrait, set the portrait start time
        if (currentOrientation.get() == Configuration.ORIENTATION_PORTRAIT) {
            portraitStartTime.set(System.currentTimeMillis())
        }
    }

    private fun calculateForegroundTime(currentTime: Long): Double {
        val tempResumes = ArrayList(resumes)
        // If the activity was destroyed while in the background
        if (tempResumes.size < pauses.size) {
            tempResumes.add(currentTime)
        }
        var resumePauseDiff = 0L
        for (i in tempResumes.indices) {
            if (i < pauses.size) {
                resumePauseDiff += tempResumes[i] - pauses[i]
            }
        }
        return (currentTime - resumePauseDiff - startSessionTime).toDouble()
    }
}
