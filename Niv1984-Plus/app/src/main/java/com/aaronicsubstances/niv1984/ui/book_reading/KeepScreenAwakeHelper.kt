package com.aaronicsubstances.niv1984.ui.book_reading

import android.view.WindowManager
import com.aaronicsubstances.niv1984.data.SharedPrefManager

class KeepScreenAwakeHelper(private val fragment: BookLoadFragment, var keepScreenOn: Boolean) {
    private var cancelKeepScreenOnRunnable: Runnable? = null
    private var lastTouchTime = 0L // used for debouncing

    init {
        // as long as touches are occurring on root view, keep screen on
        (fragment.view as ConstraintLayoutWithTouchIntercept).touchInterceptAction = Runnable {
            rescheduleKeepScreenOn()
        }
    }

    fun onPause() {
        cancelKeepScreenOn()
    }

    fun onResume() {
        scheduleKeepScreenOn()
    }

    private fun scheduleKeepScreenOn() {
        if (!keepScreenOn) {
            return
        }
        if (cancelKeepScreenOnRunnable != null) {
            return
        }

        fragment.requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cancelKeepScreenOnRunnable = Runnable {
            fragment.requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cancelKeepScreenOnRunnable = null
        }
        fragment.requireView().apply {
            removeCallbacks(cancelKeepScreenOnRunnable)
            postDelayed(cancelKeepScreenOnRunnable, SharedPrefManager.WAKE_LOCK_PERIOD)
        }
    }

    private fun cancelKeepScreenOn() {
        cancelKeepScreenOnRunnable?.let {
            fragment.requireView().removeCallbacks(it)
            it.run()
        }
    }

    private fun rescheduleKeepScreenOn() {
        if (!keepScreenOn) {
            return
        }

        if (cancelKeepScreenOnRunnable != null) {
            // debounce to reduce frequency of rescheduling
            val currTime = android.os.SystemClock.uptimeMillis()
            if (currTime - lastTouchTime < 2000L) { // 2 secs
                return
            }
            lastTouchTime = currTime
        }
        else {
            fragment.requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cancelKeepScreenOnRunnable = Runnable {
                fragment.requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                cancelKeepScreenOnRunnable = null
            }
        }

        fragment.requireView().apply {
            removeCallbacks(cancelKeepScreenOnRunnable)
            postDelayed(cancelKeepScreenOnRunnable, SharedPrefManager.WAKE_LOCK_PERIOD)
        }
    }

}