package com.aaronicsubstances.niv1984.ui.book_reading

import android.os.SystemClock

class LoadProgressReporter(private val fragment: BookLoadFragment) {
    companion object {
        private const val REPORT_DELAY_TIME = 1000L
    }

    private var lastLoadTime = 0L

    private val reporterRunnable = Runnable {
        fragment.viewModel.notifyUserOfOngoingLoadProgress()
    }

    fun shouldReportProgress(): Boolean {
        val lastLoadResultOk = fragment.viewModel.isLastLoadResultValid()
        if (lastLoadResultOk) {
            fragment.requireView().removeCallbacks(reporterRunnable)
            lastLoadTime = 0L
            return false
        }

        // load is in progress, but we would want to wait until some 1 second later
        // to report progress and not disturb user with flickering bottom panel.

        val timeSpent = if (lastLoadTime <= 0L) {
            0
        } else {
            SystemClock.uptimeMillis() - lastLoadTime
        }
        if (timeSpent >= REPORT_DELAY_TIME) {
            return true
        }

        // time is not yet up so update time left.
        if (lastLoadTime <= 0L) {
            lastLoadTime = SystemClock.uptimeMillis()
        }
        fragment.requireView().let {
            it.removeCallbacks(reporterRunnable)
            it.postDelayed(reporterRunnable, REPORT_DELAY_TIME - timeSpent)
        }
        return false
    }
}