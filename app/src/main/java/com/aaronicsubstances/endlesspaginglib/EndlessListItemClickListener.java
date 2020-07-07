package com.aaronicsubstances.endlesspaginglib;

import android.view.View;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public abstract class EndlessListItemClickListener<T> implements View.OnClickListener {
    protected final RecyclerView.ViewHolder viewHolder;

    public EndlessListItemClickListener(RecyclerView.ViewHolder viewHolder) {
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
        EndlessListItemClickListener<T> create(RecyclerView.ViewHolder viewHolder);
    }
}
