package com.mapbox.navigation.ui.maps.recenterbutton

internal object RecenterButtonProcessor {

    /**
     * The function takes [RecenterButtonAction] performs business logic
     * and returns [RecenterButtonResult]
     * @param action RecenterButtonAction user specific commands
     * @return RecenterButtonResult
     */
    fun process(action: RecenterButtonAction): RecenterButtonResult {
        return when (action) {
            is RecenterButtonAction.RecenterButtonVisibilityChanged -> {
                processRecenterButtonVisibility(action.isVisible)
            }
            is RecenterButtonAction.OnRecenterButtonClicked -> {
                RecenterButtonResult.OnRecenterButtonClicked
            }
        }
    }

    private fun processRecenterButtonVisibility(
        isVisible: Boolean
    ): RecenterButtonResult =
        if (isVisible) {
            RecenterButtonResult.RecenterButtonVisible
        } else {
            RecenterButtonResult.RecenterButtonInvisible
        }
}
