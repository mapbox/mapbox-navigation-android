package com.mapbox.navigation.ui.androidauto.feedback.ui

import androidx.annotation.UiThread
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackSender
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler

/**
 * This screen allows the user to search for a destination.
 */
internal abstract class CarGridFeedbackScreen @UiThread constructor(
    mapboxCarContext: MapboxCarContext,
    private val sourceScreenSimpleName: String,
    private val carFeedbackSender: CarFeedbackSender,
    initialPoll: CarFeedbackPoll,
    private val encodedSnapshot: String?,
) : Screen(mapboxCarContext.carContext) {

    private var currentPoll = initialPoll

    private val iconDownloader = CarFeedbackIconDownloader(screen = this)

    init {
        addBackPressedHandler {
            onFinish()
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
                option.title,
                option.type,
                option.subType,
                option.searchFeedbackReason,
                option.favoritesFeedbackReason,
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
