package com.aaronicsubstances.largelistpaging;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Intended as a recycler view adapter with api similar to {@link ListAdapter}, so as to
 * enable seamless switch to ListAdapter when data becomes large.
 * @param <T>
 * @param <VH>
 */
public abstract class FiniteListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private List<T> currentList = Collections.emptyList();

    /**
     * Creates a new instance with an initially empty list of items.
     * @param ignore not used by this class, but needed when class is changed to a {@link ListAdapter}.
     *               pass null.
     */
    public FiniteListAdapter(DiffUtil.ItemCallback<T> ignore) {
    }

    @Override
    public int getItemCount() {
        return currentList.size();
    }

    public List<T> getCurrentList() {
        return currentList;
    }

    protected T getItem(int position) {
        return currentList.get(position);
    }

    /**
     * Mimicks {@link ListAdapter#submitList(List)}} to reset list of items in this adapter.
     * @param updates new list of items. Can be null, in which empty list of items will be used.
     */
    public void submitList(List<T> updates) {
        this.currentList = updates;
        if (updates == null) {
            this.currentList = Collections.emptyList();
        }
        notifyDataSetChanged();
    }
}
