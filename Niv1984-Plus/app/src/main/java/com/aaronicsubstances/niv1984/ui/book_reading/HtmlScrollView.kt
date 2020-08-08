package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class HtmlScrollView : ScrollView {
    var viewDataChangeListener : HtmlViewManager? = null

    constructor(context: Context):
            super(context)
    constructor(context: Context, attrs: AttributeSet):
            super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int):
            super(context, attrs, defStyleAttr)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewDataChangeListener?.onScrollViewSizeChanged(this, w, h)
    }
}