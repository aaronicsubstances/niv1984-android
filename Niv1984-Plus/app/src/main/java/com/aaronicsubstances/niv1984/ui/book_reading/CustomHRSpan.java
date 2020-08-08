package com.aaronicsubstances.niv1984.ui.book_reading;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;

public class CustomHRSpan implements LineBackgroundSpan, LineHeightSpan {
    private final int color;          // Color of line
    private final float height;       // Height of HR element
    private final float line;         // Line size
    private final float marginLeft;   // Margin of line, left side
    private final float marginRight;  // Margin of line, right side

    public CustomHRSpan(int color, float height, float line) {
        this.color        = color;
        this.height       = height;
        this.line         = line;
        this.marginLeft   = 5.0f;
        this.marginRight  = 5.0f;
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom,
                               CharSequence text, int start, int end, int lnum) {
        int paintColor = p.getColor();
        float y = (float)(top+(bottom-top)/2) - line*0.5f;
        RectF r = new RectF((float)left+marginLeft, y,
                (float)(right-left)-marginRight, y+line);
        p.setColor(color);
        c.drawRect(r, p);
        p.setColor(paintColor);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        fm.descent  = (int)height / 2;
        fm.ascent   = (int)height - fm.descent;
        fm.leading  = 0;
        fm.top      = fm.ascent;
        fm.bottom   = fm.descent;
    }
}