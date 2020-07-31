package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView

class HtmlTextView : TextView {
    var viewDataChangeListener : HtmlViewManager? = null

    constructor(context: Context):
            super(context)
    constructor(context: Context, attrs: AttributeSet):
            super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int):
            super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        viewDataChangeListener?.onTextViewDrawn(this)
    }
}