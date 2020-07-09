package com.aaronicsubstances.largelistpaging;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public abstract class LargeListViewScrollListener extends RecyclerView.OnScrollListener {
    private final boolean isScrollDirectionVertical;

    private int state = RecyclerView.SCROLL_STATE_IDLE;
    private int dx, dy;

    public LargeListViewScrollListener() {
        this(true);
    }

    public LargeListViewScrollListener(boolean isScrollDirectionVertical) {
        this.isScrollDirectionVertical = isScrollDirectionVertical;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        // this call has to return quickly since it happens on every scroll!
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (state != RecyclerView.SCROLL_STATE_IDLE &&
                newState == RecyclerView.SCROLL_STATE_IDLE) {
            onScrollDebounced(recyclerView, dx, dy);
        }
        this.state = newState;
    }

    private void onScrollDebounced(RecyclerView recyclerView, int dx, int dy) {
        int change = isScrollDirectionVertical ? dy : dx;
        if (change != 0) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            int totalItemCount = layoutManager.getItemCount();
            int visibleItemCount = layoutManager.getChildCount();
            int firstVisibleItemPos = findFirstVisibleItemPosition(layoutManager);

            // this call has to return quickly since it happens every time a scroll session ends.
            listScrolled(change > 0, visibleItemCount,
                    firstVisibleItemPos, totalItemCount);
        }
    }

    protected abstract void listScrolled(boolean isScrollInForwardDirection, int visibleItemCount,
                                         int firstVisibleItemPos, int totalItemCount);

    public static int findFirstVisibleItemPosition(RecyclerView.LayoutManager mLayoutManager) {
        int firstVisibleItemPosition;
        if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).findFirstVisibleItemPositions(null);
            // get maximum element within the list
            firstVisibleItemPosition = getFirstVisibleItem(firstVisibleItemPositions);
        }
        else {
            // caters for LinearLayoutManager and subclasses such as GridLayoutManager
            firstVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findFirstVisibleItemPosition();
        }
        return firstVisibleItemPosition;
    }

    public static int getFirstVisibleItem(int[] firstVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < firstVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = firstVisibleItemPositions[i];
            }
            else if (firstVisibleItemPositions[i] > maxSize) {
                maxSize = firstVisibleItemPositions[i];
            }
        }
        return maxSize;
    }
}
