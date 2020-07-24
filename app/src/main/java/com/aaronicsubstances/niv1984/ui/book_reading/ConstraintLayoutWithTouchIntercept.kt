package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class ConstraintLayoutWithTouchIntercept: ConstraintLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var touchInterceptAction: Runnable? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        touchInterceptAction?.run()
        return super.onInterceptTouchEvent(ev)
    }
}