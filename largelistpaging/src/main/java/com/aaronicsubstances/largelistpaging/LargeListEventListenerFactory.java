package com.aaronicsubstances.largelistpaging;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public abstract class LargeListEventListenerFactory {

    public abstract <T> T create(RecyclerView.ViewHolder viewHolder, Class<T> listenerCls,
                                 Object eventContextData);

    public int getItemPosition(RecyclerView.ViewHolder viewHolder) {
        return viewHolder.getAdapterPosition();
    }

    public <T> T getItem(RecyclerView.ViewHolder viewHolder, ListAdapter<T, ?> adapter) {
        int position = viewHolder.getAdapterPosition();
        return adapter.getCurrentList().get(position);
    }

    public <T> T getItem(RecyclerView.ViewHolder viewHolder, FiniteListAdapter<T, ?> adapter) {
        int position = viewHolder.getAdapterPosition();
        return adapter.getCurrentList().get(position);
    }
}
