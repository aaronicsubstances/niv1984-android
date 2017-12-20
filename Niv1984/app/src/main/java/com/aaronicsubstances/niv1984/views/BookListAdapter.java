package com.aaronicsubstances.niv1984.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aaronicsubstances.niv1984.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by Aaron on 6/21/2017.
 */

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.ViewHolder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookListAdapter.class);

    private final String[] mBooks;
    // Store the context for easy access
    private final Context mContext;

    private RecyclerViewItemClickListener mItemClickListener;

    public BookListAdapter(Context context, String[] books) {
        this.mBooks = books;
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
        View subjectView = inflater.inflate(R.layout.item_book, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(subjectView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // Get the data model based on position
        final String description = mBooks[position];

        // Set item views based on your views and data model
        holder.nameTextView.setText(description);

        try {
            char ch;
            int i = 0;
            do {
                ch = description.toLowerCase().charAt(i++);
            } while (i < description.length() && (ch < 'a' || ch > 'z'));
            Field f = R.drawable.class.getField("ic_subject_" +
                ch);
            int resId = (int) f.get(null);
            holder.iconView.setImageResource(resId);
        }
        catch (Exception ex) {
            LOGGER.error("Could not find icon for {} book.", description, ex);
        }

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
        return mBooks.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final ImageView iconView;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.text);
            iconView = (ImageView) itemView.findViewById(R.id.icon);
        }
    }
}
