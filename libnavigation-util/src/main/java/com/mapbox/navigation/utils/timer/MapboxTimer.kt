package com.mapbox.navigation.utils.timer

import android.os.CountDownTimer
import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Schedule a countdown until an expiry time in the future. On finish of the countdown,
 * the timer starts again and loops infinitely until [stopCountDownTimer]
 * is invoked.
 *
 * @param initialCountDown Time until the count down timer should run
 * @param countDownInterval Interval by which the timer should decrement
 * @param listener Hook to receive the events from CountDownTimer
 */
class MapboxTimer(
    private val initialCountDown: Long,
    private val countDownInterval: Long,
    private val listener: CountdownTimerListener
) {
    private val restartAfter = initialCountDown + 10
    private lateinit var timer: CountDownTimer
    private val mainControllerJobScope = ThreadController.getMainScopeAndRootJob()

    init {
        mainControllerJobScope.scope.launch {
            startCountdownTimer()
            delay(restartAfter)
        }
    }

    private fun startCountdownTimer() {
        timer = object : CountDownTimer(initialCountDown, countDownInterval) {

            override fun onTick(millisUntilFinished: Long) {
                listener.millisUntilExpiry(millisUntilFinished)
            }

            override fun onFinish() {
                listener.onTimerExpired()
            }
        }
        timer.start()
    }

    fun stopCountDownTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }
}
