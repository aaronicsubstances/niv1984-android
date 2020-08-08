package com.aaronicsubstances.niv1984.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeProper(lifeCycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifeCycleOwner,
        Observer<T> { it?.let { observer.onChanged(it) } })
}

fun <T> LiveData<LiveDataEvent<T>>.observeProperAsEvent(lifeCycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifeCycleOwner,
        Observer<LiveDataEvent<T>> { event -> event?.getContentIfNotHandled()?.let { observer.onChanged(it) } })
}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class LiveDataEvent<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}