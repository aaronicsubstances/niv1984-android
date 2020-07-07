package com.aaronicsubstances.endlesspaginglib;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class EndlessListViewScrollListener<T extends EndlessListItem> extends RecyclerView.OnScrollListener {
    final EndlessListRepository<T> repo;
    final boolean considerScrollDirectionVertical;

    public EndlessListViewScrollListener(EndlessListRepository<T> repo) {
        this(repo, true);
    }

    public EndlessListViewScrollListener(EndlessListRepository<T> repo,
                                         boolean considerScrollDirectionVertical) {
        this.repo = repo;
        this.considerScrollDirectionVertical = considerScrollDirectionVertical;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx,
                           int dy) {
        super.onScrolled(recyclerView, dx, dy);
        int change = considerScrollDirectionVertical ? dy : dx;
        if (change != 0) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            int totalItemCount = layoutManager.getItemCount();
            int visibleItemCount = layoutManager.getChildCount();
            int firstVisibleItemPos = findFirstVisibleItemPosition(layoutManager);

            // this call has to return quickly since it happens on every scroll!
            repo.listScrolled(change > 0,
                    visibleItemCount, firstVisibleItemPos, totalItemCount);
        }
    }

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
