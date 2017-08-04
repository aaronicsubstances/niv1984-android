package com.aaronicsubstances.niv1984;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Aaron on 6/21/2017.
 */

public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ViewHolder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChapterListAdapter.class);

    private final String[] mChapters;
    // Store the context for easy access
    private final Context mContext;

    private RecyclerViewItemClickListener mItemClickListener;

    public ChapterListAdapter(Context context, String[] chapters) {
        this.mChapters = chapters;
        this.mContext = context;
    }

    public RecyclerViewItemClickListener getItemClickListener() {
        return mItemClickListener;
    }

    public void setItemClickListener(RecyclerViewItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View subjectView = inflater.inflate(R.layout.item_chapter, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(subjectView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // Get the data model based on position
        final String description = mChapters[position];

        // Set item views based on your views and data model
        holder.nameTextView.setText(description);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClicked(holder.getAdapterPosition(),
                            null);
                }
            }
        });
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mChapters.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.text);
        }
    }
}
