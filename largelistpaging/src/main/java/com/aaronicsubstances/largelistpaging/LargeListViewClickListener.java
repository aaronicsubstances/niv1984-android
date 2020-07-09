package com.aaronicsubstances.largelistpaging;

import android.view.View;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public abstract class LargeListViewClickListener<T> implements View.OnClickListener {
    protected final RecyclerView.ViewHolder viewHolder;

    public LargeListViewClickListener(RecyclerView.ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    public int getItemPosition() {
        return viewHolder.getAdapterPosition();
    }

    public T getItem(ListAdapter<T, ?> adapter) {
        int position = viewHolder.getAdapterPosition();
        return adapter.getCurrentList().get(position);
    }

    public T getItem(FiniteListAdapter<T, ?> adapter) {
        int position = viewHolder.getAdapterPosition();
        return adapter.getCurrentList().get(position);
    }

    public interface Factory<T> {
        LargeListViewClickListener<T> create(RecyclerView.ViewHolder viewHolder);
    }
}
