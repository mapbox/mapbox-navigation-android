package com.mapbox.androidauto.car.feedback.ui

import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSearchOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.internal.logAndroidAuto

/**
 * This screen allows the user to search for a destination.
 */
class CarGridFeedbackScreen constructor(
    carContext: CarContext,
    private val sourceScreenSimpleName: String,
    private val carFeedbackSender: CarFeedbackSender,
    initialPoll: CarFeedbackPoll,
    private val encodedSnapshot: String?,
    private val searchOptions: CarFeedbackSearchOptions = CarFeedbackSearchOptions(),
    private val onFinish: () -> Unit,
) : Screen(carContext) {

    private var currentPoll = initialPoll
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            onFinish()
        }
    }

    private val iconDownloader = CarFeedbackIconDownloader(screen = this)

    init {
        logAndroidAuto("FeedbackScreen constructor")
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    logAndroidAuto("FeedbackScreen onResume")
                    carContext.onBackPressedDispatcher.addCallback(onBackPressedCallback)
                }

                override fun onPause(owner: LifecycleOwner) {
                    logAndroidAuto("FeedbackScreen onPause")
                    onBackPressedCallback.remove()
                }
            },
        )
    }

    override fun onGetTemplate(): Template {
        return GridTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setTitle(currentPoll.title)
            .setSingleList(buildItemList(currentPoll.options))
            .build()
    }

    private fun buildItemList(options: List<CarFeedbackOption>): ItemList {
        val itemListBuilder = ItemList.Builder()
        for (option in options) {
            val itemBuilder = GridItem.Builder().setTitle(option.title)
            val image = iconDownloader.getOrDownload(option.icon)
            if (image != null) {
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
        if (option.nextPoll == null) {
            val selectedItem = CarFeedbackItem(
                option.title, option.type, option.subType,
                option.searchFeedbackReason, option.favoritesFeedbackReason,
                searchOptions.geoDeeplink, searchOptions.geocodingResponse,
                searchOptions.favoriteRecords, searchOptions.searchSuggestions,
            )
            carFeedbackSender.send(selectedItem, encodedSnapshot, sourceScreenSimpleName)
            CarToast.makeText(
                carContext,
                carContext.getString(R.string.car_feedback_submit_toast_success),
                CarToast.LENGTH_LONG,
            ).show()
            onFinish()
        } else {
            currentPoll = option.nextPoll
            invalidate()
        }
    }
}
