package com.aaronicsubstances.niv1984.etc;

/**
 * Created by Aaron on 24/12/2017.
 * Remove as soon as chapter spinner is removed from UI.
 */

public interface CurrentChapterChangeListener {
    void onCurrentChapterChanged(int bnum, int cnum);
    void onPageLoadCompleted();
}
