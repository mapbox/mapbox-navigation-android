package com.mapbox.navigation.ui.androidauto.feedback.ui

import androidx.annotation.UiThread
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackSender
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * This screen lets the user select a feedback option, walking through nested polls.
 *
 * @param screenshot captures and encodes the map, or `null` to submit without a screenshot.
 * It is started when the screen is constructed, before the screen's template replaces the map.
 */
internal abstract class CarGridFeedbackScreen @UiThread constructor(
    mapboxCarContext: MapboxCarContext,
    private val sourceScreenSimpleName: String,
    private val carFeedbackSender: CarFeedbackSender,
    initialPoll: CarFeedbackPoll,
    screenshot: (suspend () -> String?)?,
) : Screen(mapboxCarContext.carContext) {

    private var currentPoll = initialPoll
    private val parentPolls = ArrayDeque<CarFeedbackPoll>()
    private var submitting = false

    private val iconDownloader = CarFeedbackIconDownloader(screen = this)

    // Started undispatched so the snapshot request is queued before this screen is shown.
    private val encodedScreenshot: Deferred<String?> = if (screenshot != null) {
        lifecycleScope.async(start = CoroutineStart.UNDISPATCHED) { screenshot() }
    } else {
        CompletableDeferred(value = null)
    }

    init {
        addBackPressedHandler {
            onBack()
        }
    }

    abstract fun onFinish()

    override fun onGetTemplate(): Template {
        return GridTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setTitle(currentPoll.title)
            .setSingleList(buildItemList(currentPoll.options))
            .build()
    }

    private fun onBack() {
        if (parentPolls.isNotEmpty() && !submitting) {
            currentPoll = parentPolls.removeLast()
            invalidate()
        } else {
            onFinish()
        }
    }

    private fun buildItemList(options: List<CarFeedbackOption>): ItemList {
        val itemListBuilder = ItemList.Builder()
        for (option in options) {
            val itemBuilder = GridItem.Builder().setTitle(option.title)
            val image = iconDownloader.getOrDownload(option.icon)
            if (image != null && !submitting) {
                itemBuilder.setImage(image, GridItem.IMAGE_TYPE_ICON)
                itemBuilder.setOnClickListener { selectOption(option) }
            } else {
                itemBuilder.setLoading(true)
            }
            itemListBuilder.addItem(itemBuilder.build())
        }
        return itemListBuilder.build()
    }

    private fun selectOption(option: CarFeedbackOption) {
        if (submitting) return
        if (option.nextPoll == null) {
            submit(
                CarFeedbackItem(
                    option.title,
                    option.type,
                    option.subType,
                    option.searchFeedbackReason,
                    option.favoritesFeedbackReason,
                ),
            )
        } else {
            parentPolls.addLast(currentPoll)
            currentPoll = option.nextPoll
            invalidate()
        }
    }

    private fun submit(selectedItem: CarFeedbackItem) {
        submitting = true
        if (!encodedScreenshot.isCompleted) {
            // The capture is bounded by a timeout; show progress while it finishes.
            invalidate()
        }
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val screenshot = awaitScreenshot()
            val sent = carFeedbackSender.send(selectedItem, screenshot, sourceScreenSimpleName)
            val message = if (sent) {
                R.string.car_feedback_submit_toast_success
            } else {
                R.string.car_search_error
            }
            CarToast.makeText(
                carContext,
                carContext.getString(message),
                CarToast.LENGTH_LONG,
            ).show()
            onFinish()
        }
    }

    // Feedback is still worth sending without a screenshot, whatever failed while capturing it.
    private suspend fun awaitScreenshot(): String? {
        return try {
            encodedScreenshot.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logAndroidAutoFailure("Car feedback screenshot failed", e)
            null
        }
    }
}
