package com.aaronicsubstances.endlesspaginglib;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class ListAdapterForPositionalDS<T extends EndlessListItem, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private final EndlessListRepositoryForPositionalDS<T> backingRepo;

    public ListAdapterForPositionalDS(EndlessListRepositoryForPositionalDS<T> backingRepo) {
        this.backingRepo = backingRepo;
    }

    @Override
    public int getItemCount() {
        return backingRepo.getTotalCount();
    }

    protected T getItem(int position) {
        return backingRepo.getItem(position);
    }

    /**
     * Mimicks {@link ListAdapter#submitList(List)}} to reset list of items in this adapter.
     */
    public void submitList() {
        notifyDataSetChanged();
    }
}
